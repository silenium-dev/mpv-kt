{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=fe416aaedd397cacb33a610b33d60ff2b431b127";
    jni-utils.url = "github:silenium-dev/jni-utils/0.6.0";
  };

  outputs = { nixpkgs, jni-utils, ... }:
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
          ];
        };
      };

      packages."x86_64-linux" = jni-utils.lib.buildJNILib rec {
        name = "mpv-kt";
        version = "0.1.0";
        mesonTarget = targetSystem:
          if jni-utils.lib.isLinux targetSystem then "mpv:shared_library"
          else if jni-utils.lib.isWindows targetSystem then "mpv"
          else "";
        buildType = "release";
        libName = "mpv";
        libDir = "subprojects/mpv";

        additionalNativeInputs = targetSystem: pkgs: [
          pkgs.p7zip
          pkgs.curl
          pkgs.cacert
          pkgs.python3
          pkgs.pkgsCross.mingwW64.gendef
        ];
        additionalInputs = targetSystem: pkgs:
          if jni-utils.lib.isLinux targetSystem
          then [
            pkgs.libxcb
            pkgs.libxau
            pkgs.libxdmcp
            pkgs.pipewire
            pkgs.libpulseaudio
            pkgs.dbus.lib
            pkgs.alsa-lib
          ]
          else [ ];
        sources = targetSystem:
          let
            mpv = import ./nix/mpv-config.nix { inherit pkgs; };
            ffmpeg = import ./nix/ffmpeg-config.nix { inherit pkgs; };
            sourceFiles = fs.unions [
              ./meson.build
              ./meson.options
              ./subprojects.tpl
            ];
          in
          (if (jni-utils.lib.isWindows targetSystem) then
            [
              (builtins.fetchurl {
                url = mpv.${targetSystem}.source_url;
                sha256 = mpv.${targetSystem}.source_hash;
                name = "mpv.7z";
              })
            ]
          else [
            (import ./nix/linux-sources.nix { inherit pkgs; })
            (builtins.fetchurl {
              url = ffmpeg.${targetSystem}.source_url;
              sha256 = ffmpeg.${targetSystem}.source_hash;
              name = "ffmpeg.tar.xz";
            })
          ]) ++ [
            (builtins.path {
              path = fs.toSource {
                root = ./.;
                fileset = sourceFiles;
              };
              name = "mpv-kt";
            })
          ];

        unpack = targetSystem: ''
          runHook preUnpack

          # Process each source
          for src in $srcs; do
            srcName=$(stripHash "$src")

            case "$src" in
              *.tar.gz|*.tar.xz)
                dirName="''${srcName%.tar.*}"
                echo "Extracting tar archive into: $dirName"
                mkdir -p "$dirName"
                tar xf "$src" -C "$dirName"

                # Check if archive only contained one directory
                shopt -s nullglob dotglob
                contents=("$dirName"/*)
                if [ ''${#contents[@]} -eq 1 ] && [ -d "''${contents[0]}" ]; then
                  echo "Flattening single directory structure"
                  mv "''${contents[0]}"/* "$dirName"/
                  rmdir "''${contents[0]}"
                fi

                chmod -R +w "$dirName"

                for i in "$dirName"/**/*.py "$dirName"/*.py; do
                  echo "patching shebangs in $i"
                  patchShebangs --build "$i"
                done

                tar cf "$dirName.tar" -C "$dirName" .
                rm -rf "$dirName"
                ;;
              *.zip)
                dirName="''${srcName%.zip}"
                echo "Extracting zip archive into: $dirName"
                mkdir -p "$dirName"
                7z x -o"$dirName" "$src"

                # Check if archive only contained one directory
                shopt -s nullglob dotglob
                contents=("$dirName"/*)
                if [ ''${#contents[@]} -eq 1 ] && [ -d "''${contents[0]}" ]; then
                  echo "Flattening single directory structure"
                  mv "''${contents[0]}"/* "$dirName"/
                  rmdir "''${contents[0]}"
                fi

                chmod -R +w "$dirName"

                for i in "$dirName"/**/*.py "$dirName"/*.py; do
                  echo "patching shebangs in $i"
                  patchShebangs --build "$i"
                done

                tar cf "$dirName.tar" -C "$dirName" .
                rm -rf "$dirName"
                ;;
              *.7z)
                dirName="''${srcName%.7z}"
                echo "Extracting 7z archive into: $dirName"
                mkdir -p "$dirName"
                7z x -o"$dirName" "$src"

                chmod -R +w "$dirName"
                tar cf "$dirName.tar" -C "$dirName" .
                ;;
              *mpv-kt)
                echo "Copying: $srcName"
                cp -r "$src" "$srcName"
                chmod -R +w "$srcName"
                ;;
              *)
                dirName="''${srcName}"
                echo "Compressing: $srcName"
                cp -r "$src" "$srcName"
                chmod -R +w "$srcName"

                for i in "$dirName"/**/*.py "$dirName"/*.py; do
                  echo "patching shebangs in $i"
                  patchShebangs --build "$i"
                done

                tar cf "$dirName.tar" -C "$srcName" .
                rm -rf "$srcName"
                ;;
            esac
          done

          runHook postUnpack
        '';
        postUnpackPhase = targetSystem: ''
          shopt -s globstar

          case "${targetSystem}" in
            *-windows)
              mkdir -p mpv-kt/subprojects/mpv
              tar xf mpv.tar -C mpv-kt/subprojects/mpv/
              cp mpv-kt/subprojects.tpl/windows/packagefiles/mpv/* mpv-kt/subprojects/mpv/
              ;;
            *-linux)
              for p in ./*-patch.tar; do
                echo "patching ''${p%-patch.tar}"
                tar --concatenate --file="''${p%-patch.tar}.tar" "$p"
                rm "$p"
              done

              mkdir -p mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/libass/subprojects/packagefiles
              mkdir -p mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/libass/subprojects/packagefiles/libfontconfig/subprojects/packagefiles
              mv libfontconfig.tar mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/libass/subprojects/packagefiles
              mv libxml2.tar mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/libass/subprojects/packagefiles/libfontconfig/subprojects/packagefiles
              mv gperf.tar mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/libass/subprojects/packagefiles/libfontconfig/subprojects/packagefiles

              mkdir -p mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/harfbuzz/subprojects/packagefiles
              mv icu.tar mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles/harfbuzz/subprojects/packagefiles

              mv mpv.tar mpv-kt/subprojects/packagefiles

              mv *.tar mpv-kt/subprojects/packagefiles/mpv/subprojects/packagefiles

              for i in mpv-kt/subprojects.tpl/linux/**/*; do
                if [ -d "$i" ]; then
                  echo "creating dir $i"
                  mkdir -p "mpv-kt/subprojects/''${i#mpv-kt/subprojects.tpl/linux/}"
                else
                  echo "Expanding $i:"
                  cat "$i" \
                    | sed 's,''${cav_archive_base},'"$(pwd)"',g' \
                    > "mpv-kt/subprojects/''${i#mpv-kt/subprojects.tpl/linux/}"
                fi
              done
              ;;
          esac

          sourceRoot="$(pwd)/mpv-kt"
          echo "changing source root: $sourceRoot"
          cd "$sourceRoot"
        '';

        postConfigurePhase = targetSystem: ''
          meson introspect --targets ./build-${targetSystem}
        '';

        postInstallPhase = targetSystem: ''
          case "${targetSystem}" in
            *-linux)
              cp -d subprojects/ffmpeg/lib/{libavcodec,libavdevice,libavfilter,libavformat,libavutil,libswresample,libswscale}.so* $out
              ;;
            *-windows)
              ;;
          esac
          for lib in "$out"/*.so; do
            patchelf --set-rpath '$ORIGIN' "$lib"
          done
        '';
      };
    };
}
