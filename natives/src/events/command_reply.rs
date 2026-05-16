use crate::types::Node;
use libmpv2_sys::{mpv_event, mpv_event_command};

pub(super) unsafe fn event_command_reply(raw: &mpv_event) -> crate::types::Result<Node> {
    unsafe {
        let raw_data = raw.data as *mut mpv_event_command;
        Node::from_node_ptr(&(*raw_data).result)
    }
}
