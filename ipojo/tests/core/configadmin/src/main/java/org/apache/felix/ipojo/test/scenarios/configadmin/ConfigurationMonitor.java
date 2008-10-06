package org.apache.felix.ipojo.test.scenarios.configadmin;

import junit.framework.Assert;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

public class ConfigurationMonitor implements ConfigurationListener {
    
    private String waitForEvent;
    private boolean detected;
    private ServiceRegistration reg;

    public synchronized void configurationEvent(ConfigurationEvent arg0) {
        System.out.println(arg0.getPid() + " reconfiguration received");
        if (waitForEvent != null) {
            if (arg0.getPid().equals(waitForEvent)) {
                detected = true;
            }
        }
    }
    
    public ConfigurationMonitor(BundleContext bc) {
        reg = bc.registerService(ConfigurationListener.class.getName(), this, null);
    }
    
    public void stop() {
        reg.unregister();
        reg = null;
    }
    
    public void waitForEvent(String pid, String mes) {
        waitForEvent = pid;
        detected = false;
        long begin = System.currentTimeMillis();
        long duration = 0;
        while( ! detected) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Interrupted
            }
            long end = System.currentTimeMillis();
            duration = end - begin;
            if (duration > 5000) {
                Assert.fail(mes + " -> Timeout when waiting for a reconfiguration of " + pid);
            }
        }
        System.out.println("Reconfiguration detected of " + pid);
        waitForEvent = null;
        detected = false;
    }

}
