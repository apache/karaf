package org.apache.felix.ipojo.transaction.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

import java.io.File;
import java.io.InputStream;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.transaction.test.component.FooDelegator;
import org.apache.felix.ipojo.transaction.test.component.FooImpl;
import org.apache.felix.ipojo.transaction.test.service.CheckService;
import org.apache.felix.ipojo.transaction.test.service.Foo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith( JUnit4TestRunner.class )
public class TestInstallation {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    public static final File ROOT = new File("target/tmp");
    public static final File TEST = new File("src/test/resources");


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
    public static Option[] configure() {
        ROOT.mkdirs();

        InputStream service = TinyBundles.newBundle()
            .add(CheckService.class)
            .add(Foo.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"Service")
            .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
            .build();

//        try {
//            StreamUtils.copy(service, new FileOutputStream(new File(ROOT, "service.jar")));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        InputStream fooimpl = TinyBundles.newBundle()
            .add(FooImpl.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"Foo Provider")
            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
            .build( withiPOJO(new File(ROOT, "FooImpl.jar"), new File(TEST, "foo.xml"))  );

        InputStream test = TinyBundles.newBundle()
            .add(FooDelegator.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"RequiredTransactionPropagation")
            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service, javax.transaction")
            .build( withiPOJO(new File(ROOT, "requires.jar"), new File(TEST, "requires.xml"))  );


        Option[] opt =  options(
                provision(
                        mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.transaction").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.transaction").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject()
                ),
                provision(
                        service,
                        fooimpl,
                        test
                    )
                ,
                new Customizer() {
                    @Override
                    public InputStream customizeTestProbe( InputStream testProbe )
                    {
                       return TinyBundles.modifyBundle(testProbe)
                           .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
                           .build();
                    }
                });

        return opt;
    }

    @Test
    public void install() throws NotSupportedException, SystemException, IllegalStateException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException, InvalidSyntaxException {
        Bundle[] bundles = context.getBundles();
        for (Bundle b : bundles) {
            Assert.assertTrue(b.getSymbolicName(), b.getState() == Bundle.ACTIVE);
        }

        // Transaction Service available
        osgi.isServiceAvailable(TransactionManager.class.getName());
        TransactionManager tm = (TransactionManager) osgi.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Assert.assertNotNull(tm.getTransaction());
        tm.commit();

        tm.begin();
        Assert.assertNotNull(tm.getTransaction());
        tm.rollback();

        // Handler exposed
        ServiceReference ref = osgi.getServiceReference(HandlerFactory.class.getName(), "(&(handler.name=transaction)(handler.namespace=org.apache.felix.ipojo.transaction))");
        Assert.assertNotNull(ref);

        // Create an install of the components
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requires-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference[] refs = context.getAllServiceReferences(CheckService.class.getName(), "(instance.name=" + under.getInstanceName() +")");

//        ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(refs);

        ((CheckService) osgi.getServiceObject(refs[0])).doSomethingGood();
    }

}
