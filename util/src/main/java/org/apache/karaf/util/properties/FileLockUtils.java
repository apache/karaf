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
package org.apache.karaf.util.properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.apache.felix.utils.properties.Properties;

public final class FileLockUtils {

    private FileLockUtils() { }

    public static interface Runnable {
        void run(RandomAccessFile file) throws IOException;
    }

    public static interface Callable<T> {
        T call(RandomAccessFile file) throws IOException;
    }

    public static interface RunnableWithProperties {
        void run(Properties properties) throws IOException;
    }

    public static interface CallableWithProperties<T> {
        T call(Properties properties) throws IOException;
    }

    public static void execute(File file, Runnable callback) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            FileLock lock = raf.getChannel().lock();
            try {
                callback.run(raf);
            } finally {
                lock.release();
            }
        } finally {
            raf.close();
        }
    }

    public static <T> T execute(File file, Callable<T> callback) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            FileLock lock = raf.getChannel().lock();
            try {
                return callback.call(raf);
            } finally {
                lock.release();
            }
        } finally {
            raf.close();
        }
    }

    public static void execute(File file, final RunnableWithProperties callback) throws IOException {
        execute(file, new Runnable() {
            public void run(RandomAccessFile file) throws IOException {
                byte[] buffer = new byte[(int) file.length()];
                file.readFully(buffer);
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(buffer));
                callback.run(props);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                props.store(baos, null);
                file.setLength(0);
                file.write(baos.toByteArray());
            }
        });
    }

    public static <T> T execute(File file, final CallableWithProperties<T> callback) throws IOException {
        return execute(file, new Callable<T>() {
            public T call(RandomAccessFile file) throws IOException {
                byte[] buffer = new byte[(int) file.length()];
                file.readFully(buffer);
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(buffer));
                T result = callback.call(props);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                props.store(baos, null);
                file.setLength(0);
                file.write(baos.toByteArray());
                return result;
            }
        });
    }

}
