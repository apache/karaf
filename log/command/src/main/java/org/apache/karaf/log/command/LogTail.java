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

import java.io.PrintStream;
import java.util.concurrent.*;

import org.apache.karaf.shell.commands.Command;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

@Command(scope = "log", name = "tail", description = "Continuously display log entries. Use ctrl-c to quit this command")
public class LogTail extends DisplayLog {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    protected Object doExecute() throws Exception {
        PrintEventThread thread = new PrintEventThread();
        executorService.execute(thread);
       
        for (;;) {
            int c = session.getKeyboard().read();
            if (c < 0) {
                thread.abort();
                break;
            }

        }

        executorService.shutdownNow();
        return null;
    }
    
    
    class PrintEventThread implements Runnable {

        PrintStream out = System.out;
        boolean doDisplay = true;

        public void run() {
            Iterable<PaxLoggingEvent> le = logService.getEvents(entries == 0 ? Integer.MAX_VALUE : entries);
            for (PaxLoggingEvent event : le) {
                printEvent(out, event);
            }
            // Tail
            final BlockingQueue<PaxLoggingEvent> queue = new LinkedBlockingQueue<PaxLoggingEvent>();
            PaxAppender appender = new PaxAppender() {
                public void doAppend(PaxLoggingEvent event) {
                        queue.add(event);
                }
            };
            try {
                logService.addAppender(appender);
                
                if (doDisplay) {
                    PaxLoggingEvent event = queue.take();
                    if (event != null) {
                        printEvent(out, event);
                    }
                }
            } catch (InterruptedException e) {
                // Ignore
            } finally {
                logService.removeAppender(appender);
            }
            out.println();
            
        }

        public void abort() {
            doDisplay = false;
        }

    }

}
