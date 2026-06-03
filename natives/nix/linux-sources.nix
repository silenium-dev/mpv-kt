{ pkgs }: [
  (pkgs.fetchurl {
    name = "expat.tar.xz";
    url = "https://github.com/libexpat/libexpat/releases/download/R_2_7_3/expat-2.7.3.tar.xz";
    hash = "sha256-cd+PQHBqe7CoClNnB56nXZHaT4xlxY7Fm837997Nq58=";
  })
  (pkgs.fetchurl {
    name = "expat-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/expat_2.7.3-1/get_patch";
    hash = "sha256-6HDrSy48FCzh0Y7SQwmqQOB8bJEaRiX6HG/Ek7oYFTo=";
  })

  (pkgs.fetchgit {
    name = "fmt";
    url = "https://github.com/fmtlib/fmt.git";
    rev = "12.0.0";
    hash = "sha256-AZDmIeU1HbadC+K0TIAGogvVnxt0oE9U6ocpawIgl6g=";
    deepClone = false;
  })

  (pkgs.fetchgit {
    name = "freetype2";
    url = "https://gitlab.freedesktop.org/freetype/freetype.git";
    rev = "VER-2-13-3";
    hash = "sha256-fAefpGvNWM9t8XPS8HOCmXLO/76pwp2TV5yHDSfjc/4=";
    deepClone = false;
  })

  (pkgs.fetchgit {
    name = "fribidi";
    url = "https://github.com/fribidi/fribidi.git";
    rev = "v1.0.16";
    hash = "sha256-VXDgyqpgjeNHnKfihWW0Oe1yzwIv1Bw8mrs8wLBJZgw=";
    deepClone = false;
  })

  (pkgs.fetchurl {
    name = "harfbuzz.tar.xz";
    url = "https://github.com/harfbuzz/harfbuzz/releases/download/12.1.0/harfbuzz-12.1.0.tar.xz";
    hash = "sha256-5cgbf24LEC37AAz6QkU4uOiWq3ii9Lil7IyuYqtDNp4=";
  })

  (pkgs.fetchgit {
    name = "libass";
    url = "https://github.com/libass/libass";
    rev = "0.17.4";
    hash = "sha256-GgeJ9W1x5IOSyIcbR8F4D5BmAqrkG6xXs85wlVXVsig=";
    deepClone = false;
  })

  (pkgs.fetchgit {
    name = "libfontconfig";
    url = "https://gitlab.freedesktop.org/fontconfig/fontconfig.git";
    rev = "2.17.1";
    hash = "sha256-RCYZctF3Nheopx8RojjlyE8Z2w7C7Nfyi2aobVD9o5Q=";
    deepClone = false;
  })

  (pkgs.fetchgit {
    name = "libdisplay-info";
    url = "https://gitlab.freedesktop.org/emersion/libdisplay-info.git";
    rev = "0.3.0";
    hash = "sha256-nXf2KGovNKvcchlHlzKBkAOeySMJXgxMpbi5z9gLrdc=";
    deepClone = false;
  })

  (pkgs.fetchurl {
    name = "libjpeg-turbo.tar.gz";
    url = "https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.1.2/libjpeg-turbo-3.1.2.tar.gz";
    hash = "sha256-jwASI0tGTOUIkMSQ8YGU+ROnsfTmoD1mRBefoPhn0M8=";
  })

  (pkgs.fetchgit {
    name = "libplacebo";
    url = "https://github.com/haasn/libplacebo";
    rev = "v7.360.0";
    hash = "sha256-KGPjP2Aia033XLopnlftkhESEXjmOG2j5mHLgpG5dCY=";
    deepClone = false;
  })

  (pkgs.fetchurl {
    name = "libpng.tar.gz";
    url = "https://github.com/pnggroup/libpng/archive/v1.6.50.tar.gz";
    hash = "sha256-cRWOU8/fKHe8mbyrM2QdeN8/SObg2q0DCv6cuMAxqkY=";
  })
  (pkgs.fetchurl {
    name = "libpng-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/libpng_1.6.50-2/get_patch";
    hash = "sha256-qck2K7jLtCLIZIB6F0l33sOGjN/4yjgOJ7q27hyqIuo=";
  })

  (pkgs.fetchgit {
    name = "mpv";
    url = "https://github.com/mpv-player/mpv.git";
    rev = "v0.41.0";
    hash = "sha256-gJWqfvPE6xOKlgj2MzZgXiyOKxksJlY/tL6T/BeG19c=";
    deepClone = false;
  })

  (pkgs.fetchgit {
    name = "libuchardet";
    url = "https://gitlab.freedesktop.org/uchardet/uchardet.git";
    rev = "v0.0.8";
    hash = "sha256-5HSwFaclje5JkzOZKILgy2BGxLyFeDq/9p24KiTlTzE=";
    deepClone = false;
  })

  (pkgs.fetchurl {
    name = "openal-soft.tar.gz";
    url = "https://github.com/kcat/openal-soft/archive/refs/tags/1.24.3.tar.gz";
    hash = "sha256-fh/s3rRef3hyK3dsXPML0zk0uWHX/SoR4ElOBkzGMc4=";
  })
  (pkgs.fetchurl {
    name = "openal-soft-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/openal-soft_1.24.3-1/get_patch";
    hash = "sha256-mmwWYizH3bCKga+qnESSXlR1GMlIXymmjRUC4OvlTRY=";
  })

  (pkgs.fetchurl {
    name = "zlib.tar.gz";
    url = "http://zlib.net/fossils/zlib-1.3.1.tar.gz";
    hash = "sha256-mpOyt9/ax3zrpaVYpYDnRmfdb+3kWFuR7vtg8Dty3yM=";
  })
  (pkgs.fetchurl {
    name = "zlib-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/zlib_1.3.1-2/get_patch";
    hash = "sha256-nKzqAuERmWS8Uekt0jWbFN9yOjbP4N8ceNVdnJ8nY64=";
  })

  (pkgs.fetchurl {
    name = "icu.tar.gz";
    url = "https://github.com/unicode-org/icu/releases/download/release-77-1/icu4c-77_1-src.tgz";
    hash = "sha256-WI5DH3cyfDkDH/u4hDwOO8EiwhE3RIX6h9xfP6/yQGE=";
  })
  (pkgs.fetchurl {
    name = "icu-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/icu_77.1-3/get_patch";
    hash = "sha256-3yOnIOzkzYx271C8r+DYAMjBEReAa/IH1llzqV8Nzqc=";
  })

  (pkgs.fetchurl {
    name = "libxml2.tar.xz";
    url = "https://download.gnome.org/sources/libxml2/2.12/libxml2-2.12.6.tar.xz";
    hash = "sha256-iJxZOogaPbX92WzJMYyH3zTrZI7fxFgnKtRv1gc1P7s=";
  })
  (pkgs.fetchurl {
    name = "libxml2-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/libxml2_2.12.6-1/get_patch";
    hash = "sha256-d3vn/9xBZ+TQs/QF4TSSZky97y5s9vaojMVeyAESYUw=";
  })

  (pkgs.fetchgit {
    name = "gperf";
    url = "https://gitlab.freedesktop.org/tpm/gperf.git";
    rev = "c24359b4eab86d71c655c3b3fc969f13aac879ce";
    hash = "sha256-IbWwGTSFRTnPxGJHaQIA0ywvZOCec6gpdrobXtZ6TO4=";
    deepClone = false;
  })

  (pkgs.fetchurl {
    name = "brotli.tar.gz";
    url = "https://github.com/google/brotli/archive/v1.1.0.tar.gz";
    hash = "sha256-5yCmyilCi4A/StFlNxdx9TmPq6OX7fZ3iDehhZnqE/8=";
  })
  (pkgs.fetchurl {
    name = "brotli-patch.zip";
    url = "https://wrapdb.mesonbuild.com/v2/google-brotli_1.1.0-3/get_patch";
    hash = "sha256-d+daO7AZpLZTGVoVrxfMZcVvuMztJRkVT85Oa7Q8dOI=";
  })
]
