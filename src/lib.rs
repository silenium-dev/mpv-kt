use std::thread::sleep;
use std::time::Duration;
use crate::events::handler::EventCallback;
use crate::events::types::Event;
use crate::mpv::Mpv;
use jni::objects::{JObject, JString};
use jni::strings::JNIString;
use jni::{Env, EnvUnowned, JValue, jni_mangle, jni_sig};

mod core;
pub mod events;
mod handle;
pub mod mpv;
mod test_utils;

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
