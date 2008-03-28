/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.test.log;

import java.util.Enumeration;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

public class LogImpl implements LogService, LogReaderService {
    
    Vector m_messages = new Vector();

    public void log(int arg0, String arg1) {
       add(new LogEntryImpl(arg0, arg1, null, null));
    }

    public void log(final int arg0, final String arg1, final Throwable arg2) {
        Runnable runnable = new Runnable() {
            public void run() {
                add(new LogEntryImpl(arg0, arg1, arg2, null));
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void log(final ServiceReference arg0, final int arg1, final String arg2) {
        Runnable runnable = new Runnable() {
            public void run() {
                add(new LogEntryImpl(arg1, arg2, null, arg0));
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void log(final ServiceReference arg0, final int arg1, final String arg2, final Throwable arg3) {
//        Runnable runnable = new Runnable() {
//            public void run() {
//                add(new LogEntryImpl(arg1, arg2, arg3, arg0));
//            }
//        };
//        Thread thread = new Thread(runnable);
//        thread.start();
    }
    
    private void add(LogEntry entry) {
        m_messages.add(entry);
    }

    public void addLogListener(LogListener arg0) {
        throw new UnsupportedOperationException("Log Listener not supported");
        
    }

    public Enumeration getLog() {
       return m_messages.elements();
    }

    public void removeLogListener(LogListener arg0) {
        throw new UnsupportedOperationException("Log Listener not supported");
        
    }
    
    private class LogEntryImpl implements LogEntry {
        
        private int level;
        private String message;
        private Throwable exception;
        private ServiceReference reference;
        private long time;
        

        LogEntryImpl(int l, String m, Throwable e, ServiceReference ref) {
            level = l;
            message = m;
            exception = e;
            reference = ref;
            time = System.currentTimeMillis();
        }
        
        
        public Bundle getBundle() {
           return null;
        }

        public Throwable getException() {
            return exception;
        }

        public int getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public ServiceReference getServiceReference() {
            return reference;
        }

        public long getTime() {
            return time;
        }
        
    }

}
