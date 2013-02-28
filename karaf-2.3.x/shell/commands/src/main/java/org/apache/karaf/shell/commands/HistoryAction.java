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
package org.apache.karaf.shell.commands;

import jline.console.history.History;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.fusesource.jansi.Ansi;

/**
 * History command
 */
@Command(scope = "shell", name="history", description="Prints command history.")
public class HistoryAction extends AbstractAction {

    @Override
    protected Object doExecute() throws Exception {
        History history = (History) session.get(".jline.history");

        for (History.Entry element : history) {
            System.out.println(
                    Ansi.ansi()
                        .a("  ")
                        .a(Ansi.Attribute.INTENSITY_BOLD).render("%3d", element.index()).a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                        .a("  ")
                        .a(element.value())
                        .toString());
        }
        return null;
    }
}
