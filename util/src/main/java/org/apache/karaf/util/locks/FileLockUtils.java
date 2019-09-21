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
package org.apache.karaf.util.locks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.apache.felix.utils.properties.Properties;

public final class FileLockUtils {

    private FileLockUtils() { }

    public interface Runnable<T> {
        void run(T file) throws IOException;
    }

    public interface Callable<T, U> {
        U call(T file) throws IOException;
    }

    public static void execute(File file, Runnable<? super RandomAccessFile> callback) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            FileLock lock = raf.getChannel().lock();
            try {
                callback.run(raf);
            } finally {
                lock.release();
            }
        }
    }

    public static <T> T execute(File file, Callable<? super RandomAccessFile, T> callback) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            FileLock lock = raf.getChannel().lock();
            try {
                return callback.call(raf);
            } finally {
                lock.release();
            }
        }
    }

    public static void execute(File file, Runnable<? super Properties> callback, boolean writeToFile) throws IOException {
        execute(file, raf -> {
            Properties props = load(raf);
            callback.run(props);
            if (writeToFile) {
                save(props, raf);
            }
        });
    }

    public static <T> T execute(File file, Callable<? super Properties, T> callback, boolean writeToFile) throws IOException {
        return execute(file, raf -> {
            Properties props = load(raf);
            T result = callback.call(props);
            if (writeToFile) {
                save(props, raf);
            }
            return result;
        });
    }

    private static Properties load(RandomAccessFile raf) throws IOException {
        byte[] buffer = new byte[(int) raf.length()];
        raf.readFully(buffer);
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(buffer));
        return props;
    }

    private static void save(Properties props, RandomAccessFile raf) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.save(baos);
        raf.setLength(0);
        raf.write(baos.toByteArray());
    }

}
