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
package org.apache.karaf.audit.logger;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.EventLayout;
import org.apache.karaf.audit.EventLogger;
import org.apache.karaf.audit.util.FastDateFormat;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class FileEventLogger implements EventLogger {

    private final Charset encoding;
    private final String policy;
    private final int files;
    private final boolean compress;
    private final Executor executor;
    private final EventLayout layout;
    private boolean daily;
    private long maxSize;
    private long size;
    private Path path;
    private Writer writer;
    private FastDateFormat fastDateFormat;
    private TimeZone timeZone;

    public FileEventLogger(String path, String encoding, String policy, int files, boolean compress, ThreadFactory factory, EventLayout layout, TimeZone timeZone) throws IOException {
        this.fastDateFormat = new FastDateFormat(timeZone, Locale.ENGLISH);
        this.timeZone = timeZone;
        this.path = Paths.get(path);
        this.encoding = Charset.forName(encoding);
        this.policy = policy;
        this.files = files;
        this.compress = compress;
        this.executor = Executors.newSingleThreadExecutor(factory);
        this.layout = layout;
        Files.createDirectories(this.path.getParent());

        for (String pol : policy.toLowerCase(Locale.ENGLISH).split("\\s+")) {
            if ("daily".equals(pol)) {
                daily = true;
            } else if (pol.matches("size\\([0-9]+(kb|mb|gb)?\\)")) {
                String str = pol.substring(5, pol.length() - 1);
                long mult;
                if (str.endsWith("kb")) {
                    mult = 1024;
                    str = str.substring(0, str.length() - 2);
                } else if (str.endsWith("mb")) {
                    mult = 1024 * 1024;
                    str = str.substring(0, str.length() - 2);
                } else if (str.endsWith("gb")) {
                    mult = 1024 * 1024 * 1024;
                    str = str.substring(0, str.length() - 2);
                } else {
                    mult = 1;
                }
                try {
                    maxSize = Long.parseLong(str) * mult;
                } catch (NumberFormatException t) {
                    // ignore
                }
                if (maxSize <= 0) {
                    throw new IllegalArgumentException("Unsupported policy: " + pol);
                }
            } else {
                throw new IllegalArgumentException("Unsupported policy: " + pol);
            }
        }
    }

    @Override
    public void write(Event event) throws IOException {
        long timestamp = event.timestamp();
        if (writer == null) {
            init();
        } else {
            check(timestamp);
        }
        layout.format(event, writer);
        writer.append("\n");
    }

    private void init() throws IOException {
        long timestamp = System.currentTimeMillis();
        if (Files.isRegularFile(path)) {
            size = Files.size(path);
            fastDateFormat.sameDay(Files.getLastModifiedTime(path).toMillis());
            if (trigger(timestamp)) {
                Path temp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
                Files.move(path, temp);
                executor.execute(() -> rotate(temp, timestamp));
            }
        }
        fastDateFormat.sameDay(timestamp);
        writer = new Writer(Files.newBufferedWriter(path, encoding, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        size = 0;
    }


    private void check(long timestamp) throws IOException {
        if (trigger(timestamp)) {
            if (writer != null) {
                writer.flush();
                if (Files.size(path) == 0) {
                    return;
                }
                writer.close();
            }
            Path temp = Files.createTempFile(path.getParent(), path.getFileName().toString() + ".", ".tmp");
            Files.delete(temp);
            Files.move(path, temp, StandardCopyOption.ATOMIC_MOVE);
            executor.execute(() -> rotate(temp, timestamp));
            writer = new Writer(Files.newBufferedWriter(path, encoding, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
            size = 0;
        }
    }

    private boolean trigger(long timestamp) {
        return maxSize > 0 && size > maxSize
                || daily && !fastDateFormat.sameDay(timestamp);
    }

    private void rotate(Path path, long timestamp) {
        try {
            // Compute final name
            String[] fix = getFileNameFix();
            List<String> paths = Files.list(path.getParent())
                    .filter(p -> !p.equals(this.path))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(p -> p.startsWith(fix[0]))
                    .filter(p -> !p.endsWith(".tmp"))
                    .collect(Collectors.toList());
            String date = new FastDateFormat(timeZone, Locale.ENGLISH).getDate(timestamp, FastDateFormat.YYYY_MM_DD);
            List<String> sameDate = paths.stream()
                    .filter(p -> p.matches("\\Q" + fix[0] + "-" + date + "\\E(-[0-9]+)?\\Q" + fix[1] + "\\E"))
                    .collect(Collectors.toList());
            String name = fix[0] + "-" + date + fix[1];
            int idx = 0;
            while (sameDate.contains(name)) {
                name = fix[0] + "-" + date + "-" + (++idx) + fix[1];
            }
            paths.add(name);
            Path finalPath = path.resolveSibling(name);
            // Compress or move the file
            if (compress) {
                try (OutputStream out = Files.newOutputStream(finalPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                     GZIPOutputStream zip = new GZIPOutputStream(out)) {
                    Files.copy(path, zip);
                }
                Files.delete(path);
            } else {
                Files.move(path, finalPath);
            }
            // Check number of files
            if (files > 0 && paths.size() > files) {
                Collections.sort(paths);
                paths.subList(paths.size() - files, paths.size()).clear();
                for (String p : paths) {
                    Files.delete(path.resolveSibling(p));
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private String[] getFileNameFix() {
        String str = path.getFileName().toString();
        String sfx = compress ? ".gz": "";
        int idx = str.lastIndexOf('.');
        if (idx > 0) {
            return new String[] { str.substring(0, idx), str.substring(idx) + sfx };
        } else {
            return new String[] { str, sfx };
        }
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    class Writer extends java.io.Writer implements Appendable, Closeable, Flushable {
        private final BufferedWriter writer;

        public Writer(BufferedWriter writer) {
            this.writer = writer;
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void write(int c) throws IOException {
            size += 1;
            writer.write(c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            size += len;
            writer.write(cbuf, off, len);
        }

        @Override
        public void write(String s, int off, int len) throws IOException {
            size += len;
            writer.write(s, off, len);
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            size += cbuf.length;
            writer.write(cbuf);
        }

        @Override
        public void write(String str) throws IOException {
            size += str.length();
            writer.write(str);
        }

        @Override
        public java.io.Writer append(CharSequence csq) throws IOException {
            size += csq.length();
            writer.append(csq);
            return this;
        }

        @Override
        public java.io.Writer append(CharSequence csq, int start, int end) throws IOException {
            size += end - start;
            writer.append(csq, start, end);
            return this;
        }

        @Override
        public java.io.Writer append(char c) throws IOException {
            size += 1;
            writer.append(c);
            return this;
        }
    }
}
