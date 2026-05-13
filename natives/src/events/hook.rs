use crate::events::types::Event;
use libmpv2_sys::{mpv_event, mpv_event_hook};
use std::bstr::ByteStr;
use std::ffi::CStr;

pub(super) unsafe fn event_hook(raw: &mpv_event) -> Event {
    unsafe {
        let raw_data = raw.data as *mut mpv_event_hook;
        let name = ByteStr::new(CStr::from_ptr((*raw_data).name).to_bytes());
        Event::Hook {
            user_data: raw.reply_userdata,
            name: name.into(),
            id: (*raw_data).id,
        }
    }
}
