use libmpv2_sys::{mpv_event, mpv_event_id};

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum EventId {
    None,
    Shutdown,
    LogMessage,
    GetPropertyReply,
    SetPropertyReply,
    CommandReply,
    StartFile,
    EndFile,
    FileLoaded,
    Idle,
    Tick,
    ClientMessage,
    VideoReconfig,
    AudioReconfig,
    Seek,
    PlaybackRestart,
    PropertyChange,
    QueueOverflow,
    Hook,
}

impl From<mpv_event_id> for EventId {
    fn from(value: mpv_event_id) -> Self {
        match value {
            libmpv2_sys::mpv_event_id_MPV_EVENT_NONE => EventId::None,
            libmpv2_sys::mpv_event_id_MPV_EVENT_SHUTDOWN => EventId::Shutdown,
            libmpv2_sys::mpv_event_id_MPV_EVENT_LOG_MESSAGE => EventId::LogMessage,
            libmpv2_sys::mpv_event_id_MPV_EVENT_GET_PROPERTY_REPLY => EventId::GetPropertyReply,
            libmpv2_sys::mpv_event_id_MPV_EVENT_SET_PROPERTY_REPLY => EventId::SetPropertyReply,
            libmpv2_sys::mpv_event_id_MPV_EVENT_COMMAND_REPLY => EventId::CommandReply,
            libmpv2_sys::mpv_event_id_MPV_EVENT_START_FILE => EventId::StartFile,
            libmpv2_sys::mpv_event_id_MPV_EVENT_END_FILE => EventId::EndFile,
            libmpv2_sys::mpv_event_id_MPV_EVENT_FILE_LOADED => EventId::FileLoaded,
            libmpv2_sys::mpv_event_id_MPV_EVENT_IDLE => EventId::Idle,
            libmpv2_sys::mpv_event_id_MPV_EVENT_TICK => EventId::Tick,
            libmpv2_sys::mpv_event_id_MPV_EVENT_CLIENT_MESSAGE => EventId::ClientMessage,
            libmpv2_sys::mpv_event_id_MPV_EVENT_VIDEO_RECONFIG => EventId::VideoReconfig,
            libmpv2_sys::mpv_event_id_MPV_EVENT_AUDIO_RECONFIG => EventId::AudioReconfig,
            libmpv2_sys::mpv_event_id_MPV_EVENT_SEEK => EventId::Seek,
            libmpv2_sys::mpv_event_id_MPV_EVENT_PLAYBACK_RESTART => EventId::PlaybackRestart,
            libmpv2_sys::mpv_event_id_MPV_EVENT_PROPERTY_CHANGE => EventId::PropertyChange,
            libmpv2_sys::mpv_event_id_MPV_EVENT_QUEUE_OVERFLOW => EventId::QueueOverflow,
            libmpv2_sys::mpv_event_id_MPV_EVENT_HOOK => EventId::Hook,
            _ => panic!("Unknown event id: {}", value),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Event {
    pub event_id: EventId,
    // TODO: add other event fields (wip)
}

impl From<&mpv_event> for Event {
    fn from(_event: &mpv_event) -> Self {
        Self {
            event_id: _event.event_id.into(),
        }
    }
}
impl From<mpv_event> for Event {
    fn from(event: mpv_event) -> Self {
        Event::from(&event)
    }
}
