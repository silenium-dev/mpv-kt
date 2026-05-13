use std::bstr::ByteStr;
use std::ffi::CStr;
use crate::events::types::Event;
use libmpv2_sys::{mpv_event, mpv_event_log_message, mpv_log_level};

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum LogLevel {
    None,
    Fatal,
    Error,
    Warning,
    Info,
    Verbose,
    Debug,
    Trace,
    Other(u32),
}

impl From<mpv_log_level> for LogLevel {
    fn from(raw: mpv_log_level) -> Self {
        match raw {
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_NONE => LogLevel::None,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_FATAL => LogLevel::Fatal,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_ERROR => LogLevel::Error,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_WARN => LogLevel::Warning,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_INFO => LogLevel::Info,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_V => LogLevel::Verbose,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_DEBUG => LogLevel::Debug,
            libmpv2_sys::mpv_log_level_MPV_LOG_LEVEL_TRACE => LogLevel::Trace,
            _ => LogLevel::Other(raw),
        }
    }
}

pub(super) unsafe fn event_log_message(raw: &mpv_event) -> Event {
    unsafe {
        let event_data = raw.data as *mut mpv_event_log_message;
        let prefix = ByteStr::new(CStr::from_ptr((*event_data).prefix).to_bytes());
        let message = ByteStr::new(CStr::from_ptr((*event_data).text).to_bytes());
        Event::LogMessage {
            level: LogLevel::from((*event_data).log_level),
            prefix: prefix.into(),
            message: message.into(),
        }
    }
}
