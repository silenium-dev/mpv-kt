use crate::events::callback::EventCallback;
use crate::events::types::Event;
use crate::handle::MpvHandle;
use libc::{setlocale, LC_NUMERIC};
use libmpv2_sys::{
    mpv_event_id_MPV_EVENT_NONE, mpv_set_wakeup_callback, mpv_terminate_destroy, mpv_wait_event,
};
use std::ffi::CString;
use std::ops::Deref;
use std::os::raw::c_void;
use std::ptr::NonNull;
use std::sync::{Arc, Condvar, Mutex};
use std::thread;

#[derive(Debug, Default)]
struct EventLoopState {
    running: bool,
    wakeup: bool,
}

pub(crate) struct Core {
    handle: Arc<MpvHandle>,
    event_callback: Box<dyn EventCallback>,
    jvm: jni::JavaVM,

    event_loop_state: Mutex<EventLoopState>,
    cv: Condvar,
}

pub(crate) struct CoreHandle {
    handle: thread::JoinHandle<()>,
    core: Arc<Core>,
}

impl CoreHandle {
    pub fn terminate(self) -> thread::Result<()> {
        self.core.trigger_shutdown();
        let result = self.handle.join();
        unsafe { mpv_terminate_destroy(self.core.get_handle().deref().into()) };
        result
    }

    pub fn mpv_handle(&self) -> NonNull<libmpv2_sys::mpv_handle> {
        self.core.get_handle().deref().into()
    }
}

pub(crate) fn create(
    jvm: jni::JavaVM,
    callback: Box<dyn EventCallback>,
) -> Result<CoreHandle, String> {
    let locale = CString::new("C").unwrap();
    unsafe { setlocale(LC_NUMERIC, locale.as_ptr()) };

    let raw_handle = unsafe { libmpv2_sys::mpv_create() };
    let mpv_handle = NonNull::new(raw_handle).ok_or("Failed to create mpv handle")?;
    let core = Core::new(jvm, mpv_handle.into(), callback);
    let handle = core
        .start()
        .map_err(|e| format!("Failed to start core: {}", e))?;
    Ok(handle)
}

impl Core {
    fn new(jvm: jni::JavaVM, handle: MpvHandle, callback: Box<dyn EventCallback>) -> Arc<Self> {
        let result = Arc::new(Self {
            handle: Arc::new(handle),
            event_callback: callback,
            jvm,
            cv: Condvar::new(),
            event_loop_state: Mutex::new(EventLoopState {
                running: false,
                wakeup: false,
            }),
        });

        result
    }

    fn start(self: &Arc<Core>) -> Result<CoreHandle, &str> {
        let mut state = self
            .event_loop_state
            .lock()
            .expect("Failed to lock event loop state");
        if state.running {
            return Err("Event loop already running");
        }
        state.running = true;
        drop(state);

        let spawn_inner = self.clone();
        let event_thread = thread::spawn(move || {
            spawn_inner
                .jvm
                .attach_current_thread(|env| {
                    spawn_inner.event_loop(env);
                    Ok::<(), jni::errors::Error>(())
                })
                .expect("Failed to attach current thread");
        });
        unsafe {
            mpv_set_wakeup_callback(
                self.handle.deref().into(),
                Some(Core::wakeup_c),
                Arc::as_ptr(self).cast_mut().cast(),
            )
        };
        Ok(CoreHandle {
            handle: event_thread,
            core: self.clone(),
        })
    }

    fn get_handle(&self) -> &Arc<MpvHandle> {
        &self.handle
    }

    fn event_loop(&self, jni_env: &mut jni::Env) {
        loop {
            let state = self
                .event_loop_state
                .lock()
                .expect("Failed to lock event loop state");
            let state = self
                .cv
                .wait_while(state, |state| state.running && !state.wakeup)
                .expect("Failed to wait");
            if !state.running {
                break;
            }
            drop(state);
            loop {
                let raw_event_ptr = unsafe { mpv_wait_event(self.handle.deref().into(), 0.0) };
                if raw_event_ptr.is_null() {
                    break;
                }
                let raw_event = unsafe { raw_event_ptr.as_ref() }.expect("event is null");
                if raw_event.event_id == mpv_event_id_MPV_EVENT_NONE {
                    break;
                }

                let event = Event::from(raw_event);
                self.event_callback.on_event(jni_env, &event);
            }
        }
    }

    fn wakeup(&self) {
        let mut state = self
            .event_loop_state
            .lock()
            .expect("Failed to lock event loop state");
        state.wakeup = true;
        self.cv.notify_one();
    }

    extern "C" fn wakeup_c(ctx: *mut c_void) {
        if ctx.is_null() {
            println!("ctx is null");
            return;
        }
        unsafe {
            let inner = &*(ctx.cast::<Core>());
            inner.wakeup();
        }
    }

    fn trigger_shutdown(&self) {
        let mut state = self
            .event_loop_state
            .lock()
            .expect("Failed to lock event loop state");
        state.running = false;
        self.cv.notify_all();
    }
}
