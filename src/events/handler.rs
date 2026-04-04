use crate::events::types::Event;

pub trait EventCallback: Send + Sync {
    fn on_event(&self, jni_env: &mut jni::Env, event: &Event);
}

pub struct NoopEventCallback;
impl EventCallback for NoopEventCallback {
    fn on_event(&self, _jni_env: &mut jni::Env, _event: &Event) {}
}
