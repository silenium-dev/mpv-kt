use crate::core;
use crate::events::callback::EventCallback;
use crate::types::traits::MpvSendInternal;
use crate::types::Error::Rust;
use crate::types::RustError::Generic;
use crate::types::{error_to_result, Node};
use crate::types::{Format, Result};
use libmpv2_sys::{
    mpv_format_MPV_FORMAT_NODE, mpv_get_property_async, mpv_initialize,
    mpv_set_property_async,
};
use std::ffi::CString;
use std::sync::atomic::{AtomicU64, Ordering};

pub struct Mpv {
    core: core::CoreHandle,
    userdata_counter: AtomicU64,
}

impl Mpv {
    pub fn new(env: &mut jni::Env, event_callback: Box<dyn EventCallback>) -> Result<Self> {
        let jvm = env
            .get_java_vm()
            .map_err(|e| Rust(Generic(format!("Failed to get Java VM: {}", e))))?;
        let core = core::create(jvm, event_callback)?;

        Ok(Self {
            core,
            userdata_counter: AtomicU64::new(1),
        })
    }

    pub fn initialize(&self) -> Result<()> {
        let handle = self.core.mpv_handle();
        let ret = unsafe { mpv_initialize(handle.as_ptr()) };
        error_to_result(ret)
    }

    pub fn get_property_async(&self, name: &str, format: Format) -> Result<u64> {
        let handle = self.core.mpv_handle();
        let userdata = self.userdata_counter.fetch_add(1, Ordering::Relaxed);
        let name = CString::new(name)?;
        let ret = unsafe {
            mpv_get_property_async(handle.as_ptr(), userdata, name.as_ptr(), format.mpv_format)
        };
        error_to_result(ret).map(|_| userdata)
    }

    pub fn set_property_async(&self, name: &str, value: Node) -> Result<u64> {
        let handle = self.core.mpv_handle();
        let userdata = self.userdata_counter.fetch_add(1, Ordering::Relaxed);
        let name = CString::new(name)?;
        unsafe {
            value.to_mpv(|raw_value| {
                let ret = mpv_set_property_async(
                    handle.as_ptr(),
                    userdata,
                    name.as_ptr(),
                    mpv_format_MPV_FORMAT_NODE,
                    raw_value,
                );
                error_to_result(ret)
            })?
        };
        Ok(userdata)
    }

    pub fn terminate(self) {
        self.core
            .terminate()
            .expect("Failed to shutdown event loop");
    }
}

#[cfg(test)]
mod tests {
    use crate::events::callback::NoopEventCallback;
    use crate::mpv::Mpv;
    use crate::test_utils::create_jvm;

    #[test]
    fn it_works() {
        let jvm = create_jvm();
        jvm.attach_current_thread(|env| {
            let mpv =
                Mpv::new(env, Box::new(NoopEventCallback)).expect("Failed to create mpv instance");
            mpv.initialize().expect("Failed to initialize mpv");
            println!("mpv initialized: {:?}", mpv.core.mpv_handle());
            mpv.terminate();
            jni::errors::Result::Ok(())
        })
        .expect("Failed to attach thread");
        unsafe { jvm.destroy() }.expect("Failed to destroy JVM");
    }
}
