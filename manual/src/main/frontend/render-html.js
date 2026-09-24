/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

'use strict';

const { convert, convertFile, SafeMode } = require('@asciidoctor/core');
const fs = require('fs');
const path = require('path');

const FRONTEND = __dirname;
const ASCIIIDOC = path.join(FRONTEND, '..', 'asciidoc');
const TARGET = path.join(FRONTEND, '..', '..', '..', 'target');
const OUT_DIR = path.join(TARGET, 'generated-docs');

const IMAGES_SRC = path.join(ASCIIIDOC, 'images');

function copyDir(from, to) {
  if (!fs.existsSync(from)) {
    return;
  }
  fs.mkdirSync(to, { recursive: true });
  for (const entry of fs.readdirSync(from)) {
    const src = path.join(from, entry);
    const dst = path.join(to, entry);
    if (fs.statSync(src).isDirectory()) {
      copyDir(src, dst);
    } else {
      fs.copyFileSync(src, dst);
    }
  }
}

function collectAdoc(dir, out) {
  for (const entry of fs.readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (fs.statSync(full).isDirectory()) {
      collectAdoc(full, out);
    } else if (entry.endsWith('.adoc')) {
      out.push(full);
    }
  }
  return out;
}

// Asciidoctor.js does not expand include:: macros from disk reliably under
// convertFile, so resolve them ourselves (recursively, relative to each file).
function expandIncludes(file, seen) {
  const resolved = path.resolve(file);
  if (seen.has(resolved)) {
    return '';
  }
  seen.add(resolved);
  const dir = path.dirname(resolved);
  let content = fs.readFileSync(resolved, 'utf8');
  content = content.replace(/^include::([^\[]+)\[\]\s*$/gm, function (match, target) {
    const includeFile = path.join(dir, target.trim());
    if (fs.existsSync(includeFile)) {
      return expandIncludes(includeFile, seen);
    }
    return match;
  });
  return content;
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  copyDir(IMAGES_SRC, path.join(OUT_DIR, 'images'));
  const stylesheet = path.join(ASCIIIDOC, 'karaf-manual.css');
  if (fs.existsSync(stylesheet)) {
    fs.copyFileSync(stylesheet, path.join(OUT_DIR, 'karaf-manual.css'));
  }

  const attributes = {
    'source-highlighter': 'highlightjs',
    toc: '',
    'toclevels': 3,
    'toc-position': 'left',
    doctype: 'article',
    linkcss: '',
    stylesheet: 'karaf-manual.css',
    copycss: '',
    imagesdir: 'images'
  };

  const adocs = collectAdoc(ASCIIIDOC, []);
  for (const adoc of adocs) {
    const content = fs.readFileSync(adoc, 'utf8');
    let html;
    if (/^include::/m.test(content)) {
      const expanded = expandIncludes(adoc, new Set());
      html = await convert(expanded, {
        safe: SafeMode.UNSAFE,
        standalone: true,
        base_dir: ASCIIIDOC,
        attributes
      });
    } else {
      html = await convertFile(adoc, {
        safe: SafeMode.UNSAFE,
        to_file: false,
        standalone: true,
        attributes
      });
    }
    const outFile = path.join(OUT_DIR, path.basename(adoc, '.adoc') + '.html');
    fs.writeFileSync(outFile, html);
    console.log('rendered ' + path.basename(outFile));
  }
  console.log('rendered ' + adocs.length + ' adoc files');
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});