#!/bin/sh
set -e
cd "$(dirname "$0")"
rm -rf classes atd-input-driver.jar; mkdir classes
L="$APPDATA/PrismLauncher/libraries"; CP="$L/com/mojang/minecraft/26.2/minecraft-26.2-client.jar;$L/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar"
for j in deps/*.jar; do CP="$CP;$j"; done
for j in $(find "$L/com/mojang" "$L/org/lwjgl" "$L/org/slf4j" "$L/com/google" "$L/it" -name "*.jar" | grep -v "minecraft/"); do CP="$CP;$j"; done
JB="/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot/bin"
"$JB/javac" --release 25 -proc:none -cp "$CP" -d classes src/atd/driver/InputDriver.java
cp src/fabric.mod.json classes/
(cd classes && "$JB/jar" --create --file ../atd-input-driver.jar .)
sha256sum atd-input-driver.jar
