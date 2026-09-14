#!/usr/bin/env bash

# Sets the project version everywhere it is written down: pom.xml and the
# VERSION constant compiled into the binary. Run from the repo root.

set -euo pipefail

version="${1:?usage: set-version.sh <version>}"
java_file='src/main/java/me/hernancerm/GitTimeline.java'

./mvnw -q versions:set -DnewVersion="${version}" -DgenerateBackupPoms=false

sed -E "s/(String VERSION = \")[^\"]*(\")/\1${version}\2/" "${java_file}" > "${java_file}.tmp"
mv "${java_file}.tmp" "${java_file}"

# The sed is silent when the constant is renamed or reformatted, which would
# ship a binary reporting the previous version. Fail instead.
if ! grep -q "String VERSION = \"${version}\"" "${java_file}"
then
  echo "error: could not set VERSION in ${java_file}" >&2
  exit 1
fi
