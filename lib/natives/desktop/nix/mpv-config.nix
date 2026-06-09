{ pkgs
}:
let
  mpv = { arch, hash }: rec {
    source_url = "https://nexus.silenium.dev/repository/github-releases/shinchiro/mpv-winbuild-cmake/releases/download/20260307/mpv-dev-${arch}-20260307-git-f9190e5.7z";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "aarch64-windows" = mpv {
    arch = "aarch64";
    hash = "sha256-+u+508ddGd8dUthvkxY0D0crqBx208QANi2CxhqxGzk=";
  };
  "x86_64-windows" = mpv {
    arch = "x86_64";
    hash = "sha256-J022MrShhJ85LiBEu+r9HnB5rNBLPrKBoDqXaaHEi/Y=";
  };
}
