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
package org.apache.karaf.kittests;

import org.junit.Test;

import java.io.File;

import static org.apache.karaf.kittests.Helper.*;

public class KitTest {

    @Test
    public void testKit() throws Exception {
        File home = new File("target/karaf").getAbsoluteFile();
        System.out.println("Karaf home: " + home);
        System.out.println("Extracting Karaf");
        extractKit(home);

        System.out.println("Starting Karaf");
        Instance karaf = startKaraf(home);
        try {
            System.out.println("Wait for Karaf to be started");
            waitForKarafStarted(karaf, 20000);

            System.out.println("Launching stop script to shutdown");
            Process client = launchScript(home, "stop", "");
            try {
                waitForProcessEnd(client, 20000);
                System.out.println("Client terminated");
            } finally {
                kill(client);
            }

            System.out.println("Waiting for karaf to stop");
            waitForKarafStopped(karaf, 20000);
            System.out.println("Karaf stopped");

        } finally {
            kill(karaf);
        }

    }

}