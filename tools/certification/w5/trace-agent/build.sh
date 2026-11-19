#!/bin/sh
# Builds trace-agent.jar (test-only server agent for campaign trace: oracles). No dependencies.
# Also carries TraceBridge (default package) for packet-witness.jar/w4-snap.jar; the manifest puts
# the jar itself on the bootstrap path so the server class loaders can see it.
set -e
cd "$(dirname "$0")"
JB="/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot/bin"
rm -rf classes trace-agent.jar; mkdir classes
"$JB/javac" --release 25 -d classes src/atd/trace/TraceAgent.java src/TraceBridge.java
printf 'Premain-Class: atd.trace.TraceAgent\nBoot-Class-Path: trace-agent.jar\n' > classes/MANIFEST.MF
"$JB/jar" --create --file trace-agent.jar --manifest classes/MANIFEST.MF -C classes atd -C classes TraceBridge.class
sha256sum trace-agent.jar
