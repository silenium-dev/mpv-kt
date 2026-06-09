#!/usr/bin/env bash

for shared_object in "${MESON_SOURCE_ROOT}/${MESON_SUBDIR}"/lib/*.so.*.*.*; do
  filename=$(basename "$shared_object")
  lib_name=$(basename "${filename//\.so.*/}")
  lib_name="${lib_name//lib/}"
  lib_version=${filename//*.so\./}
  echo "${lib_name}:${lib_version}"
done
