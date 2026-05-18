pub(crate) mod utils;
mod node;

use jni::objects::JObject;
use jni::Env;

pub(crate) trait JniSend {
    fn as_jni<'a>(&self, env: &'a mut Env<'a>) -> crate::types::Result<JObject<'a>>;
}

pub(crate) trait JniRecv: Sized {
    fn from_jni<'a>(env: &'a mut Env<'a>, obj: &JObject) -> crate::types::Result<Self>;
}
