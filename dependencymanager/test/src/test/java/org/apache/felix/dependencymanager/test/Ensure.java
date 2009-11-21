package org.apache.felix.dependencymanager.test;

import junit.framework.Assert;

public class Ensure {
    int step = 1;
    public synchronized void step(int nr) {
        Assert.assertEquals(nr, step);
        step++;
    }
}
