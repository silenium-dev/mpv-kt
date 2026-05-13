use crate::events::types::Event;
use libmpv2_sys::{mpv_event, mpv_event_client_message, mpv_event_log_message};
use std::bstr::{ByteStr, ByteString};
use std::ffi::CStr;

pub(super) unsafe fn event_client_message(raw: &mpv_event) -> Event {
    unsafe {
        let event_data = raw.data as *mut mpv_event_client_message;
        let count = (*event_data).num_args;
        let mut args: Vec<ByteString> = Vec::with_capacity(count as usize);
        for i in 0..count {
            let arg_ptr = *(*event_data).args.add(i as usize);
            args.push(ByteStr::new(CStr::from_ptr(arg_ptr).to_bytes()).to_owned())
        }
        Event::ClientMessage { args }
    }
}
