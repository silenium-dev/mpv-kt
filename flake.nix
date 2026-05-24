{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs =
    { flake-parts, nixpkgs, ... } @ inputs: flake-parts.lib.mkFlake { inherit inputs; } (
      {
        perSystem = { config, self', inputs', pkgs, system, ... }: {
          devShells = rec {
            mpv-java = pkgs.stdenvNoCC.mkDerivation {
              name = "mpv-java";
              version = "0.1.0";

              nativeBuildInputs = with pkgs; [
                jdk25
                gradle_9
              ];
              buildInputs = with pkgs; [
                mpv
              ];
            };
            default = mpv-java;
          };
        };
        flake = { };
        systems = [ "x86_64-linux" "aarch64-linux" "x86_64-windows" "aarch64-windows" ];
      }
    );
}
