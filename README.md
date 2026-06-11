# mpv-kt

A KMP wrapper for mpv

## Supported platforms

- Android: x86_64, arm64 (SDK >= 26)
- JVM: Linux, Windows (JVM >= 25, uses [Panama API](https://openjdk.org/projects/panama/))

*Note: macOS and iOS are unsupported, as I have no Apple devices to develop/test on*

## Modules

- mpv-kt (wrapper around libmpv)
- compose (Composable VideoSurface using libmpv VO to render video into Compose-Multiplatform)
- ffm (KMP wrapper for Panama API)

## Usage

Add repository to `build.gradle.kts`:

```kotlin
repositories {
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}
```

Add dependencies (for versions, see GitHub Releases
or [https://nexus.silenium.dev/repository/maven-releases/](https://nexus.silenium.dev/repository/maven-releases/):

- mpv-kt: `dev.silenium.libs.mpv:mpv-kt:0.1.0`
- compose: `dev.silenium.libs.mpv:compose:0.1.0`

Snapshots are available
here: [https://nexus.silenium.dev/repository/maven-snapshots/](https://nexus.silenium.dev/repository/maven-snapshots/)

## Examples

For usage examples see:

- mpv-kt: [mpv-kt/examples](./mpv-kt/examples)
- compose: [compose/examples](./compose/examples)
