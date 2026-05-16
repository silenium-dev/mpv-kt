use libc::memcpy;
use std::bstr::ByteStr;
use std::ffi::CString;

/**
* Allocates a C string that is safe to call free() on
*/
pub(crate) unsafe fn c_string(s: &str) -> *mut libc::c_char {
    let c_string = CString::new(s).expect("CString::new failed");
    let bytes = c_string.as_bytes_with_nul();
    let ptr = unsafe { libc::malloc(bytes.len()) as *mut libc::c_char };
    unsafe { memcpy(ptr as *mut _, bytes.as_ptr() as *const _, bytes.len()) };
    ptr
}
pub(crate) unsafe fn c_bytestring(bs: &ByteStr) -> *mut libc::c_void {
    let ptr = unsafe { libc::malloc(bs.len()) };
    unsafe { memcpy(ptr as *mut _, bs.as_ptr() as *const _, bs.len()) };
    ptr
}
