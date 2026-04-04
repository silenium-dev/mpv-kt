use crate::core;
use crate::events::handler::EventCallback;
use libmpv2_sys::{mpv_error, mpv_error_MPV_ERROR_SUCCESS, mpv_initialize};

pub struct Mpv {
    core: core::CoreHandle,
}

impl Mpv {
    pub fn new(evn: &mut jni::Env, event_callback: Box<dyn EventCallback>) -> Result<Self, String> {
        let jvm = evn
            .get_java_vm()
            .map_err(|e| format!("Failed to get Java VM: {}", e))?;
        let core = core::create(jvm, event_callback)
            .map_err(|e| format!("Failed to create core: {}", e))?;

        Ok(Self { core })
    }

    pub fn initialize(&self) -> Result<(), mpv_error> {
        // TODO: introduce proper error type
        let handle = self.core.mpv_handle();
        let ret = unsafe { mpv_initialize(handle.as_ptr()) };
        if ret < mpv_error_MPV_ERROR_SUCCESS {
            Err(ret)
        } else {
            Ok(())
        }
    }

    pub fn terminate(self) {
        self.core.terminate().expect("Failed to shutdown event loop");
    }
}
