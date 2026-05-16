use crate::types::errors::Result;
use crate::types::node::{MpvNode, Node};
use crate::types::traits::{
    MpvFormat, MpvRecv, MpvRecvInternal, MpvRepr, MpvSend, MpvSendInternal, ToMpvRepr,
};
use crate::types::Format;
use libmpv2_sys::{mpv_node, mpv_node_list};
use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_void, CStr, CString};
use std::marker::PhantomData;
use std::mem::MaybeUninit;
use std::ptr::null_mut;

pub type NodeMap = HashMap<String, Node>;

#[derive(Debug)]
pub(crate) struct MpvNodeMap<'a> {
    _original: PhantomData<&'a NodeMap>,

    _owned_reprs: Vec<MpvNode<'a>>,
    _flat_reprs: Vec<mpv_node>,

    _owned_keys: Vec<CString>,
    _flat_keys: Vec<*const c_char>,

    node_list: Box<mpv_node_list>,
}

impl MpvRepr for MpvNodeMap<'_> {
    type Repr = mpv_node_list;

    fn ptr(&self) -> *const Self::Repr {
        &raw const *self.node_list
    }
}

impl MpvFormat for NodeMap {
    const MPV_FORMAT: Format = Format::NODE_MAP;
}

impl From<NodeMap> for Node {
    fn from(value: NodeMap) -> Self {
        Node::Map(value)
    }
}

impl From<&NodeMap> for Node {
    fn from(value: &NodeMap) -> Self {
        Node::Map(value.clone())
    }
}

impl MpvRecv for NodeMap {}
impl MpvRecvInternal for NodeMap {
    unsafe fn from_ptr(ptr: *const c_void) -> Result<Self> {
        check_null!(ptr);
        let node_list = unsafe { *(ptr as *const mpv_node_list) };

        if node_list.num <= 0 {
            return Ok(NodeMap::default())
        }

        check_null!(node_list.values);
        check_null!(node_list.keys);

        let mut values = Vec::with_capacity(node_list.num as usize);
        let node_values =
            unsafe { std::slice::from_raw_parts(node_list.values, node_list.num as usize) };
        for node_value in node_values {
            values.push(unsafe { Node::from_node_ptr(node_value)? });
        }

        let mut keys = Vec::with_capacity(node_list.num as usize);
        let node_keys =
            unsafe { std::slice::from_raw_parts(node_list.keys, node_list.num as usize) };
        for node_key in node_keys {
            keys.push(unsafe { CStr::from_ptr(*node_key) }.to_str()?.to_string())
        }

        let map = HashMap::from_iter(keys.into_iter().zip(values.into_iter()));

        Ok(map)
    }

    unsafe fn from_mpv<F: Fn(*mut c_void) -> Result<()>>(fun: F) -> Result<Self> {
        let mut node_list: MaybeUninit<mpv_node_list> = MaybeUninit::uninit();

        fun(node_list.as_mut_ptr() as *mut c_void)
            .map(|_| unsafe { Self::from_ptr(node_list.as_ptr() as *const c_void) })?
    }
}

impl MpvSend for NodeMap {}
impl MpvSendInternal for NodeMap {
    fn to_mpv<R, F: Fn(*mut c_void) -> Result<R>>(&self, fun: F) -> Result<R> {
        let repr = self.to_mpv_repr()?;
        fun(repr.ptr() as *mut c_void)
    }
}

impl ToMpvRepr for NodeMap {
    type ReprWrap<'a> = MpvNodeMap<'a>;

    fn to_mpv_repr(&self) -> Result<Self::ReprWrap<'_>> {
        let mut repr = MpvNodeMap {
            _original: PhantomData,
            _owned_reprs: Vec::with_capacity(self.len()),
            _flat_reprs: Vec::with_capacity(self.len()),
            _owned_keys: Vec::with_capacity(self.len()),
            _flat_keys: Vec::with_capacity(self.len()),
            node_list: Box::new(mpv_node_list {
                num: self.len() as c_int,
                values: null_mut(),
                keys: null_mut(),
            }),
        };

        for (key, value) in self {
            let c_string = CString::new(key.as_bytes())?;
            repr._flat_keys.push(c_string.as_ptr());
            repr._owned_keys.push(c_string);

            let val_repr = value.to_mpv_repr()?;
            repr._flat_reprs.push(*val_repr.node);
            repr._owned_reprs.push(val_repr);
        }

        repr.node_list.values = repr._flat_reprs.as_mut_ptr() as *mut _;
        repr.node_list.keys = repr._flat_keys.as_mut_ptr() as *mut _;

        Ok(repr)
    }
}
