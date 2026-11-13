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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class RollingFileHandlerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testRollOverKeepsFileName() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        // each message is 52 bytes, so the file is rolled over every 2 messages
        publish(new RollingFileHandler(file, 100, 2), 11);

        assertTrue(read(file).contains("message 10 "));
        assertTrue(read(backup(file, 1)).contains("message 9 "));
        assertTrue(read(backup(file, 2)).contains("message 7 "));
        assertFalse(backup(file, 3).exists());
        assertFalse(backup(file, 0).exists());
    }

    @Test
    public void testRollOverWithoutBackup() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        publish(new RollingFileHandler(file, 100, 0), 3);

        assertEquals(message(2), read(file));
        assertFalse(backup(file, 1).exists());
    }

    @Test
    public void testRollOverCopiesWhenRenameFails() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        // as on Windows when karaf.log is still open by the Karaf Main bootstrap log handlers
        RollingFileHandler handler = new RollingFileHandler(file, 100, 2) {
            @Override
            boolean rename(File source, File target) {
                return !source.equals(file.getAbsoluteFile()) && super.rename(source, target);
            }
        };
        List<String> errors = new ArrayList<>();
        handler.setErrorManager(errorManager(errors));
        publish(handler, 5);

        assertEquals(message(4), read(file));
        assertEquals(message(2) + message(3), read(backup(file, 1)));
        assertEquals(message(0) + message(1), read(backup(file, 2)));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testFailedRollOverIsRetriedAfterMaxSize() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        // a non empty directory can't be replaced by the rename or the copy of karaf.log
        File backup = backup(file, 1);
        assertTrue(backup.mkdir());
        Files.write(new File(backup, "file").toPath(), new byte[] {0});
        RollingFileHandler handler = new RollingFileHandler(file, 100, 1);
        List<String> errors = new ArrayList<>();
        handler.setErrorManager(errorManager(errors));
        publish(handler, 11);

        // the roll over is tried every 2 messages (not on each message once maxSize is reached)
        assertEquals(5, errors.size());
        assertEquals(11 * message(0).length(), file.length());
    }

    @Test
    public void testNoMaxSize() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        publish(new RollingFileHandler(file, 0, 2), 10);

        assertEquals(10 * message(0).length(), file.length());
        assertFalse(backup(file, 1).exists());
    }

    @Test
    public void testAppendToExistingFile() throws Exception {
        File file = new File(tmp.getRoot(), "karaf.log");
        Files.write(file.toPath(), "existing\n".getBytes(StandardCharsets.UTF_8));
        publish(new RollingFileHandler(file, 0, 2), 1);

        assertEquals("existing\n" + message(0), read(file));
    }

    private static void publish(RollingFileHandler handler, int count) {
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage();
            }
        });
        try {
            for (int i = 0; i < count; i++) {
                handler.publish(new LogRecord(Level.INFO, message(i)));
            }
        } finally {
            handler.close();
        }
    }

    private static ErrorManager errorManager(List<String> errors) {
        return new ErrorManager() {
            @Override
            public synchronized void error(String msg, Exception ex, int code) {
                errors.add(msg);
            }
        };
    }

    private static String message(int index) {
        return String.format("message %-2d %s%n", index, "x".repeat(40)).replace(System.lineSeparator(), "\n");
    }

    private static File backup(File file, int index) {
        return new File(file.getPath() + "." + index);
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
