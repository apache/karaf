package org.apache.karaf.service.command;

import org.junit.Test;

public class GetServiceTest {

    @Test
    public void GetService() throws Exception {
        GetService getServices = new GetService();
        getServices.setBundleContext(new TestBundleFactory().createBundleContext());
        getServices.setObjectClass("org.example.MyService");
        getServices.execute();
    }
}
