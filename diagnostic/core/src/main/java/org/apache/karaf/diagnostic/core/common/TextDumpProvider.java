/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.core.common;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;

/**
 * Base class for dump providers which writes text to destination.
 */
public abstract class TextDumpProvider implements DumpProvider {

    /**
     * Name of the file.
     */
    private final String name;

    /**
     * Creates new dump provider.
     * 
     * @param name Name of the file.
     */
    protected TextDumpProvider(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void createDump(DumpDestination destination) throws Exception {
        OutputStream outputStream = destination.add(name);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        try {
            writeDump(outputStreamWriter);
        } finally {
            outputStreamWriter.close();
            outputStream.close();
        }
    }

    /**
     * This method should create output.
     * 
     * @param outputStreamWriter Stream which points to file specified in constructor.
     * @throws Exception If any problem occur.
     */
    protected abstract void writeDump(OutputStreamWriter outputStreamWriter) throws Exception;

}
