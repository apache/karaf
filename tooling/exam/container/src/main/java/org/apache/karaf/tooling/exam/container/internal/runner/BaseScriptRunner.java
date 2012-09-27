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
package org.apache.karaf.tooling.exam.container.internal.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of the script runner containging the entire self repeating code.
 */
public abstract class BaseScriptRunner implements Runner {

    protected InternalRunner runner;
    protected List<String> makeExec = new ArrayList<String>();
    protected String exec;

    public BaseScriptRunner(List<String> makeExec, String exec) {
        this.makeExec = makeExec;
        this.exec = exec;
        runner = new InternalRunner();
    }

    @Override
    public void exec(final String[] environment, final File karafBase, String javaHome, String[] javaOpts,
                     String[] javaEndorsedDirs, String[] javaExtDirs, String karafHome,
                     String karafData, String[] karafOpts, String[] opts, String[] classPath, String main, String options) {
        makeEnvironmentExecutable(karafBase);
        startSystem(environment, karafBase);
    }

    private void startSystem(final String[] environment, final File karafBase) {
        new Thread("KarafJavaRunner") {
            @Override
            public void run() {
                CommandLineBuilder commandLine = createCommandLine(environment, karafBase);
                runner.exec(commandLine, karafBase, environment);
            }
        }.start();
    }

    protected abstract CommandLineBuilder createCommandLine(final String[] environment, final File karafBase);

    private void makeEnvironmentExecutable(final File karafBase) {
        new File(karafBase, exec).setExecutable(true);
        for (String execEntry : makeExec) {
            new File(karafBase, execEntry).setExecutable(true);
        }
    }

    @Override
    public void shutdown() {
        runner.shutdown();
    }

}
