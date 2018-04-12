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
package org.apache.karaf.log.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clear the last log entries.
 */
@Command(scope = "log", name = "load-test", description = "Load test log.")
@Service
public class LoadTest implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadTest.class);

    @Option(name = "--threads")
    private int threads = 1;

    @Option(name = "--messaged")
    private int messages = 1000;

    @Override
    public Object execute() throws Exception {
        Thread[] th = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int idxThread = i;
            th[i] = new Thread(() -> {
                for (int i1 = 0; i1 < messages; i1++) {
                    LOGGER.info("Message {} / {}", idxThread, i1);
                }
            });
        }
        long t0 = System.currentTimeMillis();
        for (Thread thread : th) {
            thread.start();
        }
        for (Thread thread : th) {
            thread.join();
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Time: " + (t1 - t0) + " ms");
        System.out.println("Throughput: " + ((messages * threads) / (t1 - t0 + 0.0)) + " msg/ms");
        return null;
    }

}
