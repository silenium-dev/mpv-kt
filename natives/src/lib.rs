#![feature(bstr)]

use libmpv2_sys::mpv_node;
use std::ffi::c_void;

#[macro_use]
mod macros;
mod core;
pub mod events;
mod handle;
pub mod mpv;
mod test_utils;
pub mod types;
pub mod jni_bindings;

pub(crate) unsafe fn mpv_free(data: *mut c_void) {
    #[cfg(not(test))]
    unsafe {
        libmpv2_sys::mpv_free(data);
    }
    #[cfg(test)]
    test_utils::mpv_free_stub(data);
}

pub(crate) unsafe fn mpv_free_node_contents(node: *mut mpv_node) {
    #[cfg(not(test))]
    unsafe {
        libmpv2_sys::mpv_free_node_contents(node)
    }
    #[cfg(test)]
    test_utils::mpv_free_node_contents_stub(node);
}
