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
package org.apache.karaf.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Saves a file through a temporary file of its own, synced to disk and then moved over
     * the file atomically. The file always holds its previous or its new content, even after
     * a crash or a power loss, and concurrent saves never clash. A failed write deletes the
     * temporary file.
     *
     * @param file the file to write
     * @param writer writes the new content to the stream it is given
     * @throws IOException if the content cannot be written or moved over the file
     */
    public static void atomicSave(File file, StreamWriter writer) throws IOException {
        File tmpFile = File.createTempFile(file.getName(), ".tmp", file.getAbsoluteFile().getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                writer.write(out);
                out.getFD().sync();
            }
            Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t) {
            tmpFile.delete();
            throw t;
        }
    }

    /**
     * Writes content to a stream. It must flush everything, wrappers included, before it
     * returns, as the stream is then synced to disk.
     */
    @FunctionalInterface
    public interface StreamWriter {
        void write(OutputStream out) throws IOException;
    }
}
