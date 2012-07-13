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
package org.apache.karaf.shell.console.impl.jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import jline.Terminal;

import org.fusesource.jansi.AnsiConsole;

final class StreamWrapUtil {
    private StreamWrapUtil() {
    }

    private static Object invokePrivateMethod(Object o, String methodName, Object[] params) throws Exception {
        final Method methods[] = o.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methodName.equals(methods[i].getName())) {
                methods[i].setAccessible(true);
                return methods[i].invoke(o, params);
            }
        }
        return o;
    }

    /**
     * unwrap stream so it can be recognized by the terminal and wrapped to get
     * special keys in windows
     * 
     * @param stream
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T unwrapBIS(T stream) {
        try {
            return (T) invokePrivateMethod(stream, "getInIfOpen", null);
        } catch (Throwable t) {
            return stream;
        }
    }

    private static PrintStream wrap(PrintStream stream) {
        OutputStream o = AnsiConsole.wrapOutputStream(stream);
        if (o instanceof PrintStream) {
            return ((PrintStream) o);
        } else {
            return new PrintStream(o);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap(T stream) {
        try {
            Method mth = stream.getClass().getMethod("getRoot");
            return (T) mth.invoke(stream);
        } catch (Throwable t) {
            return stream;
        }
    }

    static InputStream reWrapIn(Terminal terminal, InputStream stream) {
        try {
            return terminal.wrapInIfNeeded(unwrapBIS(stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static PrintStream reWrap(PrintStream stream) {
        return wrap(unwrap(stream));
    }
}
