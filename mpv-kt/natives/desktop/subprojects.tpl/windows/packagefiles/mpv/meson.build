project(
  'mpv', 'c',
  meson_version : '>=1.2.0',
)

target_cpu = host_machine.cpu_family()
if target_cpu == 'x86_64'
  machine_arch = 'X64'
elif target_cpu == 'aarch64'
  machine_arch = 'ARM64'
elif target_cpu == 'x86'
  machine_arch = 'X86'
elif target_cpu == 'arm'
  machine_arch = 'ARM'
else
  error('Unsupported architecture: ' + target_cpu)
endif

mpv_dll = files('libmpv-2.dll')

custom_target(
  'mpv',
  input : mpv_dll,
  output : 'mpv.dll',
  command : ['cp', '@INPUT@', '@OUTPUT@'],
)
