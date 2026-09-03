#!/bin/sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/fintech-snapshot-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" src/main/java/dev/infrai/fintech/SnapshotPolicy.java src/test/java/dev/infrai/fintech/SnapshotPolicyTest.java
java -cp "$BUILD_DIR" dev.infrai.fintech.SnapshotPolicyTest

