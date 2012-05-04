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

import jline.console.completer.FileNameCompleter;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.Completer;

import java.util.List;

/**
 * A completer for file names
 */
public class FileCompleter implements Completer {
    private FileNameCompleter completor = new FileNameCompleter();

    public FileCompleter(CommandSession session) {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int complete(String buffer, int cursor, List candidates) {
        return completor.complete(buffer, cursor, candidates);
    }
}
