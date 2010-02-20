package org.apache.felix.ipojo.tests.core;

import static org.apache.felix.ipojo.tinybundles.BundleAsiPOJO.asiPOJOBundle;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.service.MyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

@RunWith( JUnit4TestRunner.class )
public class SystemLoggerInfoTest {

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
                equinox(),
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId( "org.ops4j.pax.swissbox" ).artifactId( "pax-swissbox-tinybundles" ).version(asInProject()),
                        mavenBundle().groupId( "org.apache.felix" ).artifactId( "org.apache.felix.log" ).version(asInProject())
                        ),
                provision(
                        newBundle()
                            .add( MyService.class )
                            .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterface")
                            .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build( withBnd() )
                    ),
               provision(
                       // Component
                        newBundle()
                            .add(MyComponent.class)
                            .set(Constants.BUNDLE_SYMBOLICNAME,"MyComponent")
                            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build( asiPOJOBundle(new File(tmp, "provider.jar"), new File("component.xml")))
                            ),
                systemProperty( "ipojo.log.level" ).value( "info" )
                );
        return opt;
    }
    
    @Test
    public void testMessages() throws InterruptedException {
        List<String> messages = getMessages(log.getLog());
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
