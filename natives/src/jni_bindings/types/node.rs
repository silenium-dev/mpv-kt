use crate::jni_bindings::types::JniSend;
use crate::types::Node;
use const_format::formatcp;
use jni::objects::{JObject, JObjectArray};
use jni::signature::{RuntimeFieldSignature, RuntimeMethodSignature};
use jni::strings::JNIString;
use jni::sys::{jbyte, jsize};
use jni::{Env, JValue, jni_sig, jni_str};
use std::fmt::format;

const NODE_CLASS: &str = "dev/silenium/libs/jni/Node";
const NONE_CLASS: &str = formatcp!("{}$None", NODE_CLASS);
const STRING_CLASS: &str = formatcp!("{}$String", NODE_CLASS);
const OSD_STRING_CLASS: &str = formatcp!("{}$OsdString", NODE_CLASS);
const FLAG_CLASS: &str = formatcp!("{}$Flag", NODE_CLASS);
const INT64_CLASS: &str = formatcp!("{}$Int64", NODE_CLASS);
const DOUBLE_CLASS: &str = formatcp!("{}$Double", NODE_CLASS);
const NODE_ARRAY_CLASS: &str = formatcp!("{}$NodeArray", NODE_CLASS);
const NODE_MAP_CLASS: &str = formatcp!("{}$NodeMap", NODE_CLASS);
const BYTE_ARRAY_CLASS: &str = formatcp!("{}$ByteArray", NODE_CLASS);

impl JniSend for Node {
    fn as_jni<'a>(&self, env: &'a mut Env<'a>) -> crate::types::Result<JObject<'a>> {
        let mut utils = JniUtils::from(env);
        match self {
            Node::None => utils.get_object_instance(NONE_CLASS),
            Node::String(s) => utils.new_string_node(STRING_CLASS, s),
            Node::OsdString(s) => utils.new_string_node(OSD_STRING_CLASS, s),
            Node::Flag(f) => Ok(utils.env.new_object(
                JNIString::new(FLAG_CLASS),
                jni_sig!((jboolean) -> void),
                &[JValue::Bool(*f)],
            )?),
            Node::Int64(i) => Ok(utils.env.new_object(
                JNIString::new(INT64_CLASS),
                jni_sig!((jlong) -> void),
                &[JValue::Long(*i)],
            )?),
            Node::Double(d) => Ok(utils.env.new_object(
                JNIString::new(DOUBLE_CLASS),
                jni_sig!((jdouble) -> void),
                &[JValue::Double(*d)],
            )?),
            Node::Array(a) => {
                let array = utils.new_node_array_from_owned(a.as_slice())?;
                let sig = RuntimeMethodSignature::from_str(format!("([L{};])->void", NODE_CLASS))?;
                let obj = utils.env.new_object(
                    JNIString::new(NODE_CLASS),
                    sig.method_signature(),
                    &[JValue::Object(&array)],
                )?;
                Ok(obj)
            }
            Node::Map(m) => {
                let keys: Vec<_> = m.keys().map(|k| k.as_str()).collect();
                let values: Vec<_> = keys.iter().map(|k| m.get(*k).unwrap()).collect(); // Can unwrap, keys are always valid
                utils.new_node_map(keys.as_slice(), values.as_slice())
            }
            Node::ByteArray(ba) => utils.new_bytearray_node(BYTE_ARRAY_CLASS, ba),
        }
    }
}

struct JniUtils<'a> {
    env: &'a mut Env<'a>,
}
impl<'a> JniUtils<'a> {
    fn from(env: &'a mut Env<'a>) -> Self {
        Self { env }
    }

    fn get_object_instance(&mut self, class: &str) -> crate::types::Result<JObject<'a>> {
        let cls = self.env.find_class(JNIString::new(class))?;
        let sig = RuntimeFieldSignature::from_str(format!("L{};", class))?;
        let instance =
            self.env
                .get_static_field(cls, jni_str!("INSTANCE"), sig.field_signature())?;
        Ok(instance.into_object()?)
    }

    fn new_string_node(&mut self, class: &str, value: &str) -> crate::types::Result<JObject<'a>> {
        let s = self.env.new_string(value)?;
        Ok(self.env.new_object(
            JNIString::new(class),
            jni_sig!((java.lang.String) -> void),
            &[JValue::Object(&s)],
        )?)
    }

    fn new_bytearray_node(
        &mut self,
        class: &str,
        value: &[u8],
    ) -> crate::types::Result<JObject<'a>> {
        let ba = self.env.new_byte_array(value.len())?;
        let i8_vec: Vec<_> = value.iter().map(|&b| b as _).collect();
        ba.set_region(self.env, 0, i8_vec.as_slice())?;
        Ok(self.env.new_object(
            JNIString::new(class),
            jni_sig!(([jbyte]) -> void),
            &[JValue::Object(&ba)],
        )?)
    }

    fn new_node_array_from_owned(
        &mut self,
        value: &[Node],
    ) -> crate::types::Result<JObjectArray<'a>> {
        let elements: Vec<&Node> = value.iter().collect();
        self.new_node_array(&elements)
    }

    fn new_node_array(&mut self, value: &[&Node]) -> crate::types::Result<JObjectArray<'a>> {
        let node_class = self.env.find_class(JNIString::new(NODE_CLASS))?;
        let array = self
            .env
            .new_object_array(value.len() as jsize, node_class, JObject::null())?;
        // for (i, node) in value.iter().enumerate() {
            // let jni_node: JObject<'a> = node.as_jni(self.env)?;
            // array.set_element(self.env, i, jni_node)?;
        // }
        Ok(array)
    }

    fn new_string_array(&mut self, value: &[&str]) -> crate::types::Result<JObjectArray<'a>> {
        let string_class = self.env.find_class(JNIString::new(STRING_CLASS))?;
        let array =
            self.env
                .new_object_array(value.len() as jsize, string_class, JObject::null())?;
        for (i, s) in value.iter().enumerate() {
            let jni_s: JObject<'a> = self.env.new_string(s)?.into();
            array.set_element(self.env, i, jni_s)?;
        }
        Ok(array)
    }

    fn new_node_map(
        &mut self,
        keys: &[&str],
        values: &[&Node],
    ) -> crate::types::Result<JObject<'a>> {
        let class = self.env.find_class(JNIString::new(NODE_MAP_CLASS))?;
        let sig = RuntimeMethodSignature::from_str(format!(
            "([Ljava/lang/String;[L{};)->void",
            NODE_CLASS
        ))?;
        let keys_array = self.new_string_array(keys)?.into();
        let values_array = self.new_node_array(values)?.into();
        let obj = self.env.new_object(
            class,
            sig.method_signature(),
            &[JValue::Object(&keys_array), JValue::Object(&values_array)],
        )?;
        Ok(obj)
    }
}
