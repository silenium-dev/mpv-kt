use crate::mpv_free;
use crate::types::traits::{MpvRecvInternal, MpvSendInternal};
use crate::types::{Format, MpvFormat, MpvRecv, MpvSend, Node};
use std::ffi::{c_char, c_int, c_void, CStr, CString};
use std::ptr::null_mut;

#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct OsdString(pub String);

impl MpvFormat for String {
    const MPV_FORMAT: Format = Format::STRING;
}

impl From<String> for Node {
    fn from(string: String) -> Self {
        Node::String(string)
    }
}

impl From<&String> for Node {
    fn from(string: &String) -> Self {
        Node::String(string.clone())
    }
}

impl From<&str> for Node {
    fn from(string: &str) -> Self {
        Node::String(string.to_string())
    }
}

impl MpvRecv for String {}
impl MpvRecvInternal for String {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::Result<Self> {
        check_null!(ptr);
        let cstr = unsafe { *(ptr as *const *const c_char) };

        check_null!(cstr);
        Ok(unsafe { CStr::from_ptr(cstr) }.to_str()?.to_string())
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::Result<()>>(
        fun: F,
    ) -> crate::types::Result<Self> {
        let mut cstr: *mut c_char = null_mut();

        fun(&raw mut cstr as *mut c_void).and_then(|_| {
            let ret = unsafe { Self::from_ptr(&raw mut cstr as *const c_void) };
            unsafe { mpv_free(cstr as *mut c_void) };
            ret
        })
    }
}

impl MpvSend for String {}
impl MpvSendInternal for String {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        let c_string = CString::new(self.as_bytes())?;
        let cstr = c_string.as_ptr();
        fun(&raw const cstr as *mut c_void)
    }
}

impl MpvFormat for &str {
    const MPV_FORMAT: Format = Format::STRING;
}

impl MpvSend for &str {}
impl MpvSendInternal for &str {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        let cstring = CString::new(*self)?;
        let cstr = cstring.as_ptr();

        fun(&raw const cstr as *mut c_void)
    }
}

impl MpvFormat for OsdString {
    const MPV_FORMAT: Format = Format::OSD_STRING;
}

impl MpvRecv for OsdString {}
impl MpvRecvInternal for OsdString {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::Result<Self> {
        Ok(OsdString(unsafe { String::from_ptr(ptr)? }))
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::Result<()>>(
        fun: F,
    ) -> crate::types::Result<Self> {
        unsafe { String::from_mpv(fun) }.map(Self)
    }
}

impl MpvSend for OsdString {}
impl MpvSendInternal for OsdString {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        self.0.to_mpv(fun)
    }
}

impl MpvFormat for bool {
    const MPV_FORMAT: Format = Format::FLAG;
}

impl From<bool> for Node {
    fn from(value: bool) -> Self {
        Node::Flag(value)
    }
}

impl MpvRecv for bool {}
impl MpvRecvInternal for bool {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::Result<Self> {
        check_null!(ptr);
        Ok(unsafe { *(ptr as *const c_int) != 0 })
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::Result<()>>(
        fun: F,
    ) -> crate::types::Result<Self> {
        let mut flag: c_int = 0;
        fun(&raw mut flag as *mut c_void).map(|_| flag != 0)
    }
}

impl MpvSend for bool {}
impl MpvSendInternal for bool {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        let flag = if *self { 1 } else { 0 };
        fun(&raw const flag as *mut c_void)
    }
}

impl MpvFormat for i64 {
    const MPV_FORMAT: Format = Format::INT64;
}

impl From<i64> for Node {
    fn from(value: i64) -> Self {
        Node::Int64(value)
    }
}

impl MpvRecv for i64 {}
impl MpvRecvInternal for i64 {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::Result<Self> {
        check_null!(ptr);
        Ok(unsafe { *(ptr as *const Self) })
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::Result<()>>(
        fun: F,
    ) -> crate::types::Result<Self> {
        let mut val: Self = 0;
        fun(&raw mut val as *mut c_void).map(|_| val)
    }
}

impl MpvSend for i64 {}
impl MpvSendInternal for i64 {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        fun(self as *const Self as *mut c_void)
    }
}

impl MpvFormat for f64 {
    const MPV_FORMAT: Format = Format::DOUBLE;
}

impl From<f64> for Node {
    fn from(value: f64) -> Self {
        Node::Double(value)
    }
}

impl MpvRecv for f64 {}
impl MpvRecvInternal for f64 {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::Result<Self> {
        check_null!(ptr);
        Ok(unsafe { *(ptr as *const Self) })
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::Result<()>>(
        fun: F,
    ) -> crate::types::Result<Self> {
        let mut val: Self = 0.0;
        fun(&raw mut val as *mut c_void).map(|_| val)
    }
}

impl MpvSend for f64 {}
impl MpvSendInternal for f64 {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::Result<R> {
        fun(self as *const Self as *mut c_void)
    }
}
