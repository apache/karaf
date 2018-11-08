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
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.karaf.diagnostic.core.Dump;
import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.common.ZipDumpDestination;
import org.osgi.framework.BundleContext;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class DumpHandler implements SignalHandler, Closeable {
    private static final String SIGNAL = "HUP";
    private BundleContext context;
    private SignalHandler previous;

    public DumpHandler(BundleContext context) {
        this.context = context;
        previous = sun.misc.Signal.handle(new Signal(SIGNAL), this);
    }

    public void handle(Signal signal) {
        SimpleDateFormat dumpFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss-SSS");
        String fileName = "dump-" + dumpFormat.format(new Date()) + ".zip";
        DumpDestination destination = new ZipDumpDestination(new File(fileName));
        Dump.dump(context, destination, false, false);
    }

    @Override
    public void close() throws IOException {
        sun.misc.Signal.handle(new Signal(SIGNAL), previous);
    }
}
