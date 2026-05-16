#![feature(bstr)]

use crate::events::callback::EventCallback;
use crate::events::types::Event;
use crate::mpv::Mpv;
use crate::types::Format;
use jni::objects::{JObject, JString};
use jni::strings::JNIString;
use jni::{jni_mangle, jni_sig, Env, EnvUnowned, JValue};
use libmpv2_sys::mpv_node;
use std::ffi::c_void;
use std::thread::sleep;
use std::time::Duration;

#[macro_use]
mod macros;
mod core;
pub mod events;
mod handle;
pub mod mpv;
mod test_utils;
pub mod types;

pub(crate) unsafe fn mpv_free(data: *mut c_void) {
    #[cfg(not(test))]
    unsafe {
        libmpv2_sys::mpv_free(data);
    }
    #[cfg(test)]
    test_utils::mpv_free_stub(data);
}

pub(crate) unsafe fn mpv_free_node_contents(node: *mut mpv_node) {
    #[cfg(not(test))]
    unsafe {
        libmpv2_sys::mpv_free_node_contents(node)
    }
    #[cfg(test)]
    test_utils::mpv_free_node_contents_stub(node);
}

struct TestCallback;
impl EventCallback for TestCallback {
    fn on_event(&self, _jni_env: &mut Env, event: &Event) {
        println!("{:?}", event);
    }
}

#[jni_mangle("Test_main$TestNatives", "testN")]
pub fn testN<'local>(mut env: EnvUnowned<'local>, _this: JObject<'local>) {
    env.with_env(|env| {
        println!("testN");
        let mpv = Mpv::new(env, Box::new(TestCallback)).expect("Failed to create mpv");
        mpv.initialize().expect("Failed to initialize mpv");
        let request_id = mpv
            .get_property_async("track-list", Format::NODE)
            .expect("Failed to get property");
        println!("request_id: {}", request_id);
        let request_id = mpv
            .get_property_async("pid", Format::NODE)
            .expect("Failed to get property");
        println!("request_id: {}", request_id);
        let request_id = mpv
            .get_property_async("mpv-version", Format::OSD_STRING)
            .expect("Failed to get property");
        println!("request_id: {}", request_id);
        let request_id = mpv
            .set_property_async("hwdec", "vaapi".into())
            .expect("Failed to set property");
        println!("request_id: {}", request_id);
        sleep(Duration::from_secs(1));
        let request_id = mpv
            .get_property_async("hwdec", Format::OSD_STRING)
            .expect("Failed to get property");
        println!("request_id: {}", request_id);
        sleep(Duration::from_secs(1));
        mpv.terminate();

        let class = env
            .find_class(JNIString::new("Test_main$TestNatives"))
            .expect("Failed to find class");
        let str = JString::from_str(env, "hello from rust").expect("Failed to create string");
        env.call_static_method(
            class,
            JNIString::new("test"),
            jni_sig!("(Ljava/lang/String;)V"),
            &[JValue::Object(&str)],
        )
        .expect("Failed to call test");

        Ok::<(), jni::errors::Error>(())
    })
    .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
