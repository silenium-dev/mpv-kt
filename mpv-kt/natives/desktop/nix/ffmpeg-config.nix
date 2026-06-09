{ pkgs
}:
let
  ffmpeg = { arch, hash, ext }: rec {
    source_url = "https://nexus.silenium.dev/repository/github-releases/BtbN/FFmpeg-Builds/releases/download/autobuild-2026-03-13-13-08/ffmpeg-n8.0.1-76-gfa4ee7ab3c-${arch}-gpl-shared-8.0.${ext}";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    file_ext = ext;
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "x86_64-linux" = ffmpeg {
    arch = "linux64";
    ext = "tar.xz";
    hash = "sha256-mlwpVjKX/CCu3zXHR/x7z0HpB0w7B98U/GHXpBnu5PA=";
  };
  "aarch64-linux" = ffmpeg {
    arch = "linuxarm64";
    ext = "tar.xz";
    hash = "sha256-HPwZklj/1yxeDyvOKuO3I9W9kN47+RJa+eBMSulVM9E=";
  };
}
