package org.apache.karaf.bundle.command;
import java.util.Arrays;

import org.apache.karaf.bundle.core.internal.BundleSelectorImpl;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ListServicesTest {

    private ListBundleServices listServices;

    public ListServicesTest() {
        listServices = new ListBundleServices();
        BundleContext bundleContext = new TestBundleFactory().createBundleContext();
        listServices.setBundleContext(bundleContext);
        listServices.setBundleSelector(new BundleSelectorImpl(bundleContext));
    }
    
    @Test
    public void listAllShort() throws Exception {
        System.out.println("listAllShort");
        listServices.doExecute();
    }

    
    @Test
    public void listAllLong() throws Exception {
        System.out.println("listAllLong");
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.doExecute();
    }

    @Test
    public void listAllLongServiceUse() throws Exception {
        System.out.println("listAllLongServicesUse");
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.inUse = true;
        listServices.doExecute();
    }


}
