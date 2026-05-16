#![cfg(test)]

use jni::{InitArgsBuilder, JNIVersion, JavaVM};

pub(crate) fn create_jvm() -> JavaVM {
    let args = InitArgsBuilder::new()
        .version(JNIVersion::V1_8)
        .option("-Djava.class.path=target/test-classes")
        .build()
        .expect("failed to build JVM args");

    JavaVM::new(args).expect("failed to create JVM")
}

use libmpv2_sys::mpv_node;
use std::cell::Cell;
use std::ffi::c_void;

thread_local! {
    pub(crate) static MPV_FREE_CALLS: Cell<usize> = Cell::new(0);
    pub(crate) static MPV_FREE_NODE_CONTENTS_CALLS: Cell<usize> = Cell::new(0);
}

pub(crate) fn mpv_free_stub(_data: *mut c_void) {
    MPV_FREE_CALLS.set(MPV_FREE_CALLS.get() + 1);
}

pub(crate) fn mpv_free_node_contents_stub(_node: *mut mpv_node) {
    MPV_FREE_NODE_CONTENTS_CALLS.set(MPV_FREE_NODE_CONTENTS_CALLS.get() + 1);
}
