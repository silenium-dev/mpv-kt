{ pkgs
}:
let
  mpv = { arch, hash }: rec {
    source_url = "https://nexus.silenium.dev/repository/github-releases/shinchiro/mpv-winbuild-cmake/releases/download/20260926/mpv-dev-${arch}-20260926-git-35af06172b.7z";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "aarch64-windows" = mpv {
    arch = "aarch64";
    hash = "sha256:1f33b0n191r3ck8psv9i5mwrwxksy3aprhfmxzr60d8gwax7s38f";
  };
  "x86_64-windows" = mpv {
    arch = "x86_64";
    hash = "sha256:0mzw5dmv8jqyrwdajiscb75k53224nb3fcr90cm0xrlzgi90f8dk";
  };
}
