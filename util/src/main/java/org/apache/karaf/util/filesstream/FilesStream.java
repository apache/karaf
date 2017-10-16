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
package org.apache.karaf.util.filesstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public final class FilesStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilesStream.class);

    private FilesStream() {
	}

    /**
     * Returns a stream of Paths for the given fileNames.
     * The given names can be delimited by ",". A name can also contain
     * {@link java.nio.file.FileSystem#getPathMatcher} syntax to refer to matching files.  
     * 
     * @param fileNames list of names 
     * @return Paths to the scripts 
     */
	public static Stream<Path> stream(String fileNames) {
        if (fileNames == null) {
            return Stream.empty();
        }
        List<String> files = new ArrayList<>();
        List<String> generators = new ArrayList<>();
        StringBuilder buf = new StringBuilder(fileNames.length());
        boolean hasUnescapedReserved = false;
        boolean escaped = false;
        for (int i = 0; i < fileNames.length(); i++) {
            char c = fileNames.charAt(i);
            if (escaped) {
                buf.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == ',') {
                if (hasUnescapedReserved) {
                    generators.add(buf.toString());
                } else {
                    files.add(buf.toString());
                }
                hasUnescapedReserved = false;
                buf.setLength(0);
            } else if ("*?{[".indexOf(c) >= 0) {
                hasUnescapedReserved = true;
                buf.append(c);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            if (hasUnescapedReserved) {
                generators.add(buf.toString());
            } else {
                files.add(buf.toString());
            }
        }
        Path cur = Paths.get(System.getProperty("karaf.etc"));
        return Stream.concat(
                files.stream().map(cur::resolve),
                generators.stream().flatMap(s -> files(cur, s)));
    }

    private static Stream<Path> files(Path cur, String glob) {
        String prefix;
        String rem;
        int idx = glob.lastIndexOf(File.separatorChar);
        if (idx >= 0) {
            prefix = glob.substring(0, idx + 1);
            rem = glob.substring(idx + 1);
        } else {
            prefix = "";
            rem = glob;
        }
        Path dir = cur.resolve(prefix);
        final PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + rem);
        Stream.Builder<Path> stream = Stream.builder();
        try {
            Files.walkFileTree(dir,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.equals(dir)) {
                                return FileVisitResult.CONTINUE;
                            }
                            if (Files.isHidden(file)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            Path r = dir.relativize(file);
                            if (matcher.matches(r)) {
                                stream.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (!Files.isHidden(file)) {
                                Path r = dir.relativize(file);
                                if (matcher.matches(r)) {
                                    stream.add(file);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Error generating filenames", e);
        }
        return stream.build();
    }
}
