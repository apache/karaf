package org.apache.felix.ipojo.tests.core;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.service.MyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith( JUnit4TestRunner.class )
public class ManifestLoggerInfoTest {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    private LogReaderService log;

    @Before
    public void init() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);

        log = (LogReaderService) osgi.getServiceObject(LogReaderService.class.getName(), null);
        if (log == null) {
            throw new RuntimeException("No Log Service !");
        }

        LogService logs = (LogService) osgi.getServiceObject(LogService.class.getName(), null);
        logs.log(LogService.LOG_WARNING, "Ready");
    }

    @After
    public void stop() {
        ipojo.dispose();
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {

        File tmp = new File("target/tmp");
        tmp.mkdirs();

        Option[] opt =  options(
                felix(),
//                equinox(),
                provision(
                        // Runtime.
                        mavenBundle().groupId( "org.apache.felix" ).artifactId( "org.apache.felix.log" ).version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject()
                        ),
                provision(
                        newBundle()
                            .add( MyService.class )
                            .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterface")
                            .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build()
                    ),
               provision(
                       // Component
                        newBundle()
                            .add(MyComponent.class)
                            .set(Constants.BUNDLE_SYMBOLICNAME,"MyComponent")
                            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .set("ipojo-log-level", "info")
                            .build( withiPOJO(new File(tmp, "provider-with-level-in-manifest.jar"), new File("component.xml")))
                            )
                );
        return opt;
    }

    @Test
    @Ignore //TODO Why we have a classloading issue here ?
    public void testMessages() throws InterruptedException, InvalidSyntaxException {
        Bundle bundle = osgi.getBundle("MyComponent");
        Assert.assertNotNull(bundle);
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());

        ServiceReference r = ipojo.getServiceReferenceByName(Architecture.class.getName(), "org.apache.felix.ipojo.tests.core.component.MyComponent-0");
        Assert.assertNotNull(r);
        System.out.println(((Architecture) osgi.getServiceObject(r)).getInstanceDescription().getDescription());

        ServiceReference[] refs = context.getAllServiceReferences(null, null);
        for (ServiceReference ref : refs) {
            System.out.println(ref.getBundle().getBundleId() + " -> " + Arrays.asList((String[]) ref.getProperty(Constants.OBJECTCLASS)));
        }



   //     Assert.assertNotNull(osgi.getServiceObject(MyService.class.getName(), null));

//        osgi.waitForService("org.apache.felix.ipojo.tests.core.service.MyService", null, 5000);
        List<String> messages = getMessages(log.getLog());
        System.out.println(messages);
        Assert.assertTrue(messages.contains("Ready"));
        Assert.assertTrue(messages.contains("[INFO] org.apache.felix.ipojo.tests.core.component.MyComponent : Instance org.apache.felix.ipojo.tests.core.component.MyComponent-0 from factory org.apache.felix.ipojo.tests.core.component.MyComponent created"));
        Assert.assertTrue(messages.contains("[INFO] org.apache.felix.ipojo.tests.core.component.MyComponent : New factory created : org.apache.felix.ipojo.tests.core.component.MyComponent"));
    }

   private List<String> getMessages(Enumeration<LogEntry> log2) {
       List<String> list = new ArrayList<String>();
       while (log2.hasMoreElements()) {
           LogEntry entry = (LogEntry) log2.nextElement();
           list.add(entry.getMessage());
       }
       return list;
    }


}
