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
package org.apache.servicemix.jpm.impl;

import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.servicemix.jpm.Process;
import org.apache.servicemix.jpm.ProcessBuilder;


public class ProcessBuilderImpl implements ProcessBuilder {

    private File dir;
    private String command;

    public ProcessBuilder directory(File dir) {
        this.dir = dir;
        return this;
    }

    public ProcessBuilder command(String command) {
        this.command = command;
        return this;
    }

    public Process start() throws IOException {
        return ProcessImpl.create(dir, command);
    }

    public Process attach(int pid) throws IOException {
        return ProcessImpl.attach(pid);
    }
}
