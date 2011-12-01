package org.apache.karaf.shell.services;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ListServicesTest {

    public ListServicesTest() {
        
    }
    
    ServiceReference<?> createServiceRef(String ... objectClass) {
        ServiceReference<?> serviceRef = createMock(ServiceReference.class);
        expect(serviceRef.getProperty("objectClass")).andReturn(objectClass).atLeastOnce();
        expect(serviceRef.getPropertyKeys()).andReturn(new String[]{"objectClass"});
        replay(serviceRef);
        return serviceRef;
    }
    
    Bundle createBundle(long id, String name, ServiceReference<?>[] providedServices, ServiceReference<?>[] usedServices) {
        Bundle bundle = createMock(Bundle.class);
        expect(bundle.getRegisteredServices()).andReturn(providedServices);
        expect(bundle.getServicesInUse()).andReturn(usedServices);
        expect(bundle.getBundleId()).andReturn(id);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_NAME, name);
        expect(bundle.getHeaders()).andReturn(headers).atLeastOnce();
        replay(bundle);
        return bundle;
    }
    
    private Bundle[] createBundles() {
        Bundle bundle1 = createBundle(1, "ABundle", new ServiceReference<?>[]{createServiceRef("org.example.MyService")}, new ServiceReference<?>[]{});
        Bundle bundle2 = createBundle(2, "AnotherBundle", new ServiceReference<?>[]{}, new ServiceReference<?>[]{createServiceRef("org.example.MyService")});
        return new Bundle[] { bundle1, bundle2 };
    }
    
    private BundleContext createBundleContext() {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle[] bundles = createBundles();
        expect(bundleContext.getBundles()).andReturn(bundles);
        expect(bundleContext.getBundle(1)).andReturn(bundles[0]);
        expect(bundleContext.getBundle(2)).andReturn(bundles[1]);
        replay(bundleContext);
        return bundleContext;
    }

    
    @Test
    public void listAllShort() throws Exception {
        ListServices listServices = new ListServices();
        listServices.setBundleContext(createBundleContext());
        listServices.doExecute();
    }

    
    @Test
    public void listAllLong() throws Exception {
        ListServices listServices = new ListServices();
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.setBundleContext(createBundleContext());
        listServices.doExecute();
    }
    
    @Test
    public void listAllLongServiceUse() throws Exception {
        ListServices listServices = new ListServices();
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.inUse = true;
        listServices.setBundleContext(createBundleContext());
        listServices.doExecute();
    }


}
