use std::ffi::{CStr, CString};
use std::sync::atomic::{AtomicU64, Ordering};
use libc::user;
use crate::core;
use crate::events::handler::EventCallback;
use libmpv2_sys::{mpv_error, mpv_error_MPV_ERROR_SUCCESS, mpv_format, mpv_get_property_async, mpv_initialize};

pub struct Mpv {
    core: core::CoreHandle,
    userdata_counter: AtomicU64,
}

impl Mpv {
    pub fn new(env: &mut jni::Env, event_callback: Box<dyn EventCallback>) -> Result<Self, String> {
        let jvm = env
            .get_java_vm()
            .map_err(|e| format!("Failed to get Java VM: {}", e))?;
        let core = core::create(jvm, event_callback)
            .map_err(|e| format!("Failed to create core: {}", e))?;

        Ok(Self { core, userdata_counter: AtomicU64::new(1) })
    }

    // TODO: introduce proper error type
    pub fn initialize(&self) -> Result<(), mpv_error> {
        let handle = self.core.mpv_handle();
        let ret = unsafe { mpv_initialize(handle.as_ptr()) };
        if ret < mpv_error_MPV_ERROR_SUCCESS {
            Err(ret)
        } else {
            Ok(())
        }
    }

    // TODO: introduce proper format enum type
    pub fn get_property_async(&self, name: &str, format: mpv_format) -> Result<u64, mpv_error> {
        let handle = self.core.mpv_handle();
        let userdata = self.userdata_counter.fetch_add(1, Ordering::Relaxed);
        let name = CString::new(name).expect("invalid property name");
        let ret = unsafe { mpv_get_property_async(handle.as_ptr(), userdata, name.into_raw(), format) };
        if ret < mpv_error_MPV_ERROR_SUCCESS {
            Err(ret)
        } else {
            Ok(userdata)
        }
    }

    pub fn terminate(self) {
        self.core
            .terminate()
            .expect("Failed to shutdown event loop");
    }
}

#[cfg(test)]
mod tests {
    use crate::events::handler::NoopEventCallback;
    use crate::mpv::Mpv;
    use crate::test_utils::create_jvm;

    #[test]
    fn it_works() {
        let jvm = create_jvm();
        jvm.attach_current_thread(|env| {
            let mpv = Mpv::new(env, Box::new(NoopEventCallback)).expect("Failed to create mpv instance");
            mpv.initialize().expect("Failed to initialize mpv");
            println!("mpv initialized: {:?}", mpv.core.mpv_handle());
            mpv.terminate();
            jni::errors::Result::Ok(())
        }).expect("Failed to attach thread");
        unsafe { jvm.destroy() }.expect("Failed to destroy JVM");
    }
}
