/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.gshell.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 18, 2008
 * Time: 4:35:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoCloseInputStreamTest extends TestCase {

    public void testStream() throws Exception {
        final InputStream in = new NoCloseInputStream(System.in);
        new Thread() {
            public void run() {
                try {
                    int c;
                    System.err.println("Reading from in...");
                    while ((c = in.read()) != -1) {
                        System.err.println("Read from in: " + c);
                    }
                    System.err.println("Exiting thread...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        Thread.sleep(2000);

        in.close();

        Thread.sleep(2000);

    }
}
