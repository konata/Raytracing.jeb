# Retracing.jeb

## The ultimate deobfuscation plugin for jeb

### How to use

1. link `jeb.jar` from `$JEB_HOME/bin/app` to `libs` directory in project
2. run `./gradlew fatJar`
3. copy build jar `RayTracing-${version}.jar`  to `$JEB_HOME/coreplugins`
4. Restart Jeb & load target apk
5. run `File` -> `Plugins` -> `Execute an Engine Plugin` -> `Retracing`, fill designated mapping file and click ok
6. have fun

### Caveats

all functions are tested against Jeb 3.19.1 with a Pro license,
as https://www.pnfsoftware.com/jeb/manual/dev/introducing-jeb-extensions/ per Non-Pro License type can not use plugins


