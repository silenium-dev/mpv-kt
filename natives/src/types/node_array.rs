use crate::types::errors::Result;
use crate::types::node::{MpvNode, Node};
use crate::types::traits::{
    MpvFormat, MpvRecv, MpvRecvInternal, MpvRepr, MpvSend, MpvSendInternal, ToMpvRepr,
};
use crate::types::Format;
use libmpv2_sys::{mpv_node, mpv_node_list};
use std::ffi::{c_int, c_void};
use std::marker::PhantomData;
use std::mem::MaybeUninit;
use std::ptr::null_mut;

pub type NodeArray = Vec<Node>;

#[derive(Debug)]
pub(crate) struct MpvNodeArray<'a> {
    _original: PhantomData<&'a NodeArray>,

    _owned_reprs: Vec<MpvNode<'a>>,
    _flat_reprs: Vec<mpv_node>,

    node_list: Box<mpv_node_list>,
}

impl MpvRepr for MpvNodeArray<'_> {
    type Repr = mpv_node_list;

    fn ptr(&self) -> *const Self::Repr {
        &raw const *self.node_list
    }
}

impl MpvFormat for NodeArray {
    const MPV_FORMAT: Format = Format::NODE_ARRAY;
}

impl From<NodeArray> for Node {
    fn from(value: NodeArray) -> Self {
        Node::Array(value)
    }
}

impl From<&NodeArray> for Node {
    fn from(value: &NodeArray) -> Self {
        Node::Array(value.clone())
    }
}

impl MpvRecv for NodeArray {}
impl MpvRecvInternal for NodeArray {
    unsafe fn from_ptr(ptr: *const c_void) -> Result<Self> {
        check_null!(ptr);
        let node_list = unsafe { *(ptr as *const mpv_node_list) };

        if node_list.num <= 0 {
            return Ok(NodeArray::default())
        }

        check_null!(node_list.values);
        let mut values = Vec::with_capacity(node_list.num as usize);
        let node_values =
            unsafe { std::slice::from_raw_parts(node_list.values, node_list.num as usize) };
        for node_value in node_values {
            values.push(unsafe { Node::from_node_ptr(node_value)? });
        }
        Ok(values)
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> Result<()>>(fun: F) -> Result<Self> {
        let mut node_list: MaybeUninit<mpv_node_list> = MaybeUninit::uninit();

        fun(node_list.as_mut_ptr() as *mut c_void)
            .map(|_| unsafe { Self::from_ptr(node_list.as_ptr() as *const c_void) })?
    }
}

impl MpvSend for NodeArray {}
impl MpvSendInternal for NodeArray {
    fn to_mpv<R, F: Fn(*mut c_void) -> Result<R>>(&self, fun: F) -> Result<R> {
        let repr = self.to_mpv_repr()?;
        fun(repr.ptr() as *mut c_void)
    }
}

impl ToMpvRepr for NodeArray {
    type ReprWrap<'a> = MpvNodeArray<'a>;

    fn to_mpv_repr(&self) -> Result<Self::ReprWrap<'_>> {
        let mut repr = MpvNodeArray {
            _original: PhantomData,
            _owned_reprs: Vec::with_capacity(self.len()),
            _flat_reprs: Vec::with_capacity(self.len()),
            node_list: Box::new(mpv_node_list {
                num: self.len() as c_int,
                values: null_mut(),
                keys: null_mut(),
            }),
        };

        for node in self {
            let node_repr = node.to_mpv_repr()?;
            repr._flat_reprs.push(*node_repr.node);
            repr._owned_reprs.push(node_repr);
        }

        repr.node_list.values = repr._flat_reprs.as_mut_ptr();
        Ok(repr)
    }
}
