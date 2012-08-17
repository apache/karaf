package org.apache.karaf.shell.commands.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.junit.Test;

/**
 * These are not real tests as they do no assertions. They simply help to see how the layout will look like.
 */
public class ThreadsTest {
    @Test
    public void testThreadlist() throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        new ThreadsAction().printThreadList(threadMXBean);
    }
    
    @Test
    public void testThreadInfo() throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        new ThreadsAction().printThread(threadMXBean, 1L);
    }
}
