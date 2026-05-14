pub mod node;

#[cfg(test)]
mod test {
    use super::*;
    use libmpv2_sys::{mpv_node, mpv_node__bindgen_ty_1, mpv_node_list};
    use std::bstr::{ByteStr, ByteString};
    use std::ffi::CString;

    #[test]
    fn parse_none() {
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_NONE,
            u: mpv_node__bindgen_ty_1 { flag: 0 },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(node, node::MpvNode::None);
    }
    #[test]
    fn parse_flag() {
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_FLAG,
            u: mpv_node__bindgen_ty_1 { flag: 1 },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(node, node::MpvNode::Flag(true));
    }
    #[test]
    fn parse_int64() {
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_INT64,
            u: mpv_node__bindgen_ty_1 { int64: 123 },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(node, node::MpvNode::Int64(123));
    }
    #[test]
    fn parse_double() {
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_DOUBLE,
            u: mpv_node__bindgen_ty_1 { double_: 123.45 },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(node, node::MpvNode::Double(123.45));
    }
    #[test]
    fn parse_string() {
        let c_str = std::ffi::CString::new("test").unwrap();
        let b_str = ByteString::from(ByteStr::new(c_str.to_bytes()));
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_STRING,
            u: mpv_node__bindgen_ty_1 {
                string: c_str.into_raw(),
            },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(node, node::MpvNode::String(b_str));
    }
    #[test]
    fn parse_node_array() {
        let elem = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_FLAG,
            u: mpv_node__bindgen_ty_1 { flag: 1 },
        };
        let mut values = [elem];
        let mut array = mpv_node_list {
            num: 1,
            keys: std::ptr::null_mut(),
            values: values.as_mut_ptr(),
        };
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_NODE_ARRAY,
            u: mpv_node__bindgen_ty_1 {
                list: std::ptr::from_mut(&mut array),
            },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(
            node,
            node::MpvNode::NodeArray(vec![node::MpvNode::Flag(true)])
        );
    }
    #[test]
    fn parse_node_map() {
        let key = CString::new("test").unwrap();
        let b_key = ByteStr::new(key.as_bytes()).to_owned();
        let mut keys = [key.into_raw()];
        let value = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_FLAG,
            u: mpv_node__bindgen_ty_1 { flag: 1 },
        };
        let mut values = [value];
        let mut map = mpv_node_list {
            num: 1,
            keys: keys.as_mut_ptr(),
            values: values.as_mut_ptr(),
        };
        let raw = mpv_node {
            format: libmpv2_sys::mpv_format_MPV_FORMAT_NODE_MAP,
            u: mpv_node__bindgen_ty_1 {
                list: std::ptr::from_mut(&mut map),
            },
        };
        let node = node::MpvNode::from(&raw);
        assert_eq!(
            node,
            node::MpvNode::NodeMap(std::collections::HashMap::from([(
                b_key,
                Box::new(node::MpvNode::Flag(true))
            )]))
        )
    }
}
