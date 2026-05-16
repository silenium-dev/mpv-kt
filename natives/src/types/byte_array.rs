use crate::types::node::Node;
use crate::types::traits::{
    MpvFormat, MpvRecv, MpvRecvInternal, MpvRepr, MpvSend, MpvSendInternal, ToMpvRepr,
};
use crate::types::Format;
use crate::types::Result as MpvResult;
use libmpv2_sys::mpv_byte_array;
use std::bstr::{ByteStr, ByteString};
use std::ffi::c_void;
use std::marker::PhantomData;
use std::mem::MaybeUninit;

pub type ByteArray = ByteString;

#[derive(Debug)]
pub(crate) struct MpvByteArray<'a> {
    _original: PhantomData<&'a ByteArray>,
    byte_array: Box<mpv_byte_array>,
}

impl MpvRepr for MpvByteArray<'_> {
    type Repr = mpv_byte_array;

    fn ptr(&self) -> *const Self::Repr {
        &raw const *self.byte_array
    }
}

impl MpvFormat for ByteArray {
    const MPV_FORMAT: Format = Format::BYTE_ARRAY;
}

impl From<ByteString> for Node {
    fn from(value: ByteString) -> Self {
        Node::ByteArray(value)
    }
}

impl From<&ByteStr> for Node {
    fn from(value: &ByteStr) -> Self {
        Node::ByteArray(value.to_owned())
    }
}

impl MpvRecv for ByteArray {}
impl MpvRecvInternal for ByteArray {
    unsafe fn from_ptr(ptr: *const c_void) -> MpvResult<Self> {
        check_null!(ptr);
        let byte_array = unsafe { *(ptr as *const mpv_byte_array) };
        check_null!(byte_array.data);
        Ok(unsafe {
            ByteStr::new(std::slice::from_raw_parts(
                byte_array.data as *const u8,
                byte_array.size,
            ))
            .to_owned()
        })
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> MpvResult<()>>(fun: F) -> MpvResult<Self> {
        let mut ba: MaybeUninit<mpv_byte_array> = MaybeUninit::uninit();

        fun(ba.as_mut_ptr() as *mut c_void)
            .map(|_| unsafe { Self::from_ptr(ba.as_ptr() as *const c_void) })?
    }
}

impl MpvSend for ByteArray {}
impl MpvSendInternal for ByteArray {
    fn to_mpv<R, F: Fn(*mut c_void) -> MpvResult<R>>(&self, fun: F) -> MpvResult<R> {
        let repr = self.to_mpv_repr()?;
        fun(repr.ptr() as *mut c_void)
    }
}

impl ToMpvRepr for ByteArray {
    type ReprWrap<'a> = MpvByteArray<'a>;

    fn to_mpv_repr(&self) -> crate::types::errors::Result<Self::ReprWrap<'_>> {
        Ok(MpvByteArray {
            _original: PhantomData,
            byte_array: Box::new(mpv_byte_array {
                data: self.0.as_ptr() as *mut c_void,
                size: self.0.len(),
            }),
        })
    }
}
