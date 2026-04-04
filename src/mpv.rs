use crate::core;
use crate::events::handler::EventCallback;
use libmpv2_sys::{mpv_error, mpv_error_MPV_ERROR_SUCCESS, mpv_initialize};

pub struct Mpv {
    core: core::CoreHandle,
}

impl Mpv {
    pub fn new(env: &mut jni::Env, event_callback: Box<dyn EventCallback>) -> Result<Self, String> {
        let jvm = env
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
