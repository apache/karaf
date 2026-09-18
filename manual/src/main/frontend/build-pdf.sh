#!/usr/bin/env sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Build the Apache Karaf manual as a PDF using the asciidoctor-pdf (Ruby) gem.
# The script installs the required Ruby toolchain if it is not already present
# (tested on Ubuntu/Debian and macOS), then renders src/main/asciidoc/index.adoc
# into target/documentation.pdf using a theme aligned with the Apache Karaf
# website colors (see karaf-pdf-theme.yml).
#
# Usage: sh build-pdf.sh

set -e

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAIN_DIR=$(dirname -- "$SCRIPT_DIR")
ASCIIIDOC_DIR="$MAIN_DIR/asciidoc"
PROJECT_DIR=$(cd "$MAIN_DIR/../.." && pwd)
TARGET_DIR="$PROJECT_DIR/target"

install_ruby() {
    case "$(uname -s)" in
        Darwin)
            if command -v brew >/dev/null 2>&1; then
                printf 'Installing Ruby via Homebrew...\n'
                brew install ruby
            else
                printf 'Homebrew was not found. Please install Ruby (https://www.ruby-lang.org), then re-run this script.\n' >&2
                exit 1
            fi
            ;;
        Linux)
            OS_ID=linux
            if [ -r /etc/os-release ]; then
                OS_ID=$(. /etc/os-release; printf '%s' "$ID")
            fi
            case "$OS_ID" in
                ubuntu|debian)
                    printf 'Installing Ruby and build tools via apt...\n'
                    sudo apt-get update
                    sudo apt-get install -y ruby ruby-dev build-essential
                    ;;
                *)
                    printf 'Unsupported Linux distribution "%s". Please install Ruby and RubyGems, then re-run this script.\n' "$OS_ID" >&2
                    exit 1
                    ;;
            esac
            ;;
        *)
            printf 'Unsupported operating system. Please install Ruby and RubyGems, then re-run this script.\n' >&2
            exit 1
            ;;
    esac
}

ensure_asciidoctor_pdf() {
    if ! command -v ruby >/dev/null 2>&1 || ! command -v gem >/dev/null 2>&1; then
        install_ruby
    fi
    if ! command -v asciidoctor-pdf >/dev/null 2>&1; then
        printf 'Installing asciidoctor, asciidoctor-pdf and rouge gems...\n'
        gem install --no-document asciidoctor asciidoctor-pdf rouge
    fi
    # The gem bin directory may not be on PATH (e.g. user-local gems). Resolve it.
    if ! command -v asciidoctor-pdf >/dev/null 2>&1; then
        RUBY_GEM_HOME=$(gem env home 2>/dev/null || true)
        if [ -n "$RUBY_GEM_HOME" ] && [ -x "$RUBY_GEM_HOME/bin/asciidoctor-pdf" ]; then
            export PATH="$RUBY_GEM_HOME/bin:$PATH"
        fi
    fi
    if ! command -v asciidoctor-pdf >/dev/null 2>&1; then
        printf 'asciidoctor-pdf is not available. Re-run the script, or check the RubyGems installation.\n' >&2
        exit 1
    fi
    printf 'asciidoctor-pdf available: %s\n' "$(command -v asciidoctor-pdf)"
}

ensure_asciidoctor_pdf

mkdir -p "$TARGET_DIR"
cd "$ASCIIIDOC_DIR"
asciidoctor-pdf \
    -a pdf-theme="$SCRIPT_DIR/karaf-pdf-theme.yml" \
    -a source-highlighter=rouge \
    -a imagesdir=images \
    -a doctype=book \
    -o "$TARGET_DIR/documentation.pdf" \
    index.adoc

printf 'pdf generated: %s\n' "$TARGET_DIR/documentation.pdf"