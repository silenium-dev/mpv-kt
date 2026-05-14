use crate::events::types::Event;
use libmpv2_sys::mpv_event;

pub(super) unsafe fn event_start_file(raw: &mpv_event) -> Event {
    unsafe {
        let raw_data = raw.data as *mut libmpv2_sys::mpv_event_start_file;
        Event::StartFile {
            playlist_entry_id: (*raw_data).playlist_entry_id,
        }
    }
}
