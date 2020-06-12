#!/bin/bash

set -euxo pipefail

echo "127.0.0.1 $(hostname)" > /etc/hosts # without this spark-tools tests will fail in docker

if [[ "$1" -eq 0 ]] ; then
    ./BuildDevint -B
else
    ./BuildDevint -B -C $1
fi
