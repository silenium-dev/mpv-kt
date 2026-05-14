use crate::nodes::node::MpvNode;
use libmpv2_sys::{mpv_event, mpv_event_property};
use std::bstr::{ByteStr, ByteString};
use std::ffi::CStr;

#[derive(Debug, Clone, PartialEq)]
pub struct EventProperty {
    name: ByteString,
    value: MpvNode,
}

pub(super) unsafe fn event_property(raw: &mpv_event) -> EventProperty {
    unsafe {
        let data = raw.data as *mut mpv_event_property;
        let name_c = CStr::from_ptr((*data).name);
        EventProperty {
            name: ByteStr::new(name_c.to_bytes()).to_owned(),
            value: MpvNode::from(&*data),
        }
    }
}
