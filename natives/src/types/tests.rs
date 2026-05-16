#![cfg(test)]
#![allow(non_upper_case_globals)]

use crate::test_utils;
use crate::types::byte_array::ByteArray;
use crate::types::node::Node;
use crate::types::node_array::NodeArray;
use crate::types::node_map::NodeMap;
use crate::types::traits::{MpvRecvInternal, MpvSendInternal, ToMpvRepr};
use libmpv2_sys::{
    mpv_byte_array, mpv_format_MPV_FORMAT_DOUBLE, mpv_format_MPV_FORMAT_FLAG,
    mpv_format_MPV_FORMAT_INT64, mpv_format_MPV_FORMAT_NODE_ARRAY, mpv_format_MPV_FORMAT_NODE_MAP,
    mpv_format_MPV_FORMAT_NONE, mpv_format_MPV_FORMAT_STRING, mpv_node, mpv_node__bindgen_ty_1,
    mpv_node_list,
};
use std::bstr::ByteStr;
use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_void, CStr, CString};
use std::ptr::null_mut;

#[test]
fn string_from_mpv() {
    let cstring = CString::new("hello, world!").unwrap();

    let string = unsafe {
        String::from_mpv(|x| {
            *(x as *mut *const c_char) = cstring.as_ptr();
            Ok(())
        })
    };

    assert_eq!(string.unwrap(), cstring.to_str().unwrap());

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 1);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn string_to_mpv() {
    let string = String::from("hello, world!");

    string
        .to_mpv(|x| {
            let cstr: *const c_char = unsafe { *(x as *const *const c_char) };
            let cstr = unsafe { CStr::from_ptr(cstr) };
            let str = cstr.to_str().unwrap();
            assert_eq!(str, string);
            Ok(0)
        })
        .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn flag_true_from_mpv() {
    let cflag: c_int = 1;

    let flag = unsafe {
        bool::from_mpv(|x| {
            *(x as *mut c_int) = cflag;
            Ok(())
        })
    };

    assert!(flag.unwrap());

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn flag_false_from_mpv() {
    let cflag: c_int = 0;

    let flag = unsafe {
        bool::from_mpv(|x| {
            *(x as *mut c_int) = cflag;
            Ok(())
        })
    };

    assert!(!flag.unwrap());

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn flag_true_to_mpv() {
    let flag = true;
    flag.to_mpv(|x| {
        let cflag = unsafe { *(x as *const c_int) };
        assert_eq!(cflag, 1);
        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn flag_false_to_mpv() {
    let flag = false;
    flag.to_mpv(|x| {
        let cflag = unsafe { *(x as *const c_int) };
        assert_eq!(cflag, 0);
        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn int64_from_mpv() {
    let cval: i64 = 123456;

    let val = unsafe {
        i64::from_mpv(|x| {
            *(x as *mut i64) = cval;
            Ok(())
        })
    };

    assert_eq!(val.unwrap(), cval);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn int64_to_mpv() {
    let val: i64 = 654321;

    val.to_mpv(|x| {
        let cval = unsafe { *(x as *const i64) };
        assert_eq!(val, cval);
        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn double_from_mpv() {
    let cval: f64 = 456.789;

    let val = unsafe {
        f64::from_mpv(|x| {
            *(x as *mut f64) = cval;
            Ok(())
        })
    };

    assert_eq!(val.unwrap(), cval);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn double_to_mpv() {
    let val: f64 = 987.654;

    val.to_mpv(|x| {
        let cval = unsafe { *(x as *const f64) };
        assert_eq!(val, cval);
        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn node_none_from_mpv() {
    let cnode = mpv_node {
        u: mpv_node__bindgen_ty_1 { flag: 0 },
        format: mpv_format_MPV_FORMAT_NONE,
    };

    let node = unsafe {
        Node::from_mpv(|x| {
            *(x as *mut mpv_node) = cnode;
            Ok(())
        })
    }
    .unwrap();

    assert_eq!(node, Node::None);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 1);
}

#[test]
fn node_none_to_mpv() {
    let node = Node::None;

    node.to_mpv(|x| {
        let cnode = unsafe { *(x as *const mpv_node) };

        assert_eq!(cnode.format, mpv_format_MPV_FORMAT_NONE);
        Ok(())
    })
    .unwrap();
}

#[test]
fn node_string_from_mpv() {
    let cstring = CString::new("hello, world!").unwrap();
    let cnode = mpv_node {
        u: mpv_node__bindgen_ty_1 {
            string: cstring.as_ptr() as *mut c_char,
        },
        format: mpv_format_MPV_FORMAT_STRING,
    };

    let node = unsafe {
        Node::from_mpv(|x| {
            *(x as *mut mpv_node) = cnode;
            Ok(())
        })
    }
    .unwrap();

    assert_eq!(node, Node::String(String::from("hello, world!")));

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 1);
}

#[test]
fn node_string_to_mpv() {
    let node = Node::String(String::from("hello, world!"));

    node.to_mpv(|x| {
        let cnode = unsafe { *(x as *const mpv_node) };

        assert_eq!(cnode.format, mpv_format_MPV_FORMAT_STRING);

        let cstr = unsafe { CStr::from_ptr(cnode.u.string) };
        assert_eq!(cstr.to_str().unwrap(), "hello, world!");

        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn node_flag_from_mpv() {
    let cnode = mpv_node {
        u: mpv_node__bindgen_ty_1 { flag: 1 },
        format: mpv_format_MPV_FORMAT_FLAG,
    };

    let node = unsafe {
        Node::from_mpv(|x| {
            *(x as *mut mpv_node) = cnode;
            Ok(())
        })
    }
    .unwrap();

    assert_eq!(node, Node::Flag(true));

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 1);
}

#[test]
fn node_flag_to_mpv() {
    let node = Node::Flag(true);

    node.to_mpv(|x| {
        let cnode = unsafe { *(x as *const mpv_node) };

        assert_eq!(cnode.format, mpv_format_MPV_FORMAT_FLAG);

        let cflag = unsafe { cnode.u.flag };
        assert_eq!(cflag, 1);

        Ok(())
    })
    .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn node_complex_from_mpv() {
    let c_five_string = CString::new("five").unwrap();
    let c_counting_array_map_buffer = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                string: c_five_string.as_ptr() as *mut c_char,
            },
            format: mpv_format_MPV_FORMAT_STRING,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 7 },
            format: mpv_format_MPV_FORMAT_INT64,
        },
    ];

    let four_label = CString::new("4").unwrap();
    let six_label = CString::new("six").unwrap();
    let c_counting_array_map_labels = [four_label.as_ptr(), six_label.as_ptr()];

    let c_counting_array_map = mpv_node_list {
        num: 2,
        values: c_counting_array_map_buffer.as_ptr() as *mut mpv_node,
        keys: c_counting_array_map_labels.as_ptr() as *mut *mut c_char,
    };

    let c_counting_array_buffer = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 0 },
            format: mpv_format_MPV_FORMAT_NONE,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { flag: 1 },
            format: mpv_format_MPV_FORMAT_FLAG,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 2 },
            format: mpv_format_MPV_FORMAT_INT64,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { double_: 3.3 },
            format: mpv_format_MPV_FORMAT_DOUBLE,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                list: &raw const c_counting_array_map as *mut mpv_node_list,
            },
            format: mpv_format_MPV_FORMAT_NODE_MAP,
        },
    ];

    let c_counting_array = mpv_node_list {
        num: 5,
        values: c_counting_array_buffer.as_ptr() as *mut mpv_node,
        keys: null_mut(),
    };

    let c_hello_string = CString::new("hello, world!").unwrap();
    let c_types_map_buffer = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 0 },
            format: mpv_format_MPV_FORMAT_NONE,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                string: c_hello_string.as_ptr() as *mut c_char,
            },
            format: mpv_format_MPV_FORMAT_STRING,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { flag: 1 },
            format: mpv_format_MPV_FORMAT_FLAG,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 123456 },
            format: mpv_format_MPV_FORMAT_INT64,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { double_: 456.789 },
            format: mpv_format_MPV_FORMAT_DOUBLE,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                list: &raw const c_counting_array as *mut mpv_node_list,
            },
            format: mpv_format_MPV_FORMAT_NODE_ARRAY,
        },
    ];

    let none_label = CString::new("none").unwrap();
    let string_label = CString::new("string").unwrap();
    let flag_label = CString::new("flag").unwrap();
    let int64_label = CString::new("int64").unwrap();
    let double_label = CString::new("double").unwrap();
    let array_label = CString::new("array").unwrap();
    let c_types_map_labels = [
        none_label.as_ptr(),
        string_label.as_ptr(),
        flag_label.as_ptr(),
        int64_label.as_ptr(),
        double_label.as_ptr(),
        array_label.as_ptr(),
    ];

    let c_types_map = mpv_node_list {
        num: c_types_map_buffer.len() as c_int,
        values: c_types_map_buffer.as_ptr() as *mut mpv_node,
        keys: c_types_map_labels.as_ptr() as *mut *mut c_char,
    };

    let cnodelistbuffer = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                list: &raw const c_types_map as *mut mpv_node_list,
            },
            format: mpv_format_MPV_FORMAT_NODE_MAP,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                list: &raw const c_types_map as *mut mpv_node_list,
            },
            format: mpv_format_MPV_FORMAT_NODE_MAP,
        },
    ];

    let cnodelist = mpv_node_list {
        num: 2,
        values: cnodelistbuffer.as_ptr() as *mut mpv_node,
        keys: null_mut(),
    };

    let cnode = mpv_node {
        u: mpv_node__bindgen_ty_1 {
            list: &raw const cnodelist as *mut mpv_node_list,
        },
        format: mpv_format_MPV_FORMAT_NODE_ARRAY,
    };

    let node = unsafe {
        Node::from_mpv(|x| {
            *(x as *mut mpv_node) = cnode;
            Ok(())
        })
    }
    .unwrap();

    let test_map = Node::Map(HashMap::from([
        ("none".to_string(), Node::None),
        (
            "string".to_string(),
            Node::String("hello, world!".to_string()),
        ),
        ("flag".to_string(), Node::Flag(true)),
        ("int64".to_string(), Node::Int64(123456)),
        ("double".to_string(), Node::Double(456.789)),
        (
            "array".to_string(),
            Node::Array(vec![
                Node::None,
                Node::Flag(true),
                Node::Int64(2),
                Node::Double(3.3),
                Node::Map(HashMap::from([
                    ("4".to_string(), Node::String("five".to_string())),
                    ("six".to_string(), Node::Int64(7)),
                ])),
            ]),
        ),
    ]));

    let test_node = Node::Array(vec![test_map.clone(), test_map]);

    assert_eq!(node, test_node);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 1);
}

#[test]
fn node_complex_to_mpv() {
    let test_map = Node::Map(HashMap::from([
        ("none".to_string(), Node::None),
        (
            "string".to_string(),
            Node::String("hello, world!".to_string()),
        ),
        ("flag".to_string(), Node::Flag(true)),
        ("int64".to_string(), Node::Int64(123456)),
        ("double".to_string(), Node::Double(456.789)),
        (
            "array".to_string(),
            Node::Array(vec![
                Node::None,
                Node::Flag(true),
                Node::Int64(2),
                Node::Double(3.3),
                Node::Map(HashMap::from([
                    ("4".to_string(), Node::String("five".to_string())),
                    ("six".to_string(), Node::Int64(7)),
                ])),
            ]),
        ),
    ]));

    let test_node = Node::Array(vec![test_map.clone(), test_map]);

    println!("{test_node:?}");

    test_node
        .to_mpv(|x| {
            let root_node = unsafe { *(x as *const mpv_node) };

            assert_eq!(root_node.format, mpv_format_MPV_FORMAT_NODE_ARRAY);

            let root_list = unsafe { *root_node.u.list };
            assert_eq!(root_list.num, 2);

            let types_map_nodes =
                unsafe { std::slice::from_raw_parts(root_list.values, root_list.num as usize) };

            for types_map_node in types_map_nodes {
                assert_eq!(types_map_node.format, mpv_format_MPV_FORMAT_NODE_MAP);

                let types_map_list = unsafe { *types_map_node.u.list };

                assert_eq!(types_map_list.num, 6);
                let types_nodes = unsafe { std::slice::from_raw_parts(types_map_list.values, 6) };
                let types_labels = unsafe { std::slice::from_raw_parts(types_map_list.keys, 6) };

                let mut cnt_none = 0;
                let mut cnt_string = 0;
                let mut cnt_flag = 0;
                let mut cnt_int64 = 0;
                let mut cnt_double = 0;
                let mut cnt_array = 0;

                for idx_type in 0..6 {
                    let type_node = types_nodes[idx_type];
                    let type_label = unsafe { CStr::from_ptr(types_labels[idx_type]) }
                        .to_str()
                        .unwrap();

                    match type_node.format {
                        mpv_format_MPV_FORMAT_NONE => {
                            cnt_none += 1;
                        }
                        mpv_format_MPV_FORMAT_STRING => {
                            assert_eq!(
                                unsafe { CStr::from_ptr(type_node.u.string) }
                                    .to_str()
                                    .unwrap(),
                                "hello, world!"
                            );
                            assert_eq!(type_label, "string");
                            cnt_string += 1;
                        }
                        mpv_format_MPV_FORMAT_FLAG => {
                            assert_eq!(unsafe { type_node.u.flag }, 1);
                            assert_eq!(type_label, "flag");
                            cnt_flag += 1;
                        }
                        mpv_format_MPV_FORMAT_INT64 => {
                            assert_eq!(unsafe { type_node.u.int64 }, 123456);
                            assert_eq!(type_label, "int64");
                            cnt_int64 += 1;
                        }
                        mpv_format_MPV_FORMAT_DOUBLE => {
                            assert_eq!(unsafe { type_node.u.double_ }, 456.789);
                            assert_eq!(type_label, "double");
                            cnt_double += 1;
                        }
                        mpv_format_MPV_FORMAT_NODE_ARRAY => {
                            assert_eq!(type_label, "array");
                            cnt_array += 1;

                            let counting_array_list = unsafe { *(type_node.u.list) };

                            assert_eq!(counting_array_list.num, 5);
                            let counting_array_nodes = unsafe {
                                std::slice::from_raw_parts(counting_array_list.values, 5)
                            };

                            assert_eq!(counting_array_nodes[0].format, mpv_format_MPV_FORMAT_NONE);

                            assert_eq!(counting_array_nodes[1].format, mpv_format_MPV_FORMAT_FLAG);
                            assert_eq!(unsafe { counting_array_nodes[1].u.flag }, 1);

                            assert_eq!(counting_array_nodes[2].format, mpv_format_MPV_FORMAT_INT64);
                            assert_eq!(unsafe { counting_array_nodes[2].u.int64 }, 2);

                            assert_eq!(
                                counting_array_nodes[3].format,
                                mpv_format_MPV_FORMAT_DOUBLE
                            );
                            assert_eq!(unsafe { counting_array_nodes[3].u.double_ }, 3.3);

                            assert_eq!(
                                counting_array_nodes[4].format,
                                mpv_format_MPV_FORMAT_NODE_MAP
                            );

                            let final_count_list = unsafe { *(counting_array_nodes[4].u.list) };

                            assert_eq!(final_count_list.num, 2);
                            let final_count_nodes =
                                unsafe { std::slice::from_raw_parts(final_count_list.values, 2) };
                            let final_count_labels =
                                unsafe { std::slice::from_raw_parts(final_count_list.keys, 2) };

                            let mut cnt_4 = 0;
                            let mut cnt_6 = 0;

                            for idx_final_count in 0..2 {
                                let final_count_node = final_count_nodes[idx_final_count];
                                let final_count_label =
                                    unsafe { CStr::from_ptr(final_count_labels[idx_final_count]) }
                                        .to_str()
                                        .unwrap();

                                match final_count_node.format {
                                    mpv_format_MPV_FORMAT_STRING => {
                                        assert_eq!(
                                            unsafe { CStr::from_ptr(final_count_node.u.string) }
                                                .to_str()
                                                .unwrap(),
                                            "five"
                                        );
                                        assert_eq!(final_count_label, "4");
                                        cnt_4 += 1;
                                    }
                                    mpv_format_MPV_FORMAT_INT64 => {
                                        assert_eq!(unsafe { final_count_node.u.int64 }, 7);
                                        assert_eq!(final_count_label, "six");
                                        cnt_6 += 1;
                                    }
                                    _ => unreachable!(),
                                }
                            }

                            assert_eq!(cnt_4, 1);
                            assert_eq!(cnt_6, 1);
                        }
                        _ => unreachable!(),
                    }
                }

                assert_eq!(cnt_none, 1);
                assert_eq!(cnt_string, 1);
                assert_eq!(cnt_flag, 1);
                assert_eq!(cnt_int64, 1);
                assert_eq!(cnt_double, 1);
                assert_eq!(cnt_array, 1);
            }

            Ok(())
        })
        .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn nodemap_from_mpv() {
    let cstring = CString::new("hello, world!").unwrap();
    let cnodes = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                string: cstring.as_ptr() as *mut c_char,
            },
            format: mpv_format_MPV_FORMAT_STRING,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { flag: 1 },
            format: mpv_format_MPV_FORMAT_FLAG,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 123456 },
            format: mpv_format_MPV_FORMAT_INT64,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { double_: 456.789 },
            format: mpv_format_MPV_FORMAT_DOUBLE,
        },
    ];

    let first_label = CString::new("first").unwrap();
    let second_label = CString::new("second").unwrap();
    let third_label = CString::new("third").unwrap();
    let fourth_label = CString::new("fourth").unwrap();
    let labels = [
        first_label.as_ptr(),
        second_label.as_ptr(),
        third_label.as_ptr(),
        fourth_label.as_ptr(),
    ];

    let cnodelist = mpv_node_list {
        num: cnodes.len() as c_int,
        values: cnodes.as_ptr() as *mut mpv_node,
        keys: labels.as_ptr() as *mut *mut c_char,
    };

    let nodemap = unsafe {
        NodeMap::from_mpv(|x| {
            *(x as *mut mpv_node_list) = cnodelist;
            Ok(())
        })
    }
    .unwrap();

    let map = HashMap::from([
        (
            "first".to_string(),
            Node::String("hello, world!".to_string()),
        ),
        ("second".to_string(), Node::Flag(true)),
        ("third".to_string(), Node::Int64(123456)),
        ("fourth".to_string(), Node::Double(456.789)),
    ]);

    assert_eq!(nodemap, map);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn nodemap_to_mpv() {
    let map = HashMap::from([
        (
            "first".to_string(),
            Node::String("hello, world!".to_string()),
        ),
        ("second".to_string(), Node::Flag(true)),
        ("third".to_string(), Node::Int64(123456)),
        ("fourth".to_string(), Node::Double(456.789)),
    ]);

    let nodemap = map;

    nodemap
        .to_mpv(|x| {
            let cnodelist = unsafe { *(x as *const mpv_node_list) };

            assert_eq!(cnodelist.num, 4);

            let mut cnt_string = 0;
            let mut cnt_flags = 0;
            let mut cnt_int64 = 0;
            let mut cnt_double = 0;

            for x in 0..4 {
                let node = unsafe { cnodelist.values.offset(x).read() };
                let label = unsafe { CStr::from_ptr(cnodelist.keys.offset(x).read()) }
                    .to_str()
                    .unwrap();

                match node.format {
                    mpv_format_MPV_FORMAT_STRING => {
                        assert_eq!(
                            unsafe { CStr::from_ptr(node.u.string) }.to_str().unwrap(),
                            "hello, world!"
                        );
                        assert_eq!(label, "first");
                        cnt_string += 1;
                    }
                    mpv_format_MPV_FORMAT_FLAG => {
                        assert_eq!(unsafe { node.u.flag }, 1);
                        assert_eq!(label, "second");
                        cnt_flags += 1;
                    }
                    mpv_format_MPV_FORMAT_INT64 => {
                        assert_eq!(unsafe { node.u.int64 }, 123456);
                        assert_eq!(label, "third");
                        cnt_int64 += 1;
                    }
                    mpv_format_MPV_FORMAT_DOUBLE => {
                        assert_eq!(unsafe { node.u.double_ }, 456.789);
                        assert_eq!(label, "fourth");
                        cnt_double += 1;
                    }
                    _ => unreachable!(),
                }
            }

            assert_eq!(cnt_string, 1);
            assert_eq!(cnt_flags, 1);
            assert_eq!(cnt_int64, 1);
            assert_eq!(cnt_double, 1);

            Ok(())
        })
        .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn nodearray_from_mpv() {
    let cstring = CString::new("hello, world!").unwrap();
    let cnodes = [
        mpv_node {
            u: mpv_node__bindgen_ty_1 {
                string: cstring.as_ptr() as *mut c_char,
            },
            format: mpv_format_MPV_FORMAT_STRING,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { flag: 1 },
            format: mpv_format_MPV_FORMAT_FLAG,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { int64: 123456 },
            format: mpv_format_MPV_FORMAT_INT64,
        },
        mpv_node {
            u: mpv_node__bindgen_ty_1 { double_: 456.789 },
            format: mpv_format_MPV_FORMAT_DOUBLE,
        },
    ];

    let cnodelist = mpv_node_list {
        num: cnodes.len() as c_int,
        values: cnodes.as_ptr() as *mut mpv_node,
        keys: null_mut(),
    };

    let nodearray = unsafe {
        NodeArray::from_mpv(|x| {
            *(x as *mut mpv_node_list) = cnodelist;
            Ok(())
        })
    }
    .unwrap();

    assert_eq!(
        nodearray,
        vec![
            Node::String(String::from("hello, world!")),
            Node::Flag(true),
            Node::Int64(123456),
            Node::Double(456.789)
        ]
    );

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn nodearray_to_mpv() {
    let nodearray = vec![
        Node::String(String::from("hello, world!")),
        Node::Flag(true),
        Node::Int64(123456),
        Node::Double(456.789),
    ];

    nodearray
        .to_mpv(|x| {
            let cnodelist = unsafe { *(x as *const mpv_node_list) };

            assert_eq!(cnodelist.num, 4);
            assert_eq!(cnodelist.keys, null_mut());

            let first = unsafe { cnodelist.values.offset(0).read() };
            assert_eq!(first.format, mpv_format_MPV_FORMAT_STRING);
            assert_eq!(
                unsafe { CStr::from_ptr(first.u.string) }.to_str().unwrap(),
                "hello, world!"
            );

            let second = unsafe { cnodelist.values.offset(1).read() };
            assert_eq!(second.format, mpv_format_MPV_FORMAT_FLAG);
            assert_eq!(unsafe { second.u.flag }, 1);

            let third = unsafe { cnodelist.values.offset(2).read() };
            assert_eq!(third.format, mpv_format_MPV_FORMAT_INT64);
            assert_eq!(unsafe { third.u.int64 }, 123456);

            let fourth = unsafe { cnodelist.values.offset(3).read() };
            assert_eq!(fourth.format, mpv_format_MPV_FORMAT_DOUBLE);
            assert_eq!(unsafe { fourth.u.double_ }, 456.789);

            Ok(())
        })
        .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn ba_from_mpv() {
    let cbuffer: [u8; 8] = [0x10, 0x20, 0x30, 0x40, 0xA0, 0xB0, 0xC0, 0xD0];
    let cba = mpv_byte_array {
        data: cbuffer.as_ptr() as *mut c_void,
        size: 8,
    };

    let buffer = unsafe {
        ByteArray::from_mpv(|x| {
            *(x as *mut mpv_byte_array) = cba;
            Ok(())
        })
    };

    assert_eq!(buffer.unwrap(), cbuffer);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn ba_to_mpv() {
    let buffer: ByteArray =
        ByteStr::new(&[0xF0, 0xE0, 0xD0, 0xC0, 0x80, 0x70, 0x60, 0x50]).to_owned();

    buffer
        .to_mpv(|x| {
            let ba = unsafe { *(x as *const mpv_byte_array) };

            let cbuffer = unsafe { std::slice::from_raw_parts(ba.data as *const u8, ba.size) };

            assert_eq!(buffer, cbuffer);
            Ok(())
        })
        .unwrap();

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}

#[test]
fn node_huge_round_trip() {
    let hello_worlds = Node::Array(vec![
        Node::String(String::from("hello, world!1")),
        Node::String(String::from("hello, world!2")),
        Node::String(String::from("hello, world!3")),
        Node::String(String::from("hello, world!4")),
        Node::String(String::from("hello, world!5")),
        Node::String(String::from("hello, world!6")),
        Node::String(String::from("hello, world!7")),
        Node::String(String::from("hello, world!8")),
        Node::String(String::from("hello, world!9")),
    ]);

    let hello_map = Node::Map(HashMap::from([
        ("hello1".to_string(), hello_worlds.clone()),
        ("hello2".to_string(), hello_worlds.clone()),
        ("hello3".to_string(), hello_worlds.clone()),
        ("hello4".to_string(), hello_worlds.clone()),
        ("hello5".to_string(), hello_worlds.clone()),
        ("hello6".to_string(), hello_worlds.clone()),
        ("hello7".to_string(), hello_worlds.clone()),
        ("hello8".to_string(), hello_worlds.clone()),
        ("hello9".to_string(), hello_worlds.clone()),
    ]));

    let number_map = Node::Map(HashMap::from([
        ("1".to_string(), hello_map.clone()),
        ("2".to_string(), hello_map.clone()),
        ("3".to_string(), hello_map.clone()),
        ("4".to_string(), hello_map.clone()),
        ("5".to_string(), hello_map.clone()),
        ("6".to_string(), hello_map.clone()),
        ("7".to_string(), hello_map.clone()),
        ("8".to_string(), hello_map.clone()),
        ("9".to_string(), hello_map.clone()),
    ]));

    let number_array = Node::Array(vec![
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
        number_map.clone(),
    ]);

    let node = Node::Map(HashMap::from([
        ("number_array".to_string(), number_array.clone()),
        ("number_map".to_string(), number_map.clone()),
        ("hello_map".to_string(), hello_map.clone()),
        ("hello_worlds".to_string(), hello_worlds.clone()),
        ("hello_worlds2".to_string(), hello_worlds.clone()),
        ("hello_map2".to_string(), hello_map.clone()),
        ("number_map2".to_string(), number_map.clone()),
        ("number_array2".to_string(), number_array.clone()),
    ]));

    let mpv_repr = node.to_mpv_repr().unwrap();

    let test_node = unsafe { Node::from_node_ptr(&raw const *mpv_repr.node) }.unwrap();

    assert_eq!(test_node, node);

    assert_eq!(test_utils::MPV_FREE_CALLS.get(), 0);
    assert_eq!(test_utils::MPV_FREE_NODE_CONTENTS_CALLS.get(), 0);
}
