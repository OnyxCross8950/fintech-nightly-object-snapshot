#!/bin/sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/fintech-snapshot-example"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java -name '*.java' -print)
cp src/main/resources/application.properties "$BUILD_DIR/application.properties"
java -cp "$BUILD_DIR" dev.infrai.fintech.FintechSnapshotApplication
