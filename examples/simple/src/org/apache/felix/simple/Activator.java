package org.apache.felix.simple;

import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.felix.simple.embedded.Embedded;

/**
 * A very simple bundle that prints out a message when it is started and
 * stopped; it includes an embedded JAR file which is used on its internal
 * class path. This bundle is merely a "hello world" example; it does not
 * implement any services nor does it offer any configurable properties.
**/
public class Activator implements BundleActivator
{
    private BundleContext context = null;

    public native String foo();

    public void start(BundleContext context) throws Exception
    {
        System.out.println("Simple bundle " + context.getBundle().getBundleId()
            + " has started.");

        this.context = context;

        // Get the OS and processor properties.
        String os =
            context.getProperty("org.osgi.framework.os.name").toLowerCase();
        String processor =
            context.getProperty("org.osgi.framework.processor").toLowerCase();

        // Load library if correct platform.
        if (os.equals("linux") && processor.endsWith("86"))
        {
            try {
                System.loadLibrary("foo");
            } catch (Exception ex) {
                System.out.println("No library: " + ex);
                ex.printStackTrace();
            }
            System.out.println("From native: " + foo());
        }

        // Create class from embedded JAR file.
        Embedded embedded = new Embedded();
        embedded.sayHello();

        // Access dynamically imported servlet class.
        try {
            System.out.println("Class name = " + javax.servlet.http.HttpSession.class);
        } catch (Throwable ex) {
            System.out.println("The 'javax.servlet.http' package is not available.");
        }
        try {
            System.out.println("Class name = " + Servlet.class);
        } catch (Throwable ex) {
            System.out.println("The 'javax.servlet' package is not available.");
        }
    }

    public void stop(BundleContext context)
    {
        System.out.println("Simple bundle " + context.getBundle().getBundleId()
            + " has stopped.");
    }
}
