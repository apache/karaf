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
package org.apache.karaf.config.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;

/*
 * A mock CommandSession implementation that only cares about the properties set on the session
 */
class MockCommandSession implements Session {

    private Map<String, Object> properties = new HashMap<>();

    public void close() {
        // not implemented
    }

    public Object execute(CharSequence commandline) throws Exception {
        // not implemented
        return null;
    }

    public Object get(String name) {
        return properties.get(name);
    }

    public PrintStream getConsole() {
        // not implemented
        return null;
    }

    public InputStream getKeyboard() {
        // not implemented
        return null;
    }

    public void put(String name, Object value) {
        properties.put(name, value);
    }

    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        return null;
    }

    @Override
    public Terminal getTerminal() {
        return null;
    }

    @Override
    public History getHistory() {
        return null;
    }

    @Override
    public Registry getRegistry() {
        return null;
    }

    @Override
    public SessionFactory getFactory() {
        return null;
    }

    @Override
    public Path currentDir() {
        return null;
    }

    @Override
    public void currentDir(Path path) {
    }

    @Override
    public String resolveCommand(String name) {
        return null;
    }

    @Override
    public void run() {

    }
}
