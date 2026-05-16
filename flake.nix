{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    rust-overlay = {
      url = "github:oxalica/rust-overlay";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs =
    { flake-parts, nixpkgs, rust-overlay, ... } @ inputs: flake-parts.lib.mkFlake { inherit inputs; } (
      let
        fs = nixpkgs.lib.fileset;
      in
      {
        perSystem = { config, self', inputs', pkgs, system, ... }: rec {
          _module.args.pkgs = import inputs.nixpkgs {
            inherit system;
            overlays = [
              (import rust-overlay)
            ];
          };
          packages =
            let
              rustPlatform = pkgs.makeRustPlatform {
                cargo = pkgs.rust-bin.nightly.latest.minimal;
                rustc = pkgs.rust-bin.nightly.latest.minimal;
              };
            in
            rec {
              mpv-jni-rs = rustPlatform.buildRustPackage {
                name = "mpv-jni-rs";
                version = "0.1.0";
                src = (builtins.path {
                  path = fs.toSource {
                    root = ./natives;
                    fileset = fs.unions [
                      ./natives/src
                      ./natives/Cargo.toml
                      ./natives/Cargo.lock
                    ];
                  };
                });
                cargoLock = {
                  lockFile = ./natives/Cargo.lock;
                };
                useNextest = true;
                buildType = "debug";
                checkType = "debug";
                nativeBuildInputs = with pkgs; [
                  which
                  cargo-nextest
                ];
                buildInputs = with pkgs; [
                  jdk21
                  mpv
                  llvmPackages.libclang
                ];
                LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [ pkgs.mpv ];
                LIBCLANG_PATH = "${pkgs.llvmPackages.libclang.lib}/lib";
                NEXTEST_SUCCESS_OUTPUT = "immediate";
                NEXTEST_FAILURE_OUTPUT = "immediate";
              };
              default = mpv-jni-rs;
            };
          devShells = {
            default = pkgs.mkShell {
              nativeBuildInputs = with pkgs; [
                gradle_9
                jdk25
                pkg-config
              ];
              buildInputs = with pkgs; [
                mpv
              ];
            };
          };
        };
        flake = { };
        systems = [ "x86_64-linux" "aarch64-linux" "x86_64-windows" "aarch64-windows" ];
      }
    );
}
