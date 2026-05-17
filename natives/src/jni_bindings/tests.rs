use crate::events::callback::EventCallback;
use crate::events::types::Event;
use crate::mpv::Mpv;
use crate::types::Format;
use jni::objects::{JObject, JString};
use jni::strings::JNIString;
use jni::{jni_mangle, jni_sig, jni_str, Env, EnvUnowned, JValue};
use std::thread::sleep;
use std::time::Duration;

struct TestCallback;
impl EventCallback for TestCallback {
    fn on_event(&self, _jni_env: &mut Env, event: &Event) {
        println!("{:?}", event);
    }
}

#[jni_mangle("dev.silenium.mpv.Bindings", "testN")]
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
            .find_class(jni_str!("dev/silenium/mpv/Bindings"))
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
