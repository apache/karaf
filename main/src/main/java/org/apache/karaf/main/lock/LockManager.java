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
package org.apache.karaf.main.lock;

public class LockManager {

    private Lock lock;
    private boolean exiting = false;

    /**
     * If a lock should be used before starting the runtime
     */
    public static final String PROPERTY_USE_LOCK = "karaf.lock";

    /**
     * The lock implementation
     */
    public static final String PROPERTY_LOCK_CLASS = "karaf.lock.class";

    public static final String PROPERTY_LOCK_CLASS_DEFAULT = SimpleFileLock.class.getName();
    private final LockCallBack lockCallback;
    private final int lockCheckInterval;
    
	public LockManager(Lock lock, LockCallBack lockCallback, int lockCheckIntervalSeconds) {
	    this.lock = lock;
        this.lockCallback = lockCallback;
        this.lockCheckInterval = lockCheckIntervalSeconds;
	}
	
    public void startLockMonitor() {
        new Thread() {
            public void run() {
                runLockManager();
            }
        }.start();
    }
    
    public void stopLockMonitor() {
        this.exiting = true;
    }
    
    private void runLockManager() {
        while (!exiting) {
            try {
                if (lock.lock()) {
                    lockCallback.lockAquired();
                    for (;;) {
                        if (!lock.isAlive() || exiting) {
                            break;
                        }
                        Thread.sleep(lockCheckInterval);
                    }
                    if (!exiting) {
                        lockCallback.lockLost();
                    }
                } else {
                    lockCallback.waitingForLock();
                }
                Thread.sleep(lockCheckInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void unlock() throws Exception {
        lock.release();
    }

    public Lock getLock() {
        return lock;
    }
}
