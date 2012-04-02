package org.apache.karaf.packages.core;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.karaf.packages.core.internal.Packages;
import org.junit.Test;


/**
 * Checks that the PackagesmBean is valid and can be installed in the MBeanServer
 *
 */
public class InstallMBeantest {
    @Test
    public void test() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Packages pack = new Packages(null);
        ObjectName oName = new ObjectName("org.apache.karaf:type=package,name=root");
        server.registerMBean(pack,  oName);
        server.unregisterMBean(oName);
    }
}
