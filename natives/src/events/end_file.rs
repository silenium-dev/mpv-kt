use crate::events::types::Event;
use crate::types::errors::Error;
use libmpv2_sys::{mpv_end_file_reason, mpv_event};

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum EndFileReason {
    EOF,
    Stop,
    Quit,
    Error,
    Redirect,
}

impl From<mpv_end_file_reason> for EndFileReason {
    fn from(value: mpv_end_file_reason) -> Self {
        match value {
            libmpv2_sys::mpv_end_file_reason_MPV_END_FILE_REASON_EOF => Self::EOF,
            libmpv2_sys::mpv_end_file_reason_MPV_END_FILE_REASON_STOP => Self::Stop,
            libmpv2_sys::mpv_end_file_reason_MPV_END_FILE_REASON_QUIT => Self::Quit,
            libmpv2_sys::mpv_end_file_reason_MPV_END_FILE_REASON_ERROR => Self::Error,
            libmpv2_sys::mpv_end_file_reason_MPV_END_FILE_REASON_REDIRECT => Self::Redirect,
            _ => panic!("Unknown end file reason: {}", value),
        }
    }
}

pub(super) unsafe fn event_end_file(raw: &mpv_event) -> Event {
    unsafe {
        let raw_data = raw.data as *mut libmpv2_sys::mpv_event_end_file;
        Event::EndFile {
            reason: EndFileReason::from((*raw_data).reason),
            error: Error::from(raw.error),
            playlist_entry_id: (*raw_data).playlist_entry_id,
            playlist_insert_id: (*raw_data).playlist_insert_id,
            playlist_insert_num_entries: (*raw_data).playlist_insert_num_entries,
        }
    }
}
