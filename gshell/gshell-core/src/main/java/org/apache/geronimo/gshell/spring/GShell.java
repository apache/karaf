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

import org.apache.geronimo.gshell.DefaultEnvironment;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.InteractiveShell;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 11, 2007
 * Time: 10:20:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class GShell implements Runnable {

    private InteractiveShell shell;
    private Thread thread;
    private IO io;
    private Environment env;
    private boolean start;

    public GShell(InteractiveShell shell) {
        this.shell = shell;
        this.io = new IO(System.in, System.out, System.err);
        this.env = new DefaultEnvironment(io);
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void start() {
        if (start) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }

    public void run() {
        IOTargetSource.setIO(io);
        EnvironmentTargetSource.setEnvironment(env);
        try {
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
