use jni::objects::{JClass, JObject, JObjectArray};
use jni::signature::RuntimeFieldSignature;
use jni::strings::JNIString;
use jni::sys::jbyte;
use jni::{jni_sig, Env, JValue};
use std::bstr::ByteStr;

pub(super) struct JniUtils {}
impl JniUtils {
    pub(super) const NODE_NONE: &str = "dev/silenium/mpv/types/Node$None";
    pub(super) const NODE_FLAG: &str = "dev/silenium/mpv/types/Node$Flag";
    pub(super) const NODE_INT64: &str = "dev/silenium/mpv/types/Node$Int64";
    pub(super) const NODE_DOUBLE: &str = "dev/silenium/mpv/types/Node$Double";
    pub(super) const NODE_STRING: &str = "dev/silenium/mpv/types/Node$String";
    pub(super) const NODE_OSD_STRING: &str = "dev/silenium/mpv/types/Node$OsdString";
    pub(super) const NODE_BYTE_ARRAY: &str = "dev/silenium/mpv/types/Node$ByteArray";
    pub(super) const NODE_ARRAY: &str = "dev/silenium/mpv/types/Node$Array";
    pub(super) const NODE_MAP: &str = "dev/silenium/mpv/types/Node$Map";
    pub(super) const NODE_BASE: &str = "dev/silenium/mpv/types/Node";
    pub(super) const KOTLIN_PAIR: &str = "kotlin/Pair";

    pub(super) fn get_object_instance<'a>(
        env: &mut Env<'a>,
        class_name: &str,
    ) -> jni::errors::Result<JObject<'a>> {
        let class = env.find_class(JNIString::new(class_name))?;
        let signature = RuntimeFieldSignature::from_str(format!("L{};", class_name))?;
        let instance_field = env.get_static_field(
            class,
            JNIString::new("INSTANCE"),
            signature.field_signature(),
        )?;
        instance_field.into_object()
    }

    pub(super) fn none_node<'a>(env: &mut Env<'a>) -> jni::errors::Result<JObject<'a>> {
        JniUtils::get_object_instance(env, JniUtils::NODE_NONE)
    }

    pub(super) fn base_class<'a>(env: &mut Env<'a>) -> jni::errors::Result<JClass<'a>> {
        env.find_class(JNIString::new(JniUtils::NODE_BASE))
    }

    pub(super) fn new_map_node<'a>(env: &mut Env<'a>, pair_array: JObjectArray<'a>) -> jni::errors::Result<JObject<'a>> {
        env.new_object(
            JNIString::new(JniUtils::NODE_MAP),
            jni_sig!(([kotlin.Pair]) -> void),
            &[JValue::Object(&pair_array)],
        )
    }

    pub(super) fn new_array_node<'a>(env: &mut Env<'a>, array: JObjectArray<'a>) -> jni::errors::Result<JObject<'a>> {
        env.new_object(
            JNIString::new(JniUtils::NODE_ARRAY),
            jni_sig!(([dev.silenium.mpv.types.Node]) -> void),
            &[JValue::Object(&array)],
        )
    }

    pub(super) fn kotlin_pair<'a>(env: &mut Env<'a>) -> jni::errors::Result<JClass<'a>> {
        env.find_class(JNIString::new(JniUtils::KOTLIN_PAIR))
    }

    pub(super) fn new_pair<'a>(
        env: &mut Env<'a>,
        first: JObject<'a>,
        second: JObject<'a>,
    ) -> jni::errors::Result<JObject<'a>> {
        let kotlin_pair = JniUtils::kotlin_pair(env)?;
        env.new_object(
            kotlin_pair,
            jni_sig!((java.lang.Object, java.lang.Object) -> void),
            &[(&first).into(), (&second).into()],
        )
    }

    pub(super) fn new_boolean_node<'a>(
        env: &mut Env<'a>,
        value: bool,
    ) -> jni::errors::Result<JObject<'a>> {
        env.new_object(
            JNIString::new(Self::NODE_FLAG),
            jni_sig!((boolean) -> void),
            &[JValue::Bool(value)],
        )
    }

    pub(super) fn new_long_node<'a>(
        env: &mut Env<'a>,
        value: i64,
    ) -> jni::errors::Result<JObject<'a>> {
        env.new_object(
            JNIString::new(Self::NODE_INT64),
            jni_sig!((long) -> void),
            &[JValue::Long(value)],
        )
    }

    pub(super) fn new_double_node<'a>(
        env: &mut Env<'a>,
        value: f64,
    ) -> jni::errors::Result<JObject<'a>> {
        env.new_object(
            JNIString::new(Self::NODE_DOUBLE),
            jni_sig!((double) -> void),
            &[JValue::Double(value)],
        )
    }

    pub(super) fn new_bytearray_node<'a>(
        env: &mut Env<'a>,
        value: &ByteStr,
    ) -> jni::errors::Result<JObject<'a>> {
        let i8_slice: Vec<jbyte> = value.into_iter().map(|b| *b as _).collect();
        let array = env.new_byte_array(value.len())?;
        array.set_region(env, 0, i8_slice.as_slice())?;
        env.new_object(
            JNIString::new(Self::NODE_BYTE_ARRAY),
            jni_sig!(([jbyte]) -> void),
            &[JValue::Object(&array)],
        )
    }

    pub(crate) fn new_string_node<'a>(
        env: &mut Env<'a>,
        value: &str,
    ) -> jni::errors::Result<JObject<'a>> {
        let s = env.new_string(value)?;
        env.new_object(
            JNIString::new(Self::NODE_STRING),
            jni_sig!((java.lang.String) -> void),
            &[JValue::Object(&s)],
        )
    }

    pub(crate) fn new_osd_string_node<'a>(
        env: &mut Env<'a>,
        value: &str,
    ) -> jni::errors::Result<JObject<'a>> {
        let s = env.new_string(value)?;
        env.new_object(
            JNIString::new(Self::NODE_OSD_STRING),
            jni_sig!((java.lang.String) -> void),
            &[JValue::Object(&s)],
        )
    }
}
