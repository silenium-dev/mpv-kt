use libmpv2_sys::mpv_handle;
use std::ptr::NonNull;

pub(crate) struct MpvHandle(NonNull<mpv_handle>);
// mpv_handle is safe for concurrent use
unsafe impl Send for MpvHandle {}
// mpv_handle is safe for concurrent use
unsafe impl Sync for MpvHandle {}
impl MpvHandle {
    pub fn new(handle: NonNull<mpv_handle>) -> Self {
        Self(handle)
    }
}
impl From<NonNull<mpv_handle>> for MpvHandle {
    fn from(handle: NonNull<mpv_handle>) -> Self {
        Self::new(handle)
    }
}
impl From<*mut mpv_handle> for MpvHandle {
    fn from(handle: *mut mpv_handle) -> Self {
        Self::new(NonNull::new(handle).unwrap())
    }
}
impl Into<*mut mpv_handle> for &MpvHandle {
    fn into(self) -> *mut mpv_handle {
        self.0.as_ptr()
    }
}
impl Into<NonNull<mpv_handle>> for &MpvHandle {
    fn into(self) -> NonNull<mpv_handle> {
        self.0
    }
}
