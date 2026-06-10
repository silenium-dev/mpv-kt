# mpv-kt

A KMP wrapper for mpv

## Supported platforms

- Android
- JVM

## Modules

- mpv-kt (wrapper around libmpv)
- compose (Composable VideoSurface using libmpv VO to render video into Compose-Multiplatform)
- ffm (KMP wrapper for Panama API)

## Usage

Add repository to `build.gradle.kts`:

```kotlin
repositories {
    maven("https://nexus.silenium.dev/repository/maven-releases")
}
```

Add dependencies:

- mpv-kt: `dev.silenium.libs.mpv:mpv-kt:0.1.0`
- compose: `dev.silenium.libs.mpv:compose:0.1.0`

## Examples

For usage examples see:

- mpv-kt: [mpv-kt/examples](./mpv-kt/examples)
- compose: [compose/examples](./compose/examples)
