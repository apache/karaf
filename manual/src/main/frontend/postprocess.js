/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";

const fs = require("fs");
const path = require("path");

const baseDir = path.join(__dirname, "..", "..", "..", "target", "generated-docs");
const headSnippet = fs.readFileSync(path.join(__dirname, "karaf-manual-head.html"), "utf8").trim();
const footerSnippet = fs.readFileSync(path.join(__dirname, "karaf-manual-footer.html"), "utf8").trim();

function walk(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (fs.statSync(full).isDirectory()) {
      out.push(...walk(full));
    } else if (entry.endsWith(".html") && !entry.startsWith("karaf-manual-")) {
      out.push(full);
    }
  }
  return out;
}

function stripTags(s) {
  return (s || "").replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
    .replace(/&#8217;|&#39;|&apos;/g, "'")
    .replace(/&#8211;|&#8212;/g, "-")
    .replace(/\s+/g, " ").trim();
}

function escapeHtml(s) {
  return (s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function processFile(file) {
  let html = fs.readFileSync(file, "utf8");
  const fileName = path.basename(file);

  // 1. head injection (fonts + pre-paint theme script)
  if (!html.includes("fonts.googleapis.com/css2") && html.includes("</head>")) {
    html = html.replace("</head>", headSnippet + "\n</head>");
  }

  // 2. capture doc title from Asciidoctor header
  let docTitle = fileName.replace(/\.html$/, "");
  const th = /<h1[^>]*>([\s\S]*?)<\/h1>/.exec(html);
  if (th) { docTitle = stripTags(th[1]); }
  // drop the Asciidoctor header block (brand replaces it)
  html = html.replace(/[ \t]*<div id="header">[\s\S]*?<\/div>\r?\n?/, "");

  // 3. extract #toc block (balanced div, includes nested <ul>/<li>/<div>)
  let tocHtml = "";
  const tocOpen = /<div id="toc"[^>]*>/i.exec(html);
  if (tocOpen) {
    const depthStart = tocOpen.index + tocOpen[0].length;
    let depth = 1;
    const re = /<(\/)?div\b[^>]*>/gi;
    let m;
    let idx = depthStart;
    while ((m = re.exec(html))) {
      if (m.index < depthStart) { continue; }
      if (m[1]) { depth--; } else { depth++; }
      if (depth === 0) {
        idx = m.index + m[0].length;
        break;
      }
    }
    tocHtml = html.slice(tocOpen.index, idx);
  }

  // 4. extract #content block + following pager spot; content ends right before <div id="footer">
  const contentOpenRe = /<div id="content"[^>]*>/;
  const co = contentOpenRe.exec(html);
  let contentHtml = "";
  let beforeContent = "";
  let afterContent = "";
  if (co) {
    const openEnd = co.index + co[0].length;
    beforeContent = html.slice(0, co.index);
    const footerMatch = /<\/div>\s*<div id="footer">[\s\S]*?<\/div>/;
    const fm = footerMatch.exec(html);
    if (fm && fm.index >= openEnd) {
      contentHtml = html.slice(openEnd, fm.index);
      afterContent = html.slice(openEnd + contentHtml.length);
    } else {
      // no footer fallback
      const closeBody = html.indexOf("</body>");
      contentHtml = html.slice(openEnd, closeBody >= 0 ? closeBody : html.length);
      afterContent = html.slice(closeBody >= 0 ? closeBody : html.length);
    }
  }

  // 5. collect sections at every heading level (h2/h3/h4) from content for search index + pager
  const sections = [];
  const headRe = /<(h[234])\s+id="([^"]+)">([\s\S]*?)<\/\1>/g;
  const heads = [];
  let hm;
  while ((hm = headRe.exec(contentHtml)) !== null) {
    heads.push({ level: parseInt(hm[1].charAt(1), 10), id: hm[2], title: stripTags(hm[3]), index: hm.index, end: hm.index + hm[0].length });
  }
  let part = "";
  for (let i = 0; i < heads.length; i++) {
    const h = heads[i];
    if (h.level === 2) part = h.title;
    const bodyStart = h.end;
    const bodyEnd = i + 1 < heads.length ? heads[i + 1].index : contentHtml.length;
    const text = stripTags(contentHtml.slice(bodyStart, bodyEnd));
    if (h.id) {
      sections.push({ id: h.id, title: h.title, level: h.level, text: text, part: part });
    }
  }

  // 6. build static shell
  const brandHtml =
    '<a class="k-manual-brand" href="/" aria-label="Apache Karaf">' +
    '<img class="k-logo k-logo-dark" src="images/karaf-logo.svg" alt="">' +
    '<img class="k-logo k-logo-light" src="images/karaf-logo-light.svg" alt="">' +
    '<span class="k-manual-brand-name">Apache Karaf</span></a>';

  const searchHtml =
    '<div class="k-manual-search">' +
    '<input type="search" id="k-manual-search" class="k-manual-search-input" placeholder="Search documentation..." autocomplete="off">' +
    '<ul id="k-manual-search-results" class="k-manual-search-results" aria-live="polite"></ul>' +
    "</div>";

  const filterHtml =
    '<label class="visually-hidden" for="k-manual-toc-filter">Filter sections</label>' +
    '<input type="search" id="k-manual-toc-filter" class="k-manual-toc-filter" placeholder="Filter sections..." autocomplete="off">';

  const hasToc = tocHtml.indexOf('id="toc"') !== -1;
  const tocCol = '<div class="toc-col">' + (hasToc ? filterHtml : "") + tocHtml + "</div>";

  // right "On this page" rail: the page's own h2/h3 outline (skip when huge, e.g. consolidated index)
  let rightCol = "";
  const pageOutline = sections.filter(function (s) { return s.level <= 3; });
  if (pageOutline.length > 0 && pageOutline.length < 40) {
    const links = pageOutline.map(function (s) {
      const cls = s.level === 2 ? "k-toc-right-h2" : "k-toc-right-h3";
      return '<a class="k-toc-right-link ' + cls + '" href="#' + escapeHtml(s.id) + '">' + escapeHtml(s.title) + "</a>";
    }).join("\n");
    rightCol =
      '<div class="k-toc-right">' +
      '<div class="k-toc-right-title">On this page</div>' +
      '<div class="k-toc-right-links">' + links + "</div>" +
      "</div>";
  }

  // static prev/next pager over the page's top-level (h2) parts
  const parts = sections.filter(function (s) { return s.level === 2; });
  let pagerInner = "";
  if (parts.length > 1) {
    const cur = parts[0];
    const curIdx = parts.indexOf(cur);
    const prev = curIdx > 0 ? parts[curIdx - 1] : null;
    const next = curIdx + 1 < parts.length ? parts[curIdx + 1] : null;
    const link = function (s, cls, label) {
      return '<a class="k-manual-pager-link ' + cls + '" href="#' + escapeHtml(s.id) + '">' +
        '<span class="k-manual-pager-label">' + label + "</span>" +
        '<span class="k-manual-pager-title">' + escapeHtml(s.title) + "</span></a>";
    };
    const backTop = '<a class="k-manual-backtop" href="#k-manual-title">Top</a>';
    pagerInner =
      (prev ? link(prev, "is-prev", "Previous") : '<span class="k-manual-pager-empty"></span>') +
      backTop +
      (next ? link(next, "is-next", "Next") : '<span class="k-manual-pager-empty"></span>');
  }

  const titleEl = '<h1 class="k-manual-title" id="k-manual-title">' + escapeHtml(docTitle) + "</h1>";
  const fixedContent = titleEl + contentHtml;

  const searchIndexJson = JSON.stringify(sections);

  const shell =
    '<header class="k-manual-header">' + brandHtml + searchHtml +
    '<button type="button" class="k-manual-toggle" id="karaf-theme-toggle" aria-label="Toggle dark mode">' +
    '<svg class="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"></path></svg>' +
    '<svg class="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>' +
    "</button>" +
    '<button type="button" class="k-manual-menu" id="k-manual-menu" aria-label="Toggle menu" aria-controls="toc"><span></span></button>' +
    "</header>" +
    '<div class="k-manual-body' + (hasToc ? "" : " k-no-toc") + '">' + tocCol + rightCol +
    '<div id="content" class="content">' + fixedContent +
    (pagerInner ? '<nav class="k-manual-pager" id="k-manual-pager" aria-label="Documentation sections">' + pagerInner + "</nav>" : "") +
    "</div>" +
    '</div>' +
    '<a class="k-back-top" href="#k-manual-title" title="Back to top" aria-label="Back to top">' +
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 19V5M5 12l7-7 7 7"></path></svg>' +
    "</a>" +
    '<script type="application/json" id="k-manual-search-index">' + searchIndexJson + "</script>";

  // 7. assemble final document
  const bodyOpenEnd = html.indexOf(">", html.indexOf("<body")) + 1;
  const tail = "</body></html>";
  html = html.slice(0, bodyOpenEnd) + "\n" + shell + "\n" + footerSnippet + "\n" + tail;

  fs.writeFileSync(file, html, "utf8");
  return fileName;
}

if (!fs.existsSync(baseDir)) {
  throw new Error("generated-docs not found: " + baseDir);
}

const logoDir = path.join(baseDir, "images");
fs.mkdirSync(logoDir, { recursive: true });
for (const logo of ["karaf-logo.svg", "karaf-logo-light.svg"]) {
  const src = path.join(__dirname, logo);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(logoDir, logo));
  }
}

let count = 0;
for (const file of walk(baseDir)) {
  processFile(file);
  count++;
}
console.log("postprocess: processed " + count + " file(s).");