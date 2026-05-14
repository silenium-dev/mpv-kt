use libmpv2_sys::{mpv_byte_array, mpv_event_property, mpv_node, mpv_node_list};
use libmpv2_sys::{
    mpv_format_MPV_FORMAT_BYTE_ARRAY as MPV_FORMAT_BYTE_ARRAY,
    mpv_format_MPV_FORMAT_DOUBLE as MPV_FORMAT_DOUBLE,
    mpv_format_MPV_FORMAT_FLAG as MPV_FORMAT_FLAG, mpv_format_MPV_FORMAT_INT64 as MPV_FORMAT_INT64,
    mpv_format_MPV_FORMAT_NODE as MPV_FORMAT_NODE,
    mpv_format_MPV_FORMAT_NODE_ARRAY as MPV_FORMAT_NODE_ARRAY,
    mpv_format_MPV_FORMAT_NODE_MAP as MPV_FORMAT_NODE_MAP,
    mpv_format_MPV_FORMAT_NONE as MPV_FORMAT_NONE,
    mpv_format_MPV_FORMAT_STRING as MPV_FORMAT_STRING,
};
use std::bstr::{ByteStr, ByteString};
use std::collections::HashMap;
use std::ffi::CStr;
use std::os::raw::c_char;

#[derive(Debug, Clone, PartialEq)]
pub enum MpvNode {
    None,
    String(ByteString),
    Flag(bool),
    Int64(i64),
    Double(f64),
    NodeArray(Vec<MpvNode>),
    NodeMap(HashMap<ByteString, Box<MpvNode>>),
    ByteArray(ByteString),
}

impl From<&mpv_event_property> for MpvNode {
    fn from(value: &mpv_event_property) -> Self {
        unsafe {
            match value.format {
                MPV_FORMAT_STRING => {
                    let raw = *(value.data as *mut *mut c_char);
                    MpvNode::String(byte_string(raw))
                }
                MPV_FORMAT_FLAG => {
                    let raw = *(value.data as *mut u8);
                    MpvNode::Flag(raw != 0)
                }
                MPV_FORMAT_INT64 => {
                    let raw = *(value.data as *mut i64);
                    MpvNode::Int64(raw)
                }
                MPV_FORMAT_DOUBLE => {
                    let raw = *(value.data as *mut f64);
                    MpvNode::Double(raw)
                }
                MPV_FORMAT_NODE => {
                    let raw = *(value.data as *mut mpv_node);
                    MpvNode::from(&raw)
                }
                MPV_FORMAT_NODE_ARRAY => {
                    let raw = *(value.data as *mut mpv_node_list);
                    MpvNode::NodeArray(node_array(raw))
                }
                MPV_FORMAT_NODE_MAP => {
                    let raw = *(value.data as *mut mpv_node_list);
                    MpvNode::NodeMap(node_map(raw))
                }
                MPV_FORMAT_BYTE_ARRAY => {
                    let raw = *(value.data as *mut mpv_byte_array);
                    MpvNode::ByteArray(byte_array(raw))
                }
                MPV_FORMAT_NONE => MpvNode::None,
                _ => panic!("Unknown format: {}", value.format),
            }
        }
    }
}
impl From<&mpv_node> for MpvNode {
    fn from(value: &mpv_node) -> Self {
        unsafe {
            match value.format {
                MPV_FORMAT_STRING => MpvNode::String(byte_string(value.u.string)),
                MPV_FORMAT_FLAG => MpvNode::Flag(value.u.flag != 0),
                MPV_FORMAT_INT64 => MpvNode::Int64(value.u.int64),
                MPV_FORMAT_DOUBLE => MpvNode::Double(value.u.double_),
                MPV_FORMAT_NODE_ARRAY => MpvNode::NodeArray(node_array(*value.u.list)),
                MPV_FORMAT_NODE_MAP => MpvNode::NodeMap(node_map(*value.u.list)),
                MPV_FORMAT_BYTE_ARRAY => MpvNode::ByteArray(byte_array(*value.u.ba)),
                MPV_FORMAT_NONE => MpvNode::None,
                _ => panic!("Unknown format: {}", value.format),
            }
        }
    }
}

unsafe fn node_array(list: mpv_node_list) -> Vec<MpvNode> {
    unsafe {
        if list.num == 0 {
            return Vec::new();
        }
        let values = std::slice::from_raw_parts(list.values, list.num as usize);
        values.iter().map(|v| MpvNode::from(v)).collect()
    }
}

unsafe fn node_map(list: mpv_node_list) -> HashMap<ByteString, Box<MpvNode>> {
    unsafe {
        if list.num == 0 {
            return HashMap::new();
        }
        let values: Vec<_> = std::slice::from_raw_parts(list.values, list.num as usize)
            .iter()
            .map(|v| Box::from(MpvNode::from(v)))
            .collect();
        let keys: Vec<_> = std::slice::from_raw_parts(list.keys, list.num as usize)
            .iter()
            .map(|v| ByteStr::new(CStr::from_ptr(*v).to_bytes()).to_owned())
            .collect();
        std::iter::zip(keys, values).collect()
    }
}

unsafe fn byte_array(ba: mpv_byte_array) -> ByteString {
    unsafe {
        if ba.size == 0 {
            return ByteString::default();
        }
        let slice = std::slice::from_raw_parts(ba.data as *const u8, ba.size);
        ByteStr::new(slice).to_owned()
    }
}

unsafe fn byte_string(raw: *mut c_char) -> ByteString {
    unsafe {
        if raw.is_null() {
            return ByteString::default();
        }
        let bytes = CStr::from_ptr(raw).to_bytes();
        ByteStr::new(bytes).to_owned()
    }
}
