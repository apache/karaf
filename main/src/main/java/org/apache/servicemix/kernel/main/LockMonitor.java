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
package org.apache.servicemix.kernel.main;

import org.apache.felix.framework.Felix;

/**
 * LockMonitor monitors the status of the startup lock,
 * if monitor determines that the lock has been lost
 * then it will signal the SMX process to shutdown and 
 * return to attempting to get the lock.
 *
 * @version $Revision: $
 */
class LockMonitor extends Thread {

    private Lock lock;
    private Felix felix;
    private int timeout;

    LockMonitor (Lock lock, Felix felix, int timeout) {
        this.lock = lock;
        this.felix = felix;
        this.timeout = timeout;
    }

    public void run() {
        try {
            for (;;) {
                if (!lock.isAlive()) {
                    break;
                }
                Thread.sleep(timeout);
            }
            System.out.println("Lost the lock, stopping this instance ...");
            felix.stop();
        } catch (Exception ex) {
            System.err.println("An error has occured while monitoring failover lock: " + ex.getMessage());
        } finally {
            System.exit(1);
        }
    }

}
