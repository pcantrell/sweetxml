#!/bin/bash

# Using a shell script for now, because it's not clear that Maven's release plugin
# exactly fits the bill....

cd "$(dirname "$0")"/..

for f in */pom.sxml; do
    echo "$(echo "$f:                        " | cut -c 1-24)" $(grep '^    version:' "$f")
done
