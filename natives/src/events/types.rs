use crate::events::client_message::event_client_message;
use crate::events::command_reply::event_command_reply;
use crate::events::end_file::{EndFileReason, event_end_file};
use crate::events::hook::event_hook;
use crate::events::log_message::{LogLevel, event_log_message};
use crate::events::property::{EventProperty, event_property};
use crate::events::start_file::event_start_file;
use crate::nodes::node::MpvNode;
use libmpv2_sys::{mpv_error, mpv_event};
use std::bstr::ByteString;

#[derive(Debug, Clone)]
pub enum Event {
    None,
    Shutdown,
    LogMessage {
        level: LogLevel,
        prefix: ByteString,
        message: ByteString,
    },
    GetPropertyReply {
        error: EventError,
        user_data: u64,
        property: EventProperty,
    },
    SetPropertyReply {
        error: EventError,
        user_data: u64,
    },
    CommandReply {
        error: EventError,
        user_data: u64,
        reply: MpvNode,
    },
    StartFile {
        playlist_entry_id: i64,
    },
    EndFile {
        reason: EndFileReason,
        error: EventError,
        playlist_entry_id: i64,
        playlist_insert_id: i64,
        playlist_insert_num_entries: i32,
    },
    FileLoaded,
    Idle,
    Tick,
    ClientMessage {
        args: Vec<ByteString>,
    },
    VideoReconfig,
    AudioReconfig,
    Seek,
    PlaybackRestart,
    PropertyChange {
        user_data: u64,
        property: EventProperty,
    },
    QueueOverflow,
    Hook {
        user_data: u64,
        name: ByteString,
        id: u64,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum EventError {
    Ok,
    Err(mpv_error),
}

impl From<mpv_error> for EventError {
    fn from(value: mpv_error) -> Self {
        if value >= 0 {
            EventError::Ok
        } else {
            EventError::Err(value)
        }
    }
}

impl From<&mpv_event> for Event {
    fn from(raw: &mpv_event) -> Self {
        match raw.event_id {
            libmpv2_sys::mpv_event_id_MPV_EVENT_NONE => Event::None,
            libmpv2_sys::mpv_event_id_MPV_EVENT_SHUTDOWN => Event::Shutdown,
            libmpv2_sys::mpv_event_id_MPV_EVENT_LOG_MESSAGE => unsafe { event_log_message(raw) },
            libmpv2_sys::mpv_event_id_MPV_EVENT_GET_PROPERTY_REPLY => Event::GetPropertyReply {
                error: EventError::from(raw.error),
                user_data: raw.reply_userdata,
                property: unsafe { event_property(raw) },
            },
            libmpv2_sys::mpv_event_id_MPV_EVENT_SET_PROPERTY_REPLY => Event::SetPropertyReply {
                error: EventError::from(raw.error),
                user_data: raw.reply_userdata,
            },
            libmpv2_sys::mpv_event_id_MPV_EVENT_COMMAND_REPLY => Event::CommandReply {
                error: EventError::from(raw.error),
                user_data: raw.reply_userdata,
                reply: unsafe { event_command_reply(raw) },
            },
            libmpv2_sys::mpv_event_id_MPV_EVENT_START_FILE => unsafe { event_start_file(raw) },
            libmpv2_sys::mpv_event_id_MPV_EVENT_END_FILE => unsafe { event_end_file(raw) },
            libmpv2_sys::mpv_event_id_MPV_EVENT_FILE_LOADED => Event::FileLoaded,
            libmpv2_sys::mpv_event_id_MPV_EVENT_IDLE => Event::Idle,
            libmpv2_sys::mpv_event_id_MPV_EVENT_TICK => Event::Tick,
            libmpv2_sys::mpv_event_id_MPV_EVENT_CLIENT_MESSAGE => unsafe {
                event_client_message(raw)
            },
            libmpv2_sys::mpv_event_id_MPV_EVENT_VIDEO_RECONFIG => Event::VideoReconfig,
            libmpv2_sys::mpv_event_id_MPV_EVENT_AUDIO_RECONFIG => Event::AudioReconfig,
            libmpv2_sys::mpv_event_id_MPV_EVENT_SEEK => Event::Seek,
            libmpv2_sys::mpv_event_id_MPV_EVENT_PLAYBACK_RESTART => Event::PlaybackRestart,
            libmpv2_sys::mpv_event_id_MPV_EVENT_PROPERTY_CHANGE => Event::PropertyChange {
                user_data: raw.reply_userdata,
                property: unsafe { event_property(raw) },
            },
            libmpv2_sys::mpv_event_id_MPV_EVENT_QUEUE_OVERFLOW => Event::QueueOverflow,
            libmpv2_sys::mpv_event_id_MPV_EVENT_HOOK => unsafe { event_hook(raw) },
            _ => panic!("Unknown event id: {}", raw.event_id),
        }
    }
}
impl From<mpv_event> for Event {
    fn from(event: mpv_event) -> Self {
        Event::from(&event)
    }
}
