jni_headers_dep = jni_headers.get_variable('jni_headers_dep')
deps = [jni_headers_dep]
link_args = []

if get_option('mpv_subproject')
  mpv_dep = mpv.get_variable('libmpv_dep')
  deps += [mpv_dep]
else
  mpv = dependency('mpv')
  deps += [mpv]
endif

sources = [
  'helper/JniCallRef.hpp',
  'helper/JniMpvCallback.cpp',
  'helper/JniMpvCallback.hpp',
  'helper/MpvCallback.hpp',
  'helper/results.cpp',
  'helper/results.hpp',
  'jni/commands.cpp',
  'jni/lifecycle.cpp',
  'jni/observe.cpp',
  'jni/properties_async.cpp',
  'jni/properties_sync.cpp',
  'jni/render.cpp',
  'mpv/commands.cpp',
  'mpv/events.cpp',
  'mpv/instance.cpp',
  'mpv/instance.hpp',
  'mpv/nodes.hpp',
  'mpv/nodes.cpp',
  'mpv/observe.cpp',
  'mpv/platform.hpp',
  'mpv/properties_async.cpp',
  'mpv/properties_sync.cpp',
  'mpv/renderer.hpp',
  'mpv/renderer.cpp',
  'util/Errors.cpp',
  'util/MPVException.cpp',
  'util/MPVException.hpp',
]

os = host_machine.system()
if os == 'linux'
  egl = dependency('egl')
  glx = dependency('glx')
  x11 = dependency('x11')
  deps += [egl, glx, x11]
  sources += ['mpv/platform_linux.cpp']
  add_project_arguments('-DTARGET_LINUX', language : 'cpp')
elif os == 'windows'
  sources += ['mpv/platform_windows.cpp']
  cc = meson.get_compiler('cpp')
  link_args += ['opengl32.lib']
  add_project_arguments('-DTARGET_WINDOWS', language : 'cpp')
endif

shared_library(
  'mpv-kt',
  sources,
  dependencies : deps,
  link_args : link_args,
  cpp_args : ['-Wimplicit-fallthrough'],
)
