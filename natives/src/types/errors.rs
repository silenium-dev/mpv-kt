use std::ffi::{c_int, NulError};
use std::str::Utf8Error;

pub type Result<T> = std::result::Result<T, Error>;

pub(crate) fn error_to_result(value: c_int) -> Result<()> {
    if value >= 0 {
        Ok(())
    } else {
        Err(Error::from(value))
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VersionError {
    pub expected: u64,
    pub found: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DebugLoc {
    pub file: &'static str,
    pub line: u32,
    pub function: &'static str,
    pub variable: Option<&'static str>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RustError {
    InvalidUtf8(Utf8Error),
    InteriorNull(NulError),
    VersionMismatch(VersionError),
    Pointer(Option<DebugLoc>),
    Generic(String),
    AlreadyRunning,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum MpvError {
    Success(i32),
    EventQueueFull,
    NoMemory,
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

#[derive(Debug)]
pub enum Error {
    Mpv(MpvError),
    Rust(RustError),
    Jni(jni::errors::Error),
}

impl From<Utf8Error> for Error {
    fn from(value: Utf8Error) -> Self {
        Self::Rust(RustError::InvalidUtf8(value))
    }
}

impl From<NulError> for Error {
    fn from(value: NulError) -> Self {
        Self::Rust(RustError::InteriorNull(value))
    }
}

impl From<jni::errors::Error> for Error {
    fn from(value: jni::errors::Error) -> Self {
        Self::Jni(value)
    }
}

impl From<MpvError> for Error {
    fn from(value: MpvError) -> Self {
        Self::Mpv(value)
    }
}

impl From<RustError> for Error {
    fn from(value: RustError) -> Self {
        Self::Rust(value)
    }
}

impl From<c_int> for Error {
    fn from(value: c_int) -> Self {
        Self::Mpv(value.into())
    }
}

impl From<c_int> for MpvError {
    fn from(value: c_int) -> Self {
        match value {
            value @ 0.. => MpvError::Success(value),
            libmpv2_sys::mpv_error_MPV_ERROR_EVENT_QUEUE_FULL => MpvError::EventQueueFull,
            libmpv2_sys::mpv_error_MPV_ERROR_NOMEM => MpvError::NoMemory,
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
            _ => unimplemented!(),
        }
    }
}

impl From<&MpvError> for c_int {
    fn from(value: &MpvError) -> Self {
        match value {
            MpvError::EventQueueFull => libmpv2_sys::mpv_error_MPV_ERROR_EVENT_QUEUE_FULL,
            MpvError::NoMemory => libmpv2_sys::mpv_error_MPV_ERROR_NOMEM,
            MpvError::Uninitialized => libmpv2_sys::mpv_error_MPV_ERROR_UNINITIALIZED,
            MpvError::InvalidParameter => libmpv2_sys::mpv_error_MPV_ERROR_INVALID_PARAMETER,
            MpvError::OptionNotFound => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_NOT_FOUND,
            MpvError::OptionFormat => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_FORMAT,
            MpvError::OptionError => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_ERROR,
            MpvError::PropertyNotFound => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_NOT_FOUND,
            MpvError::PropertyFormat => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_FORMAT,
            MpvError::PropertyUnavailable => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_UNAVAILABLE,
            MpvError::PropertyError => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_ERROR,
            MpvError::Command => libmpv2_sys::mpv_error_MPV_ERROR_COMMAND,
            MpvError::LoadingFailed => libmpv2_sys::mpv_error_MPV_ERROR_LOADING_FAILED,
            MpvError::AoInitFailed => libmpv2_sys::mpv_error_MPV_ERROR_AO_INIT_FAILED,
            MpvError::VoInitFailed => libmpv2_sys::mpv_error_MPV_ERROR_VO_INIT_FAILED,
            MpvError::NothingToPlay => libmpv2_sys::mpv_error_MPV_ERROR_NOTHING_TO_PLAY,
            MpvError::UnknownFormat => libmpv2_sys::mpv_error_MPV_ERROR_UNKNOWN_FORMAT,
            MpvError::Unsupported => libmpv2_sys::mpv_error_MPV_ERROR_UNSUPPORTED,
            MpvError::NotImplemented => libmpv2_sys::mpv_error_MPV_ERROR_NOT_IMPLEMENTED,
            MpvError::Generic => libmpv2_sys::mpv_error_MPV_ERROR_GENERIC,
            MpvError::Success(x) => *x as c_int,
        }
    }
}
