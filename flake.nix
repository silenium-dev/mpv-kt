{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs =
    { flake-parts, nixpkgs, ... } @ inputs: flake-parts.lib.mkFlake { inherit inputs; } (
      {
        perSystem = { config, self', inputs', pkgs, system, ... }: rec {
          packages = {
            jdk25-wrapped = pkgs.symlinkJoin {
              name = "jdk25-wrapped";
              paths = [ pkgs.jdk25 ];
              nativeBuildInputs = with pkgs; [
                makeWrapper
              ];
              buildInputs = with pkgs; [
                libglvnd
                mesa
                libva
                libpulseaudio
#                pipewire
              ];

              postBuild = let
              libPath =  "${pkgs.libglvnd}/lib:${pkgs.mesa}/lib:${pkgs.libva.out}/lib";
              in ''
                for bin in $out/bin/*; do
                  wrapProgram "$bin" --set LD_LIBRARY_PATH "${libPath}"
                done
              '';
            };
          };
          devShells = rec {
            mpv-java = pkgs.stdenvNoCC.mkDerivation {
              name = "mpv-java";
              version = "0.1.0";

              nativeBuildInputs = with pkgs; [
                packages.jdk25-wrapped
                gradle_9
              ];
              buildInputs = with pkgs; [
                libglvnd
              ];
              shellHook = ''
                export JAVA_HOME="${packages.jdk25-wrapped}"
              '';
            };
            default = mpv-java;
          };
        };
        flake = { };
        systems = [ "x86_64-linux" "aarch64-linux" ];
      }
    );
}
