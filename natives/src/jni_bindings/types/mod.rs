pub(crate) mod utils;

use jni::objects::JObject;
use jni::Env;

pub(crate) enum JniNode {
    
}

pub(crate) trait JniSend {
    fn as_jni<'a>(&self, env: &mut Env<'a>) -> crate::types::Result<JObject<'a>>;
}

pub(crate) trait JniRecv: Sized {
    fn from_jni<'a>(env: &mut Env<'a>, obj: &JObject) -> crate::types::Result<Self>;
}
