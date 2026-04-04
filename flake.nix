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
      packages."x86_64-linux" = {
        default = pkgs.rustPlatform.buildRustPackage {
          name = "mpv-jni-rs";
          version = "0.1.0";
          src = (builtins.path {
            path = fs.toSource {
              root = ./.;
              fileset = fs.unions [
                ./src
                ./Cargo.toml
                ./Cargo.lock
              ];
            };
            name = "compose-av";
          });
          cargoLock = {
            lockFile = ./Cargo.lock;
          };
          useNextest = true;
          buildType = "debug";
          checkType = "debug";
          nativeBuildInputs = with pkgs; [
            jdk21
            which
            cargo-nextest
          ];
          buildInputs = with pkgs; [
            mpv
            llvmPackages.libclang
          ];
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [ pkgs.mpv ];
          LIBCLANG_PATH = "${pkgs.llvmPackages.libclang.lib}/lib";
          NEXTEST_SUCCESS_OUTPUT = "immediate";
          NEXTEST_FAILURE_OUTPUT = "immediate";
        };
      };
    };
}
