pub(crate) mod basics;
pub(crate) mod byte_array;
pub(crate) mod errors;
pub(crate) mod node;
pub(crate) mod node_array;
pub(crate) mod node_map;
mod tests;
pub(crate) mod traits;

pub use basics::OsdString;
pub use byte_array::ByteArray;
pub use errors::*;
pub use node::Node;
pub use node_array::NodeArray;
pub use node_map::NodeMap;

pub use traits::MpvFormat;
pub use traits::MpvRecv;
pub use traits::MpvSend;

use libmpv2_sys::mpv_format;

#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Format {
    pub(crate) mpv_format: mpv_format,
}

impl Format {
    const fn from(native: mpv_format) -> Self {
        Self { mpv_format: native }
    }
}

impl Format {
    pub const NONE: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_NONE);
    pub const STRING: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_STRING);
    pub const OSD_STRING: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_OSD_STRING);
    pub const FLAG: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_FLAG);
    pub const INT64: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_INT64);
    pub const DOUBLE: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_DOUBLE);
    pub const NODE: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_NODE);
    pub const NODE_ARRAY: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_NODE_ARRAY);
    pub const NODE_MAP: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_NODE_MAP);
    pub const BYTE_ARRAY: Format = Format::from(libmpv2_sys::mpv_format_MPV_FORMAT_BYTE_ARRAY);
}
