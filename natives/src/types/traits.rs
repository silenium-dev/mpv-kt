use crate::types::errors::Result;
use crate::types::Format;
use std::ffi::c_void;

pub trait MpvFormat: Sized {
    const MPV_FORMAT: Format;
}

#[allow(private_bounds)]
pub trait MpvSend: MpvSendInternal {}
pub(crate) trait MpvSendInternal: MpvFormat {
    fn to_mpv<R, F: Fn(*mut c_void) -> Result<R>>(&self, fun: F) -> Result<R>;
}

#[allow(private_bounds)]
pub trait MpvRecv: MpvRecvInternal {}
pub(crate) trait MpvRecvInternal: MpvFormat {
    unsafe fn from_ptr(ptr: *const c_void) -> Result<Self>;
    unsafe fn from_mpv<F: Fn(*mut c_void) -> Result<()>>(fun: F) -> Result<Self>;
}

pub(crate) trait ToMpvRepr: MpvSend {
    type ReprWrap<'a>: MpvRepr
    where
        Self: 'a;

    fn to_mpv_repr(&self) -> Result<Self::ReprWrap<'_>>;
}

pub(crate) trait MpvRepr: Sized {
    type Repr;

    fn ptr(&self) -> *const Self::Repr;
}
