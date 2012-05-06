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

}
