package org.apache.felix.ipojo.tests.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.SingletonComponentType;
import org.example.service.Foo;
import org.example.service.impl.FooImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;



@RunWith( JUnit4TestRunner.class )
public class PrimitiveComponentTest {

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
    public static Option[] configure() {
        Option[] opt =  options(
                felix(),
                equinox(),
                provision(
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.api").version(asInProject())
                    )
                );
        return opt;
    }

    @Test
    public void createAServiceProvider() throws Exception {
        assertThat( context, is( notNullValue() ) );
        ComponentInstance ci = null;

        PrimitiveComponentType type = createAProvider();
        ci = type.createInstance();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojo.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));

    }

    @Test
    public void killTheFactory() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        assertThat( context, is( notNullValue() ) );
        ComponentInstance ci = null;

        PrimitiveComponentType type = createAProvider();
        ci = type.createInstance();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojo.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));
        type.stop();
        assertThat("Ci is disposed", ci.getState(),
                is(ComponentInstance.DISPOSED));
        ref = ipojo.getServiceReferenceByName(Foo.class.getName(), ci
                .getInstanceName());
        assertThat(ref, is(nullValue()));

    }

    @Test
    public void createAServiceCons() throws Exception {
        assertThat( context, is( notNullValue() ) );
        ComponentInstance ci = null;

        PrimitiveComponentType type = createAConsumer();
        ci = type.createInstance();
        assertThat("Ci is invalid", ci.getState(),
                is(ComponentInstance.INVALID));

    }

    @Test
    public void createBoth() throws Exception {
        ComponentInstance cons = createAConsumer().createInstance();
        // cons is invalid
        assertThat("cons is invalid", cons.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().createInstance();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createTwoCons() throws Exception {
        ComponentInstance cons1 = createAConsumer().createInstance();
        // cons is invalid
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().createInstance();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons1.getState(), is(ComponentInstance.VALID));

        ComponentInstance cons2 = createAnOptionalConsumer().createInstance();

        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));

        prov.stop();
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));
        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));
    }

    private PrimitiveComponentType createAProvider() {
        return new PrimitiveComponentType()
        .setBundleContext(context)
        .setClassName(FooImpl.class.getName())
        .addService(new Service()); // Provide the FooService
    }

    private PrimitiveComponentType createAConsumer() {
        return new SingletonComponentType()
        .setBundleContext(context)
        .setClassName(org.example.service.impl.MyComponentImpl.class.getName())
        .addDependency(new Dependency().setField("myFoo"))
        .setValidateMethod("start");
    }

    private PrimitiveComponentType createAnOptionalConsumer() {
        return new SingletonComponentType()
        .setBundleContext(context)
        .setClassName(org.example.service.impl.MyComponentImpl.class.getName())
        .addDependency(new Dependency().setField("myFoo").setOptional(true))
        .setValidateMethod("start");
    }



}
