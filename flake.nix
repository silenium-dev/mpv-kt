{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=fe416aaedd397cacb33a610b33d60ff2b431b127";
  };

  outputs = { nixpkgs, ... }:
    let
      pkgs = nixpkgs.legacyPackages."x86_64-linux";
      fs = nixpkgs.lib.fileset;
    in
    {
      devShells."x86_64-linux" = {
        default = pkgs.mkShell {
          hardeningDisable = [ "fortify" ];
          nativeBuildInputs = with pkgs; [
            p7zip
            curl
            cacert
            python3
            meson
            cmake
            ninja
            pkg-config
            rustPackages.rustc
            rustPackages.rustPlatform.rustLibSrc
            rustPackages.cargo
            rustPackages.rustfmt
            rustPackages.clippy
          ];
          buildInputs = with pkgs; [
            gcc
            gdb
            libxcb
            libX11
            libGL
            eglexternalplatform
            egl-wayland
            mpv
            llvmPackages.libclang
          ];

          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [ pkgs.mpv ];
          LIBCLANG_PATH = "${pkgs.llvmPackages.libclang.lib}/lib";
        };
      };
    };
}
