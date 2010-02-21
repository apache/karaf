package org.apache.felix.ipojo.test.optional;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.File;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.optional.MyComponent;
import org.apache.felix.ipojo.test.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.helpers.OSGiHelper;
import org.apache.felix.ipojo.tinybundles.BundleAsiPOJO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;

import aQute.lib.osgi.Constants;


/**
 * Reproduces FELIX-2093
 * iPOJO doesn't always use the correct class loader to load nullable object.
 */
@RunWith( JUnit4TestRunner.class )
public class NullableTransitiveClassloadingTest {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    @Before
    public void init() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
    }

    @After
    public void stop() {
        ipojo.dispose();
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure()  {

        File tmp = new File("target/tmp");
        tmp.mkdirs();


        Option[] opt =  options(
                felix(),
                equinox(),
                knopflerfish(),
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.test.helpers").version(asInProject())
                        ),
                provision(
                        TinyBundles.newBundle()
                            .add(MyComponent.class)
                            .set(Constants.IMPORT_PACKAGE, "*")
                            .build(BundleAsiPOJO.asiPOJOBundle(new File("src/main/resources/metadata.xml"))
                            )
                            
                ));

        return opt;
    }

    @Test
    public void testCreation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Factory factory = ipojo.getFactory("optional-log-cons");
        ComponentInstance ci = factory.createComponentInstance(null);
        
        ci.dispose();
    }

}
