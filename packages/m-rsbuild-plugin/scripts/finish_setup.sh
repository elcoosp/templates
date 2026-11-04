#!/usr/bin/env bash
set -euo pipefail

# Flatten the rendered template directory into the output project root
shopt -s dotglob
mv template/* .
rmdir template
