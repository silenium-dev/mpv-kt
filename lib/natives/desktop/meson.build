project('mpv-kt',
        'cpp', 'java',
        default_options : {
          'cpp_std' : 'c++20',
          'wrap_mode' : 'forcefallback',
        },
        meson_version : '>=1.2.0',
)


os = host_machine.system()
if os == 'linux'
  add_global_arguments('-fPIC', language : 'c')
  add_global_arguments('-fPIC', language : 'cpp')
endif
add_global_link_arguments(
  language : 'cpp',
)

if get_option('mpv_subproject')
  mpv = subproject('mpv',
                   default_options : {
                     'libmpv' : true,
                     'drm' : 'enabled',
                     'openal' : 'enabled',
                     'wrap_mode' : 'forcefallback',
                     'default_library' : 'both',
                     'prefer_static' : true,
                   }
  )
endif
