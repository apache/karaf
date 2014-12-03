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
package org.apache.karaf.shell.log;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.log.layout.PatternConverter;
import org.apache.karaf.shell.log.layout.PatternParser;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

@Command(scope = "log", name = "tail", description = "Continuously display log entries. Use ctrl-c to quit this command")
public class LogTail extends DisplayLog {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected Object doExecute() throws Exception {
        PrintEventThread printThread = new PrintEventThread();
        ReadKeyBoardThread readKeyboardThread = new ReadKeyBoardThread(Thread.currentThread());
        executorService.execute(printThread);
        executorService.execute(readKeyboardThread);
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(200);
            } catch (java.lang.InterruptedException e) {
                break;
            }
        }
        printThread.abort();
        readKeyboardThread.abort();
        executorService.shutdownNow();  
        return null;
    }
    
    class ReadKeyBoardThread implements Runnable {
        private Thread sessionThread;
        boolean readKeyboard = true;
        
        public ReadKeyBoardThread(Thread thread) {
            this.sessionThread = thread;
        }
        
        public void abort() {
            readKeyboard = false;            
        }
        
        public void run() {
            while (readKeyboard) {
                try {
                    int c = session.getKeyboard().read();
                    if (c < 0) {
                        sessionThread.interrupt();
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
                
            }
        }
    }
    
    class PrintEventThread implements Runnable {

        boolean doDisplay = true;

        public void run() {
            final PatternConverter cnv = new PatternParser(overridenPattern != null ? overridenPattern : pattern).parse();
            final PrintStream out = System.out;

            Iterable<PaxLoggingEvent> le = events.getElements(entries == 0 ? Integer.MAX_VALUE : entries);
            for (PaxLoggingEvent event : le) {
                if (event != null) {
                    display(cnv, event, out);
                }
            }
            // Tail
            final BlockingQueue<PaxLoggingEvent> queue = new LinkedBlockingQueue<PaxLoggingEvent>();
            PaxAppender appender = new PaxAppender() {
                public void doAppend(PaxLoggingEvent event) {
                    queue.add(event);
                }
            };
            try {
                events.addAppender(appender);
                while (doDisplay) {
                    PaxLoggingEvent logEvent = queue.take();
                    if (logEvent != null) {
                        display(cnv, logEvent, out);
                    }
                }
            } catch (InterruptedException e) {
                // Ignore
            } finally {
                events.removeAppender(appender);
            }
            out.println();
        }

        public void abort() {
            doDisplay = false;
        }
    }

}
