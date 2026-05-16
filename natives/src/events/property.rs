use crate::types::traits::MpvRecvInternal;
use crate::types::{ByteArray, Node, NodeArray, NodeMap, OsdString};
use libmpv2_sys::{mpv_event, mpv_event_property};
use std::bstr::{ByteStr, ByteString};
use std::ffi::CStr;

#[derive(Debug, Clone, PartialEq)]
pub struct EventProperty {
    name: ByteString,
    value: Node,
}

pub(super) unsafe fn event_property(raw: &mpv_event) -> crate::types::Result<EventProperty> {
    unsafe {
        let data = raw.data as *mut mpv_event_property;
        let name_c = CStr::from_ptr((*data).name);
        Ok(EventProperty {
            name: ByteStr::new(name_c.to_bytes()).to_owned(),
            value: decode_event_property(&*data)?,
        })
    }
}

unsafe fn decode_event_property(raw: &mpv_event_property) -> crate::types::Result<Node> {
    unsafe {
        match raw.format {
            libmpv2_sys::mpv_format_MPV_FORMAT_NONE => Ok(Node::None),
            libmpv2_sys::mpv_format_MPV_FORMAT_STRING => {
                Ok(Node::String(String::from_ptr(raw.data)?))
            }
            libmpv2_sys::mpv_format_MPV_FORMAT_OSD_STRING => {
                Ok(Node::OsdString(OsdString::from_ptr(raw.data)?.0))
            }
            libmpv2_sys::mpv_format_MPV_FORMAT_FLAG => Ok(Node::Flag(bool::from_ptr(raw.data)?)),
            libmpv2_sys::mpv_format_MPV_FORMAT_INT64 => Ok(Node::Int64(i64::from_ptr(raw.data)?)),
            libmpv2_sys::mpv_format_MPV_FORMAT_DOUBLE => Ok(Node::Double(f64::from_ptr(raw.data)?)),
            libmpv2_sys::mpv_format_MPV_FORMAT_NODE => Node::from_ptr(raw.data),
            libmpv2_sys::mpv_format_MPV_FORMAT_NODE_ARRAY => {
                Ok(Node::Array(NodeArray::from_ptr(raw.data)?))
            }
            libmpv2_sys::mpv_format_MPV_FORMAT_NODE_MAP => {
                Ok(Node::Map(NodeMap::from_ptr(raw.data)?))
            }
            libmpv2_sys::mpv_format_MPV_FORMAT_BYTE_ARRAY => {
                Ok(Node::ByteArray(ByteArray::from_ptr(raw.data)?))
            }
            _ => unimplemented!("{:?}", raw.format),
        }
    }
}
