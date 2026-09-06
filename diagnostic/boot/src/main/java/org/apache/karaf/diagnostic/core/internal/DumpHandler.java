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
package org.apache.karaf.diagnostic.core.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.karaf.diagnostic.core.Dump;
import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.common.ZipDumpDestination;
import org.osgi.framework.BundleContext;

/**
 * Creates a dump when the process receives SIGHUP. sun.misc.Signal is used reflectively, as in
 * org.apache.karaf.main.Main, to avoid the compiler warning about internal proprietary API.
 */
public class DumpHandler implements Closeable {

    private static final String SIGNAL = "HUP";

    private final BundleContext context;
    private final Method handleMethod;
    private final Object signal;
    private final Object previous;

    public DumpHandler(BundleContext context) throws Exception {
        this.context = context;

        final Class<?> signalClass = Class.forName("sun.misc.Signal");
        final Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");

        Object signalHandler = Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {
                signalHandlerClass
            },
                (proxy, method, args) -> {
                    handle();
                    return null;
                }
        );

        handleMethod = signalClass.getMethod("handle", signalClass, signalHandlerClass);
        signal = signalClass.getConstructor(String.class).newInstance(SIGNAL);
        previous = handleMethod.invoke(null, signal, signalHandler);
    }

    private void handle() {
        SimpleDateFormat dumpFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss-SSS");
        String fileName = "dump-" + dumpFormat.format(new Date()) + ".zip";
        DumpDestination destination = new ZipDumpDestination(new File(fileName));
        Dump.dump(context, destination, false, false);
    }

    @Override
    public void close() throws IOException {
        try {
            handleMethod.invoke(null, signal, previous);
        } catch (Exception e) {
            throw new IOException("Cannot restore the previous " + SIGNAL + " handler", e);
        }
    }

}
