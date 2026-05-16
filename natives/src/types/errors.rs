use std::ffi::{c_int, NulError};
use std::str::Utf8Error;

pub type Result<T> = std::result::Result<T, Error>;

pub(crate) fn error_to_result_code(value: c_int) -> Result<i32> {
    if value >= 0 {
        Ok(value)
    } else {
        Err(Error::from(value))
    }
}

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
    JniError(String),
    AlreadyRunning,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
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
    Rust(RustError),
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

impl From<c_int> for Error {
    fn from(value: c_int) -> Self {
        match value {
            value @ 0.. => Error::Success(value),
            libmpv2_sys::mpv_error_MPV_ERROR_EVENT_QUEUE_FULL => Error::EventQueueFull,
            libmpv2_sys::mpv_error_MPV_ERROR_NOMEM => Error::NoMemory,
            libmpv2_sys::mpv_error_MPV_ERROR_UNINITIALIZED => Error::Uninitialized,
            libmpv2_sys::mpv_error_MPV_ERROR_INVALID_PARAMETER => Error::InvalidParameter,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_NOT_FOUND => Error::OptionNotFound,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_FORMAT => Error::OptionFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_OPTION_ERROR => Error::OptionError,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_NOT_FOUND => Error::PropertyNotFound,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_FORMAT => Error::PropertyFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_UNAVAILABLE => Error::PropertyUnavailable,
            libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_ERROR => Error::PropertyError,
            libmpv2_sys::mpv_error_MPV_ERROR_COMMAND => Error::Command,
            libmpv2_sys::mpv_error_MPV_ERROR_LOADING_FAILED => Error::LoadingFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_AO_INIT_FAILED => Error::AoInitFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_VO_INIT_FAILED => Error::VoInitFailed,
            libmpv2_sys::mpv_error_MPV_ERROR_NOTHING_TO_PLAY => Error::NothingToPlay,
            libmpv2_sys::mpv_error_MPV_ERROR_UNKNOWN_FORMAT => Error::UnknownFormat,
            libmpv2_sys::mpv_error_MPV_ERROR_UNSUPPORTED => Error::Unsupported,
            libmpv2_sys::mpv_error_MPV_ERROR_NOT_IMPLEMENTED => Error::NotImplemented,
            libmpv2_sys::mpv_error_MPV_ERROR_GENERIC => Error::Generic,
            _ => unimplemented!(),
        }
    }
}

impl From<&Error> for c_int {
    fn from(value: &Error) -> Self {
        match value {
            Error::EventQueueFull => libmpv2_sys::mpv_error_MPV_ERROR_EVENT_QUEUE_FULL,
            Error::NoMemory => libmpv2_sys::mpv_error_MPV_ERROR_NOMEM,
            Error::Uninitialized => libmpv2_sys::mpv_error_MPV_ERROR_UNINITIALIZED,
            Error::InvalidParameter => libmpv2_sys::mpv_error_MPV_ERROR_INVALID_PARAMETER,
            Error::OptionNotFound => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_NOT_FOUND,
            Error::OptionFormat => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_FORMAT,
            Error::OptionError => libmpv2_sys::mpv_error_MPV_ERROR_OPTION_ERROR,
            Error::PropertyNotFound => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_NOT_FOUND,
            Error::PropertyFormat => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_FORMAT,
            Error::PropertyUnavailable => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_UNAVAILABLE,
            Error::PropertyError => libmpv2_sys::mpv_error_MPV_ERROR_PROPERTY_ERROR,
            Error::Command => libmpv2_sys::mpv_error_MPV_ERROR_COMMAND,
            Error::LoadingFailed => libmpv2_sys::mpv_error_MPV_ERROR_LOADING_FAILED,
            Error::AoInitFailed => libmpv2_sys::mpv_error_MPV_ERROR_AO_INIT_FAILED,
            Error::VoInitFailed => libmpv2_sys::mpv_error_MPV_ERROR_VO_INIT_FAILED,
            Error::NothingToPlay => libmpv2_sys::mpv_error_MPV_ERROR_NOTHING_TO_PLAY,
            Error::UnknownFormat => libmpv2_sys::mpv_error_MPV_ERROR_UNKNOWN_FORMAT,
            Error::Unsupported => libmpv2_sys::mpv_error_MPV_ERROR_UNSUPPORTED,
            Error::NotImplemented => libmpv2_sys::mpv_error_MPV_ERROR_NOT_IMPLEMENTED,
            Error::Generic => libmpv2_sys::mpv_error_MPV_ERROR_GENERIC,
            Error::Rust(_) => libmpv2_sys::mpv_error_MPV_ERROR_GENERIC,
            Error::Success(x) => *x as c_int,
        }
    }
}
