package org.apache.felix.dependencymanager;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Allows you to enqueue tasks from multiple threads and then execute
 * them on one thread sequentially. It assumes more than one thread will
 * try to execute the tasks and it will make an effort to pick the first
 * task that comes along whilst making sure subsequent tasks return
 * without waiting.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class SerialExecutor {
    private final LinkedList m_workQueue = new LinkedList();
    private Runnable m_active;
    
    /**
     * Enqueue a new task for later execution. This method is
     * thread-safe, so multiple threads can contribute tasks.
     * 
     * @param runnable the runnable containing the actual task
     */
    public synchronized void enqueue(final Runnable runnable) {
    	m_workQueue.addLast(new Runnable() {
			public void run() {
				try {
					runnable.run();
				}
				finally {
					scheduleNext();
				}
			}
		});
    }
    
    /**
     * Execute any pending tasks. This method is thread safe,
     * so multiple threads can try to execute the pending
     * tasks, but only the first will be used to actually do
     * so. Other threads will return immediately.
     */
    public void execute() {
    	Runnable active;
    	synchronized (this) {
    		active = m_active;
    	}
    	if (active == null) {
    		scheduleNext();
    	}
    }

    private void scheduleNext() {
    	Runnable active;
    	synchronized (this) {
    		try {
    			m_active = (Runnable) m_workQueue.removeFirst();
    		}
    		catch (NoSuchElementException e) {
    			m_active = null;
    		}
    		active = m_active;
    	}
    	if (active != null) {
            active.run();
        }
    }
}
