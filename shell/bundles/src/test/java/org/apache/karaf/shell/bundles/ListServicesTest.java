package org.apache.karaf.shell.bundles;
import java.util.Arrays;

import org.junit.Test;

public class ListServicesTest {

    public ListServicesTest() {
        
    }
    
    @Test
    public void listAllShort() throws Exception {
        System.out.println("listAllShort");
        ListBundleServices listServices = new ListBundleServices();
        listServices.setBundleContext(new TestBundleFactory().createBundleContext());
        listServices.doExecute();
    }

    
    @Test
    public void listAllLong() throws Exception {
        System.out.println("listAllLong");
        ListBundleServices listServices = new ListBundleServices();
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.setBundleContext(new TestBundleFactory().createBundleContext());
        listServices.doExecute();
    }

    @Test
    public void listAllLongServiceUse() throws Exception {
        System.out.println("listAllLongServicesUse");
        ListBundleServices listServices = new ListBundleServices();
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.inUse = true;
        listServices.setBundleContext(new TestBundleFactory().createBundleContext());
        listServices.doExecute();
    }


}
