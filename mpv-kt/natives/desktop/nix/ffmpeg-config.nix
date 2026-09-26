{ pkgs
}:
let
  ffmpeg = { arch, hash, ext }: rec {
    source_url = "https://nexus.silenium.dev/repository/github-releases/BtbN/FFmpeg-Builds/releases/download/autobuild-2026-09-26-13-03/ffmpeg-n9.0.2-10-g51c4a23d74-${arch}-gpl-shared-9.0.${ext}";
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
    hash = "sha256:13kkpmfd6d1zd14wz76jr7vlnbnvk19fy2ffcyyrpjaa28id6r8y";
  };
  "aarch64-linux" = ffmpeg {
    arch = "linuxarm64";
    ext = "tar.xz";
    hash = "sha256:0lsin1wsbca8ak43af5d2n8z4d4nrq8sgq16q0qriynm0wnw7gvh";
  };
}
