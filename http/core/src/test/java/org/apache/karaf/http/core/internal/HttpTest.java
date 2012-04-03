package org.apache.karaf.http.core.internal;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.junit.Test;

public class HttpTest {

    @Test
    public void testRegisterMBean() throws Exception {
        Http httpMBean = new Http(new ServletServiceImpl(new ServletEventHandler()));
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        mbeanServer.registerMBean(httpMBean, new ObjectName("org.apache.karaf:type=http,name=root"));
        
        TabularData data = httpMBean.getServlets();
    }
}
