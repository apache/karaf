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

import junit.framework.Assert;

import org.apache.karaf.main.util.Utils;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class MainLockingTest {
    
    @Test
    public void testLostMasterLock() throws Exception {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        File home = new File(basedir, "test-karaf-home");
        File data = new File(home, "data");

        Utils.deleteDirectory(data);

                String[] args = new String[0];
                System.setProperty("karaf.home", home.toString());
                System.setProperty("karaf.data", data.toString());
        System.setProperty("karaf.framework.factory", "org.apache.felix.framework.FrameworkFactory");

        System.setProperty("karaf.lock","true");
        System.setProperty("karaf.lock.delay","1000");
        System.setProperty("karaf.lock.class","org.apache.karaf.main.MockLock");

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
        
        Thread.sleep(2000);
        
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
}
