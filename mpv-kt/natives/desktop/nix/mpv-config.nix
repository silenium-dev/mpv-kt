{ pkgs
}:
let
  mpv = { arch, hash }: rec {
    source_url = "https://nexus.silenium.dev/repository/github-releases/shinchiro/mpv-winbuild-cmake/releases/download/20261111/mpv-dev-${arch}-20261111-git-bcde6b93cc.7z";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "aarch64-windows" = mpv {
    arch = "aarch64";
    hash = "sha256:a42c1cebf7366a957c7c321a20df414b80d4a55bbc65ded032d78b749c8d6445";
  };
  "x86_64-windows" = mpv {
    arch = "x86_64";
    hash = "sha256:273c079b11ea5575822f972756f4c9a71149a5c10130353abeb8586738938452";
  };
}
