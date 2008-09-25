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
package org.apache.servicemix.jpm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProcessTest {

    @Test
    public void testCreate() throws Exception {
        StringBuilder command = new StringBuilder();
        command.append("java -classpath ");
        String clRes = getClass().getName().replace('.', '/') + ".class";
        String str = getClass().getClassLoader().getResource(clRes).toString();
        str = str.substring("file:".length(), str.indexOf(clRes));
        command.append(str);
        command.append(" ");
        command.append(MainTest.class.getName());
        command.append(" ");
        command.append(60000);
        System.err.println("Executing: " + command.toString());

        ProcessBuilder builder = ProcessBuilderFactory.newInstance().newBuilder();
        Process p = builder.command(command.toString()).start();
        assertNotNull(p);
        System.err.println("Process: " + p.getPid());
        assertNotNull(p.getPid());
        Thread.currentThread().sleep(1000);
        System.err.println("Running: " + p.isRunning());
        assertTrue(p.isRunning());
        System.err.println("Destroying");
        p.destroy();
        System.err.println("Running: " + p.isRunning());
        assertFalse(p.isRunning());
    }

    /*
    @Test
    @Ignore("When the process creation fails, no error is reported by the script")
    public void testFailure() throws Exception {
        ProcessBuilder builder = ProcessBuilderFactory.newInstance().newBuilder();
        Process p = builder.command("ec").start();
        fail("An exception should have been thrown");
    }
    */
}
