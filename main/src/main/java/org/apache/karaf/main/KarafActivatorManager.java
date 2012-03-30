package org.apache.karaf.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.main.util.BootstrapLogManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.launch.Framework;

public class KarafActivatorManager {
    public static final String KARAF_ACTIVATOR = "Karaf-Activator";
    Logger LOG = Logger.getLogger(this.getClass().getName());

    private List<BundleActivator> karafActivators = new ArrayList<BundleActivator>();
    private final ClassLoader classLoader;
    private final Framework framework;
    
    public KarafActivatorManager(ClassLoader classLoader, Framework framework) {
        this.classLoader = classLoader;
        this.framework = framework;
        LOG.addHandler(BootstrapLogManager.getDefaultHandler());
    }

    void startKarafActivators() throws IOException {
        Enumeration<URL> urls = classLoader.getResources("META-INF/MANIFEST.MF");
        while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String className = null;
            InputStream is = url.openStream();
            try {
                Manifest mf = new Manifest(is);
                className = mf.getMainAttributes().getValue(KARAF_ACTIVATOR);
                if (className != null) {
                    BundleActivator activator = (BundleActivator) classLoader.loadClass(className).newInstance();
                    activator.start(framework.getBundleContext());
                    karafActivators.add(activator);
                }
            } catch (Throwable e) {
                if (className != null) {
                    System.err.println("Error starting karaf activator " + className + ": " + e.getMessage());
                    LOG.log(Level.WARNING, "Error starting karaf activator " + className + " from url " + url, e);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    void stopKarafActivators() {
        for (BundleActivator activator : karafActivators) {
            try {
                activator.stop(framework.getBundleContext());
            } catch (Throwable e) {
                LOG.log(Level.WARNING, "Error stopping karaf activator " + activator.getClass().getName(), e);
            }
        }
    }
}
