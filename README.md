# Retracing.jeb

## The ultimate deobfuscation plugin for jeb

## How to install

### Build From Source

1. link `jeb.jar` from `$JEB_HOME/bin/app` to `libs` directory in project
2. run `./gradlew fatJar`
3. copy build jar `RayTracing-${version}.jar` to `$JEB_HOME/coreplugins`
4. restart Jeb

### Install Precompiled

1. download jar from [outputs](./outputs/Raytracing-0.3.14.jar) and place into `$JEB_HOME/coreplugins`
2. restart Jeb

## how to use

1. load target apk
2. run `File` -> `Plugins` -> `Execute an Engine Plugin` -> `Retracing`, fill the prompting dialog with your designated
   mapping file and click ok
3. have fun

### Caveats

all functions are tested against Jeb 3.19.1 with a Pro license,
as https://www.pnfsoftware.com/jeb/manual/dev/introducing-jeb-extensions/ per Non-Pro License type can not use plugins


