#!/usr/bin/env bash

function assemble_deps() {
  local wrap_file="$1"
  depName=$(basename "$wrap_file" .wrap)
  dir=$(dirname "$wrap_file")
  wrap_type=$(grep -oP '(?<=\[wrap-)[^\]]+(?=\])' "$wrap_file")
  case "$wrap_type" in
    file)
      file_url=$(grep -oP '(?<=source_url = )[^\n]+' "$wrap_file")
      file_name=$(grep -oP '(?<=source_filename = )[^\n]+' "$wrap_file")
      file_hash=sha256:$(grep -oP '(?<=source_hash = )[^\n]+' "$wrap_file")
      if ! file_hash_nix=$(nix hash convert "$file_hash" 2>/dev/null); then
        return
      fi
      file_ext="${file_name##*.}"
      cat <<EOF
      (pkgs.fetchurl {
        name = "${depName}${file_ext}";
        url = "$file_url";
        hash = "$file_hash_nix";
      })
EOF
      ;;
    git)
      git_url=$(grep -oP '(?<=url = )[^\n]+' "$wrap_file")
      git_revision=$(grep -oP '(?<=revision = )[^\n]+' "$wrap_file")
      git_depth=$(grep -oP '(?<=depth = )[^\n]+' "$wrap_file")
      deepClone="false"
      if [ -z "$git_depth" ] && [ "$git_depth" != "1" ]; then
        deepClone="true"
      fi
      cat <<EOF
      (pkgs.fetchgit {
        name = "$depName";
        url = "$git_url";
        rev = "$git_revision";
        hash = "";
        deepClone = $deepClone;
      })
EOF
      ;;
    *)
      echo "Unknown wrap type: $wrap_type"
      exit 1
      ;;
  esac
  patch_url=$(grep -oP '(?<=patch_url = )[^\n]+' "$wrap_file")
  patch_hash=$(grep -oP '(?<=patch_hash = )[^\n]+' "$wrap_file")
  if [ -n "$patch_url" ]; then
    patch_hash_nix=$(nix hash convert "sha256:$patch_hash" 2>/dev/null)
    if [ -z "$patch_hash_nix" ]; then
      echo "Failed to convert patch hash: $patch_hash"
      return
    fi
    cat <<EOF
      (pkgs.fetchurl {
        name = "$depName-patch.zip";
        url = "$patch_url";
        hash = "$patch_hash_nix";
      })
EOF
  fi

  for sub in "$dir/packagefiles/$depName/subprojects"/*.wrap; do
    if [ -f "$sub" ]; then
      assemble_deps "$sub"
    fi
  done
}

for subproject in subprojects/*.wrap; do
  assemble_deps "$subproject"
done
