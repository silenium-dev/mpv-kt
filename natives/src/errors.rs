#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum MpvError {
    Success,
    EventQueueFull,
    Nomem,
    Uninitialized,
    InvalidParameter,
    OptionNotFound,
    OptionFormat,
    OptionError,
    PropertyNotFound,
    PropertyFormat,
    PropertyUnavailable,
    PropertyError,
    Command,
    LoadingFailed,
    AoInitFailed,
    VoInitFailed,
    NothingToPlay,
    UnknownFormat,
    Unsupported,
    NotImplemented,
    Generic,
}

impl From<libmpv2_sys::mpv_error> for MpvError {
    fn from(value: libmpv2_sys::mpv_error) -> Self {
        if value >= 0 {
            return MpvError::Success;
        }
        match value {
            libmpv2_sys::mpv_error_MPV_ERROR_EVENT_QUEUE_FULL => MpvError::EventQueueFull,
            libmpv2_sys::mpv_error_MPV_ERROR_NOMEM => MpvError::Nomem,
            libmpv2_sys::mpv_error_MPV_ERROR_UNINITIALIZED => MpvError::Uninitialized,
            libmpv2_sys::mpv_error_MPV_ERROR_INVALID_PARAMETER => MpvError::InvalidParameter,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_NOT_FOUND => MpvError::OptionNotFound,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_FORMAT => MpvError::OptionFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_ERROR => MpvError::OptionError,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_NOT_FOUND => MpvError::PropertyNotFound,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_FORMAT => MpvError::PropertyFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_UNAVAILABLE => MpvError::PropertyUnavailable,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_ERROR => MpvError::PropertyError,
            libmpv2_sys::mpv_error_MPV_ERROR_COMMAND => MpvError::Command,
            libmpv2_sys::mpv_error_MPV_ERROR_LOADING_FAILED => MpvError::LoadingFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_AO_INIT_FAILED => MpvError::AoInitFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_VO_INIT_FAILED => MpvError::VoInitFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_NOTHING_TO_PLAY => MpvError::NothingToPlay,
            libmpv2_sys::mpv_error_MPV_ERROR_UNKNOWN_FORMAT => MpvError::UnknownFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_UNSUPPORTED => MpvError::Unsupported,
            libmpv2_sys::mpv_error_MPV_ERROR_NOT_IMPLEMENTED => MpvError::NotImplemented,
            libmpv2_sys::mpv_error_MPV_ERROR_GENERIC => MpvError::Generic,
            _ => panic!("Unknown error code: {}", value),
        }
    }
}
