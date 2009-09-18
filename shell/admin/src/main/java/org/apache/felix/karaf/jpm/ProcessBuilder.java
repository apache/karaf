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
package org.apache.felix.karaf.jpm;

import java.io.File;
import java.io.IOException;
import java.lang.*;

/**
 * Interface used to create new processes.
 */
public interface ProcessBuilder {

    /**
     * Specified the current directory to run the command from
     *
     * @param dir the directory to run the command from
     * @return the ProcessBuilder instance
     */
    ProcessBuilder directory(File dir);

    /**
     * Set the command to execute
     *
     * @param command the command to execute
     * @return the ProcessBuilder instance
     */
    ProcessBuilder command(String command);

    /**
     * Create and start the process
     *
     * @return the process that has been started
     * @throws IOException if the process can not be created
     */
    org.apache.felix.karaf.jpm.Process start() throws IOException;

    /**
     * Attach to an existing process
     *
     * @return the process that has been attached
     * @throws IOException if the process can not be attached to
     */
    org.apache.felix.karaf.jpm.Process attach(int pid) throws IOException;

}
