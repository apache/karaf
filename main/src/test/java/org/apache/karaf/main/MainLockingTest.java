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
package org.apache.karaf.main;

import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.File;
import java.io.IOException;

import org.apache.karaf.main.util.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class MainLockingTest {
    private File home;

    private File data;
    private File log;

    @Before
    public void setUp() throws IOException {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        home = new File(basedir, "test-karaf-home");
        data = new File(home, "data" + System.currentTimeMillis());
        log = new File(home, "log" + System.currentTimeMillis());

        Utils.deleteDirectory(data);

        System.setProperty("karaf.home", home.toString());
        System.setProperty("karaf.data", data.toString());
        System.setProperty("karaf.log", log.toString());
        System.setProperty("karaf.framework.factory", "org.apache.felix.framework.FrameworkFactory");

        System.setProperty("karaf.lock", "true");
        System.setProperty("karaf.lock.delay", "1000");
        System.setProperty("karaf.lock.class", "org.apache.karaf.main.MockLock");
    }

    @After
    public void tearDown() {
        home = null;
        data = null;
        log = null;

        System.clearProperty("karaf.home");
        System.clearProperty("karaf.data");
        System.clearProperty("karaf.log");
        System.clearProperty("karaf.framework.factory");

        System.clearProperty("karaf.lock");
        System.clearProperty("karaf.lock.delay");
        System.clearProperty("karaf.lock.lostThreshold");
        System.clearProperty("karaf.lock.class");

        System.clearProperty("karaf.pid.file");
    }

    @Test
    public void testLostMasterLock() throws Exception {
        String[] args = new String[0];
        Main main = new Main(args);
        main.launch();
        Framework framework = main.getFramework();
        String activatorName = TimeoutShutdownActivator.class.getName().replace('.', '/') + ".class";
        Bundle bundle = framework.getBundleContext().installBundle("foo",
                TinyBundles.bundle()
                    .set( Constants.BUNDLE_ACTIVATOR, TimeoutShutdownActivator.class.getName() )
                    .add( activatorName, getClass().getClassLoader().getResourceAsStream( activatorName ) )
                    .build( withBnd() )
        );
        
        bundle.start();

        Thread.sleep(1000);

        FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);

        MockLock lock = (MockLock) main.getLock();

        Assert.assertEquals(100, sl.getStartLevel());

        // simulate losing a lock
        lock.setIsAlive(false);
        lock.setLock(false);

        // lets wait until the start level change is complete
        lock.waitForLock();
        Assert.assertEquals(1, sl.getStartLevel());

        Thread.sleep(1000);

        // get lock back
        lock.setIsAlive(true);
        lock.setLock(true);

        Thread.sleep(1000);

        // exit framework + lock loop
        main.destroy();
    }

    @Test
    public void testRetainsMasterLockOverFluctuation() throws Exception {
        System.setProperty("karaf.lock.lostThreshold", "3");

        String[] args = new String[0];
        Main main = new Main(args);
        main.launch();
        Framework framework = main.getFramework();
        String activatorName = TimeoutShutdownActivator.class.getName().replace('.', '/') + ".class";
        Bundle bundle = framework.getBundleContext().installBundle("foo",
                TinyBundles.bundle()
                    .set( Constants.BUNDLE_ACTIVATOR, TimeoutShutdownActivator.class.getName() )
                    .add( activatorName, getClass().getClassLoader().getResourceAsStream( activatorName ) )
                    .build( withBnd() )
        );

        bundle.start();       
        
        FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
        
        MockLock lock = (MockLock) main.getLock();

        Thread.sleep(1000);
        Assert.assertEquals(100, sl.getStartLevel());       

        // simulate losing a lock
        lock.setIsAlive(false);
        lock.setLock(false);
        
        // lets wait until the start level change is complete
        lock.waitForLock();
        Assert.assertEquals(100, sl.getStartLevel());

        Thread.sleep(1000);
        
        // get lock back
        lock.setIsAlive(true);
        lock.setLock(true);

        
        Thread.sleep(1000);
        
        // exit framework + lock loop
        main.destroy();
    }

    @Test
    public void testLostMasterLockAfterThreshold() throws Exception {
        System.setProperty("karaf.lock.lostThreshold", "3");

        String[] args = new String[0];
        Main main = new Main(args);
        main.launch();
        Framework framework = main.getFramework();
        String activatorName = TimeoutShutdownActivator.class.getName().replace('.', '/') + ".class";
        Bundle bundle = framework.getBundleContext().installBundle("foo",
                TinyBundles.bundle().set(Constants.BUNDLE_ACTIVATOR, TimeoutShutdownActivator.class.getName())
                        .add(activatorName, getClass().getClassLoader().getResourceAsStream(activatorName))
                        .build(withBnd()));

        bundle.start();

        FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);

        MockLock lock = (MockLock) main.getLock();

        Assert.assertEquals(100, sl.getStartLevel());

        // simulate losing a lock
        lock.setIsAlive(false);
        lock.setLock(false);

        // lets wait until the start level change is complete - thrice
        // (lostThreshold)
        Thread.sleep(5000);
        Assert.assertEquals(1, sl.getStartLevel());

        Thread.sleep(1000);

        // get lock back
        lock.setIsAlive(true);
        lock.setLock(true);

        Thread.sleep(1000);

        // exit framework + lock loop
        main.destroy();
    }

    @Test
    public void testMasterWritesPid() throws Exception {
        // use data because it's always deleted at the beginning of the test
        File pidFile = new File(data, "test-karaf.pid");
        System.setProperty("karaf.pid.file", pidFile.toString());

        try {
            Assert.assertFalse(pidFile.isFile());

            String[] args = new String[0];
            Main main = new Main(args);
            main.launch();

            Thread.sleep(1000);

            Framework framework = main.getFramework();
            FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
            Assert.assertEquals(100, sl.getStartLevel());

            MockLock lock = (MockLock) main.getLock();

            Assert.assertTrue(lock.lock());
            Assert.assertTrue(lock.isAlive());
            Assert.assertTrue(pidFile.isFile());

            main.destroy();
        } finally {
            System.clearProperty("karaf.pid.file");
        }
    }

    @Test
    public void testSlaveWritesPid() throws Exception {
        // simulate that the lock is not acquired (i.e. instance runs as slave)
        System.setProperty("test.karaf.mocklock.initiallyLocked", "false");
        System.setProperty("karaf.lock.level", "59");

        // use data because it's always deleted at the beginning of the test
        File pidFile = new File(data, "test-karaf.pid");
        System.setProperty("karaf.pid.file", pidFile.toString());

        try {
            Assert.assertFalse(pidFile.isFile());

            String[] args = new String[0];
            Main main = new Main(args);
            main.launch();

            Thread.sleep(1000);

            Framework framework = main.getFramework();
            FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
            Assert.assertEquals(59, sl.getStartLevel());

            MockLock lock = (MockLock) main.getLock();

            Assert.assertFalse(lock.lock());
            Assert.assertTrue(lock.isAlive());
            Assert.assertTrue(pidFile.isFile());

            main.destroy();
        } finally {
            System.clearProperty("test.karaf.mocklock.initiallyLocked");
            System.clearProperty("karaf.lock.level");
            System.clearProperty("karaf.pid.file");
        }
    }
}
