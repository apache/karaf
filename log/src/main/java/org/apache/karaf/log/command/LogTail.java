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

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

@Command(scope = "log", name = "tail", description = "Continuously display log entries. Use ctrl-c to quit this command")
@Service
public class LogTail extends DisplayLog {

    @Reference
    Session session;

    @Reference
    LogService logService;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Object execute() throws Exception {
        PrintEventThread printThread = new PrintEventThread();
        ReadKeyBoardThread readKeyboardThread = new ReadKeyBoardThread(this, Thread.currentThread());
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
        private LogTail logTail;
        private Thread sessionThread;
        boolean readKeyboard = true;
        public ReadKeyBoardThread(LogTail logtail, Thread thread) {
            this.logTail = logtail;
            this.sessionThread = thread;
        }

        public void abort() {
            readKeyboard = false;            
        }

        public void run() {
            while (readKeyboard) {
                try {
                    int c = this.logTail.session.getKeyboard().read();
                    if (c < 0) {
                        this.sessionThread.interrupt();
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
                
            }
        }
    } 
    
    class PrintEventThread implements Runnable {

        PrintStream out = System.out;
        boolean doDisplay = true;

        public void run() {
            Iterable<PaxLoggingEvent> le = logService.getEvents(entries == 0 ? Integer.MAX_VALUE : entries);
            for (PaxLoggingEvent event : le) {
                if (event != null) {
                    printEvent(out, event);
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
                logService.addAppender(appender);
                
                while (doDisplay) {
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
