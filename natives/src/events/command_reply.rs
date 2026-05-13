use crate::nodes::node::MpvNode;
use libmpv2_sys::{mpv_event, mpv_event_command};

pub(super) unsafe fn event_command_reply(raw: &mpv_event) -> MpvNode {
    unsafe {
        let raw_data = raw.data as *mut mpv_event_command;
        MpvNode::from(&(*raw_data).result)
    }
}
