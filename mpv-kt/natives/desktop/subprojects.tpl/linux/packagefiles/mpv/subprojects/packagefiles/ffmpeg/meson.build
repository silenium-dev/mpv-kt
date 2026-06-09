project('ffmpeg', 'c')

cc = meson.get_compiler('c')

versions_command = run_command('bash', 'collect_versions.sh', capture : true, check : true)
versions = versions_command.stdout().strip().split('\n')

library_names = ['avcodec', 'avfilter', 'avformat', 'avutil', 'swresample', 'swscale']

foreach lib_name : library_names
    foreach version : versions
        version_name = version.split(':')[0].strip()
        version_value = version.split(':')[1].strip()

        if version_name == lib_name
            message(version)
            dep = declare_dependency(
                version : version_value,
                dependencies : cc.find_library(lib_name, dirs : meson.current_source_dir() + '/lib'),
                include_directories : include_directories('include'),
            )
            meson.override_dependency('lib' + lib_name, dep)
            break
        endif
    endforeach
endforeach
