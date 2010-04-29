package org.apache.felix.ipojo.tests.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.example.service.impl.HostImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;



@RunWith( JUnit4TestRunner.class )
public class ExternalHandlerTest {

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
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.api").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.whiteboard").version(asInProject())
                    )
                );
        return opt;
    }

    @Test
    public void createAHost() throws Exception {
        PrimitiveComponentType type = createAWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat (ci.getState(), is (ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat (hd, is (notNullValue()));
    }

    @Test
    public void createDoubleHost() throws Exception {
        PrimitiveComponentType type = createASecondWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat (ci.getState(), is (ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat (hd, is (notNullValue()));
    }

    private PrimitiveComponentType createAWhiteboardHost() {
        return new PrimitiveComponentType()
        .setBundleContext(context)
        .setClassName(HostImpl.class.getName())
        .addHandler(new Whiteboard()
            .onArrival("arrival")
            .onDeparture("departure")
            .setFilter("(foo=foo)")
         );
    }

    private PrimitiveComponentType createASecondWhiteboardHost() {
        return new PrimitiveComponentType()
        .setBundleContext(context)
        .setClassName(HostImpl.class.getName())
        .addHandler(new Whiteboard()
            .onArrival("arrival")
            .onDeparture("departure")
            .setFilter("(foo=foo)")
         )
         .addHandler(new Whiteboard()
         .onArrival("arrival")
         .onDeparture("departure")
         .setFilter("(foo=bar)")
         .onModification("modification")
      );
    }

}
