use crate::mpv_free_node_contents;
use crate::types::byte_array::{ByteArray, MpvByteArray};
use crate::types::node_array::{MpvNodeArray, NodeArray};
use crate::types::node_map::{MpvNodeMap, NodeMap};
use crate::types::traits::{
    MpvFormat, MpvRecv, MpvRecvInternal, MpvRepr, MpvSend, MpvSendInternal, ToMpvRepr,
};
use crate::types::Format;
use libmpv2_sys::{
    mpv_format_MPV_FORMAT_BYTE_ARRAY as MPV_FORMAT_BYTE_ARRAY,
    mpv_format_MPV_FORMAT_DOUBLE as MPV_FORMAT_DOUBLE,
    mpv_format_MPV_FORMAT_FLAG as MPV_FORMAT_FLAG, mpv_format_MPV_FORMAT_INT64 as MPV_FORMAT_INT64,
    mpv_format_MPV_FORMAT_NODE_ARRAY as MPV_FORMAT_NODE_ARRAY,
    mpv_format_MPV_FORMAT_NODE_MAP as MPV_FORMAT_NODE_MAP,
    mpv_format_MPV_FORMAT_NONE as MPV_FORMAT_NONE,
    mpv_format_MPV_FORMAT_OSD_STRING as MPV_FORMAT_OSD_STRING,
    mpv_format_MPV_FORMAT_STRING as MPV_FORMAT_STRING, mpv_node, mpv_node__bindgen_ty_1,
};
use std::ffi::{c_int, c_void, CStr, CString};
use std::marker::PhantomData;
use std::mem::MaybeUninit;

#[derive(Debug, Clone, PartialEq)]
pub enum Node {
    None,
    String(String),
    OsdString(String),
    Flag(bool),
    Int64(i64),
    Double(f64),
    Array(NodeArray),
    Map(NodeMap),
    ByteArray(ByteArray),
}

#[derive(Debug)]
pub(crate) struct MpvNode<'a> {
    _original: PhantomData<&'a Node>,

    _owned_cstring: Option<CString>,
    _array_repr: Option<MpvNodeArray<'a>>,
    _map_repr: Option<MpvNodeMap<'a>>,
    _bytes_repr: Option<MpvByteArray<'a>>,

    pub(crate) node: Box<mpv_node>,
}

impl MpvRepr for MpvNode<'_> {
    type Repr = mpv_node;

    fn ptr(&self) -> *const Self::Repr {
        &raw const *self.node
    }
}

impl MpvFormat for Node {
    const MPV_FORMAT: Format = Format::NODE;
}

impl MpvRecv for Node {}
impl MpvRecvInternal for Node {
    unsafe fn from_ptr(ptr: *const c_void) -> crate::types::errors::Result<Self> {
        unsafe { Self::from_node_ptr(ptr as *const mpv_node) }
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> crate::types::errors::Result<()>>(
        fun: F,
    ) -> crate::types::errors::Result<Self> {
        let mut node: MaybeUninit<mpv_node> = MaybeUninit::uninit();
        fun(node.as_mut_ptr() as *mut c_void).map(|_| {
            let ret = unsafe { Self::from_node_ptr(node.as_ptr()) };
            unsafe { mpv_free_node_contents(node.as_mut_ptr()) };
            ret
        })?
    }
}

impl MpvSend for Node {}
impl MpvSendInternal for Node {
    fn to_mpv<R, F: Fn(*mut c_void) -> crate::types::errors::Result<R>>(
        &self,
        fun: F,
    ) -> crate::types::errors::Result<R> {
        let repr = self.to_mpv_repr()?;

        fun(repr.ptr() as *mut c_void)
    }
}

impl ToMpvRepr for Node {
    type ReprWrap<'a> = MpvNode<'a>;

    fn to_mpv_repr(&self) -> crate::types::errors::Result<Self::ReprWrap<'_>> {
        let mut repr = MpvNode {
            _original: PhantomData,
            _owned_cstring: None,
            _array_repr: None,
            _map_repr: None,
            _bytes_repr: None,
            node: Box::new(mpv_node {
                u: mpv_node__bindgen_ty_1 { int64: 0 },
                format: 0,
            }),
        };
        match self {
            Node::None => {
                repr.node.format = MPV_FORMAT_NONE;
                repr.node.u = mpv_node__bindgen_ty_1 { flag: 0 };
            }
            Node::OsdString(s) => {
                repr._owned_cstring = Some(CString::new(s.as_bytes())?);
                let cstring_ptr = repr._owned_cstring.as_ref().unwrap().as_ptr();

                repr.node.u = mpv_node__bindgen_ty_1 {
                    string: cstring_ptr as *mut _,
                };
                repr.node.format = MPV_FORMAT_OSD_STRING;
            }
            Node::String(s) => {
                repr._owned_cstring = Some(CString::new(s.as_bytes())?);
                let cstring_ptr = repr._owned_cstring.as_ref().unwrap().as_ptr();

                repr.node.u = mpv_node__bindgen_ty_1 {
                    string: cstring_ptr as *mut _,
                };
                repr.node.format = MPV_FORMAT_STRING;
            }
            Node::Flag(b) => {
                repr.node.u = mpv_node__bindgen_ty_1 { flag: *b as c_int };
                repr.node.format = MPV_FORMAT_FLAG;
            }
            Node::Int64(i) => {
                repr.node.u = mpv_node__bindgen_ty_1 { int64: *i };
                repr.node.format = MPV_FORMAT_INT64;
            }
            Node::Double(d) => {
                repr.node.u = mpv_node__bindgen_ty_1 { double_: *d };
                repr.node.format = MPV_FORMAT_DOUBLE;
            }
            Node::Array(a) => {
                repr._array_repr = Some(a.to_mpv_repr()?);
                let mpv_ptr = repr._array_repr.as_ref().unwrap().ptr();

                repr.node.u = mpv_node__bindgen_ty_1 {
                    list: mpv_ptr as *mut _,
                };
                repr.node.format = MPV_FORMAT_NODE_ARRAY;
            }
            Node::Map(m) => {
                repr._map_repr = Some(m.to_mpv_repr()?);
                let mpv_ptr = repr._map_repr.as_ref().unwrap().ptr();

                repr.node.u = mpv_node__bindgen_ty_1 {
                    list: mpv_ptr as *mut _,
                };
                repr.node.format = MPV_FORMAT_NODE_MAP;
            }
            Node::ByteArray(b) => {
                repr._bytes_repr = Some(b.to_mpv_repr()?);
                let mpv_ptr = repr._bytes_repr.as_ref().unwrap().ptr();

                repr.node.u = mpv_node__bindgen_ty_1 {
                    ba: mpv_ptr as *mut _,
                };
                repr.node.format = MPV_FORMAT_BYTE_ARRAY;
            }
        };

        Ok(repr)
    }
}

impl Node {
    pub(crate) unsafe fn from_node_ptr(ptr: *const mpv_node) -> crate::types::errors::Result<Self> {
        check_null!(ptr);
        let node = unsafe { *ptr };

        match node.format {
            MPV_FORMAT_NONE => Ok(Node::None),
            MPV_FORMAT_STRING => Ok(Node::String(
                unsafe { CStr::from_ptr(node.u.string) }
                    .to_str()?
                    .to_string(),
            )),
            MPV_FORMAT_FLAG => Ok(Node::Flag(unsafe { node.u.flag } != 0)),
            MPV_FORMAT_INT64 => Ok(Node::Int64(unsafe { node.u.int64 })),
            MPV_FORMAT_DOUBLE => Ok(Node::Double(unsafe { node.u.double_ })),
            MPV_FORMAT_NODE_ARRAY => Ok(Node::Array(unsafe {
                NodeArray::from_ptr(node.u.list as *const c_void)?
            })),
            MPV_FORMAT_NODE_MAP => Ok(Node::Map(unsafe {
                NodeMap::from_ptr(node.u.list as *const c_void)?
            })),
            MPV_FORMAT_BYTE_ARRAY => Ok(Node::ByteArray(unsafe {
                ByteArray::from_ptr(node.u.ba as *const c_void)?
            })),
            _ => unimplemented!("{:?}", node.format),
        }
    }
}

impl From<&[Node]> for Node {
    fn from(value: &[Node]) -> Self {
        Node::Array(value.to_vec())
    }
}

impl From<&[(&str, Node)]> for Node {
    fn from(value: &[(&str, Node)]) -> Self {
        Node::Map(
            value
                .iter()
                .map(|(k, v)| (k.to_string(), v.clone()))
                .collect(),
        )
    }
}
