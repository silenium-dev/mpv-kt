#![feature(bstr)]

#[macro_use]
pub(crate) mod macros;

pub(crate) mod core;
pub(crate) mod ffi;
pub(crate) mod handle;
pub(crate) mod test_utils;

pub mod events;
pub mod jni_bindings;
pub mod mpv;
pub mod types;
