use crate::events::client_message::event_client_message;
use crate::events::command_reply::event_command_reply;
use crate::events::end_file::{event_end_file, EndFileReason};
use crate::events::hook::event_hook;
use crate::events::log_message::{event_log_message, LogLevel};
use crate::events::property::{event_property, EventProperty};
use crate::events::start_file::event_start_file;
use crate::types::errors::MpvError;
use crate::types::{Error, Node};
use libmpv2_sys::mpv_event;
use std::bstr::ByteString;

#[derive(Debug, Clone, PartialEq)]
pub enum Event {
    None,
    Shutdown,
    LogMessage {
        level: LogLevel,
        prefix: ByteString,
        message: ByteString,
    },
    GetPropertyReply {
        error: MpvError,
        user_data: u64,
        property: EventProperty,
    },
    SetPropertyReply {
        error: MpvError,
        user_data: u64,
    },
    CommandReply {
        error: MpvError,
        user_data: u64,
        reply: Node,
    },
    StartFile {
        playlist_entry_id: i64,
    },
    EndFile {
        reason: EndFileReason,
        error: MpvError,
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

impl TryFrom<&mpv_event> for Event {
    type Error = Error;

    fn try_from(raw: &mpv_event) -> crate::types::Result<Self> {
        match raw.event_id {
            libmpv2_sys::mpv_event_id_MPV_EVENT_NONE => Ok(Event::None),
            libmpv2_sys::mpv_event_id_MPV_EVENT_SHUTDOWN => Ok(Event::Shutdown),
            libmpv2_sys::mpv_event_id_MPV_EVENT_LOG_MESSAGE => {
                Ok(unsafe { event_log_message(raw) })
            }
            libmpv2_sys::mpv_event_id_MPV_EVENT_GET_PROPERTY_REPLY => Ok(Event::GetPropertyReply {
                error: MpvError::from(raw.error),
                user_data: raw.reply_userdata,
                property: unsafe { event_property(raw)? },
            }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_SET_PROPERTY_REPLY => Ok(Event::SetPropertyReply {
                error: MpvError::from(raw.error),
                user_data: raw.reply_userdata,
            }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_COMMAND_REPLY => Ok(Event::CommandReply {
                error: MpvError::from(raw.error),
                user_data: raw.reply_userdata,
                reply: unsafe { event_command_reply(raw)? },
            }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_START_FILE => Ok(unsafe { event_start_file(raw) }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_END_FILE => Ok(unsafe { event_end_file(raw) }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_FILE_LOADED => Ok(Event::FileLoaded),
            libmpv2_sys::mpv_event_id_MPV_EVENT_IDLE => Ok(Event::Idle),
            libmpv2_sys::mpv_event_id_MPV_EVENT_TICK => Ok(Event::Tick),
            libmpv2_sys::mpv_event_id_MPV_EVENT_CLIENT_MESSAGE => {
                Ok(unsafe { event_client_message(raw) })
            }
            libmpv2_sys::mpv_event_id_MPV_EVENT_VIDEO_RECONFIG => Ok(Event::VideoReconfig),
            libmpv2_sys::mpv_event_id_MPV_EVENT_AUDIO_RECONFIG => Ok(Event::AudioReconfig),
            libmpv2_sys::mpv_event_id_MPV_EVENT_SEEK => Ok(Event::Seek),
            libmpv2_sys::mpv_event_id_MPV_EVENT_PLAYBACK_RESTART => Ok(Event::PlaybackRestart),
            libmpv2_sys::mpv_event_id_MPV_EVENT_PROPERTY_CHANGE => Ok(Event::PropertyChange {
                user_data: raw.reply_userdata,
                property: unsafe { event_property(raw)? },
            }),
            libmpv2_sys::mpv_event_id_MPV_EVENT_QUEUE_OVERFLOW => Ok(Event::QueueOverflow),
            libmpv2_sys::mpv_event_id_MPV_EVENT_HOOK => Ok(unsafe { event_hook(raw) }),
            _ => panic!("Unknown event id: {}", raw.event_id),
        }
    }
}
impl TryFrom<mpv_event> for Event {
    type Error = Error;
    fn try_from(event: mpv_event) -> crate::types::Result<Self> {
        Event::try_from(&event)
    }
}
