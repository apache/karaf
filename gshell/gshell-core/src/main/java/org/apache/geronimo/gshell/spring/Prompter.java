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
package org.apache.geronimo.gshell.spring;

import org.apache.geronimo.gshell.ansi.Renderer;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.layout.model.Node;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.ShellInfo;

/**
 * A prompter that displays the current sub-shell.
 */
public class Prompter implements Console.Prompter {

    private Renderer renderer = new Renderer();
    private ShellInfo shellInfo;
    private Environment env;

    public Prompter(ShellInfo shellInfo, Environment env) {
        this.shellInfo = shellInfo;
        this.env = env;
    }

    public String prompt() {
        String userName = shellInfo.getUserName();
        String hostName = shellInfo.getLocalHost().getHostName();

        Node start = (Node) env.getVariables().get(LayoutManager.CURRENT_NODE);
        String path = "/";
        if (start != null) {
            path = start.getPath();
        }

        return renderer.render("@|bold " + userName + "|@" + hostName + ":@|bold " + path + "|> ");
    }
}
