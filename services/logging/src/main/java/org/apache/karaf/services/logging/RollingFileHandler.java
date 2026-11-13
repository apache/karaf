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
package org.apache.karaf.services.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * File handler always writing to the same file (unlike {@link java.util.logging.FileHandler} which writes to
 * <code>file.0</code> when rotating). When the file reaches <code>maxSize</code>, it's renamed to <code>file.1</code>
 * (previous backups being shifted up to <code>file.maxBackupIndex</code>) and a new file is started. If the file can't
 * be renamed, it's copied to <code>file.1</code> and truncated.
 */
public class RollingFileHandler extends StreamHandler {

    private final File file;
    private long maxSize;
    private int maxBackupIndex;
    private CountingOutputStream output;

    public RollingFileHandler(File file, long maxSize, int maxBackupIndex) throws IOException {
        this.file = file.getAbsoluteFile();
        this.maxSize = maxSize;
        this.maxBackupIndex = maxBackupIndex;
        setEncoding(StandardCharsets.UTF_8.name());
        open();
    }

    public File getFile() {
        return file;
    }

    public synchronized void setLimits(long maxSize, int maxBackupIndex) {
        this.maxSize = maxSize;
        this.maxBackupIndex = maxBackupIndex;
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
        if (maxSize > 0 && output.getCount() >= maxSize) {
            rollOver();
        }
    }

    private void open() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory " + parent);
        }
        output = new CountingOutputStream(new FileOutputStream(file, true), file.length());
        setOutputStream(output);
    }

    private void rollOver() {
        super.close();
        boolean rolled;
        if (maxBackupIndex > 0) {
            backup(maxBackupIndex).delete();
            for (int i = maxBackupIndex - 1; i >= 1; i--) {
                File backup = backup(i);
                if (backup.exists()) {
                    rename(backup, backup(i + 1));
                }
            }
            rolled = rename(file, backup(1)) || copyAndTruncate(backup(1));
        } else {
            rolled = file.delete() || truncate();
        }
        try {
            open();
        } catch (IOException e) {
            reportError("Unable to roll over " + file, e, ErrorManager.OPEN_FAILURE);
            return;
        }
        if (!rolled) {
            reportError("Unable to roll over " + file, null, ErrorManager.GENERIC_FAILURE);
            // don't try again for each record, but once maxSize bytes have been written
            output.resetCount();
        }
    }

    // package visible for testing
    boolean rename(File source, File target) {
        return source.renameTo(target);
    }

    // On Windows, a file still open by another stream (like the Karaf Main bootstrap log handlers do with karaf.log)
    // can't be renamed or deleted, but it can be copied and truncated
    private boolean copyAndTruncate(File backup) {
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return false;
        }
        return truncate();
    }

    private boolean truncate() {
        try (FileOutputStream truncated = new FileOutputStream(file)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private File backup(int index) {
        return new File(file.getPath() + "." + index);
    }

    private static final class CountingOutputStream extends FilterOutputStream {

        private long count;

        CountingOutputStream(OutputStream out, long count) {
            super(new BufferedOutputStream(out));
            this.count = count;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }

        long getCount() {
            return count;
        }

        void resetCount() {
            count = 0;
        }
    }
}
