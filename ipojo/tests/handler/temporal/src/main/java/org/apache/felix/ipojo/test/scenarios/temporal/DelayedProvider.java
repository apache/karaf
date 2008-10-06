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
package org.apache.felix.ipojo.test.scenarios.temporal;

import org.apache.felix.ipojo.ComponentInstance;

public class DelayedProvider implements Runnable {
    
    
    public static final long DELAY = 1000;
    ComponentInstance instance;
    long delay = DELAY;
    Thread thread;
    
    public DelayedProvider(ComponentInstance ci) {
        instance =ci;
    }
    
    public DelayedProvider(ComponentInstance ci, long time) {
        instance =ci;
        delay = time;
    }    
    
    public void start() {
        thread = new Thread(this);
        thread.start();
    }
    
    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void run() {
            System.out.println("Start sleeping for " + delay);
            long begin = System.currentTimeMillis();
            try {
                Thread.sleep(delay);
                long end = System.currentTimeMillis();
                if (end - begin < delay) {
                	// Wait for the remaining time
                	Thread.sleep(delay - (end - begin));
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted ...");
                return;
            }
            System.out.println("Wakeup");
            thread = null;
            instance.start();
            System.out.println(instance.getInstanceName() + " started");
    }

}
