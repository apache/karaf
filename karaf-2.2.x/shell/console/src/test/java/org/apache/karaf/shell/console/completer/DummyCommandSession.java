/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console.completer;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;

public class DummyCommandSession implements CommandSession {
    public Object convert(Class<?> type, Object instance) {
        return null;
    }
    public CharSequence format(Object target, int level) {
        return null;
    }
    public void put(String name, Object value) {
    }
    public Object get(String name) {
        return null;
    }
    public PrintStream getConsole() {
        return null;
    }
    public InputStream getKeyboard() {
        return null;
    }
    public void close() {
    }
    public Object execute(CharSequence commandline) throws Exception {
        return null;
    }
}
