/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.example.extenderbased.host;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import org.osgi.framework.*;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.main.AutoActivator;

/**
 * The activator of the host application bundle. The activator creates the
 * main application <tt>JFrame</tt> and starts tracking <tt>SimpleShape</tt>
 * extensions. All activity is performed on the Swing event thread to avoid
 * synchronization and repainting issues. Closing the application window
 * will result in <tt>Bundle.stop()</tt> being called on the system bundle,
 * which will cause the framework to shutdown and the JVM to exit.
 * <p>
 * This class also provides a static <tt>main()</tt> method so that it can be
 * run as a stand-alone host application. In such a scenario, the application
 * creates its own embedded Felix framework instance and interacts with the
 * internal extensions to providing drawing functionality. To successfully
 * launch the stand-alone application, it must be run from this bundle's
 * installation directory using "<tt>java -jar</tt>".
**/
public class Activator implements BundleActivator
{
    private DrawingFrame m_frame = null;
    private ShapeTracker m_shapetracker = null;

    /**
     * Displays the applications window and starts extension tracking;
     * everything is done on the Swing event thread to avoid synchronization
     * and repainting issues.
     * @param context The context of the bundle.
    **/
    public void start(final BundleContext context)
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            // This creates of the application window.
            public void run()
            {
                m_frame = new DrawingFrame();
                m_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                m_frame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent evt)
                    {
                        try
                        {
                            context.getBundle(0).stop();
                        }
                        catch (BundleException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                });

                m_frame.setVisible(true);

                m_shapetracker = new ShapeTracker(context, m_frame);
                m_shapetracker.open();
            }
        });
    }

    /**
     * Stops extension tracking and disposes of the application window.
     * @param context The context of the bundle.
    **/
    public void stop(BundleContext context)
    {
        Runnable runner = new Runnable() {
            // This disposes of the application window.
            public void run()
            {
                m_shapetracker.close();
                m_frame.setVisible(false);
                m_frame.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            runner.run();
        }
        else
        {
            try
            {
                javax.swing.SwingUtilities.invokeAndWait(runner);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Enables the bundle to run as a stand-alone application. When this
     * static <tt>main()</tt> method is invoked, the application creates
     * its own embedded Felix framework instance and interacts with the
     * internal extensions to provide drawing functionality. To successfully
     * launch as a stand-alone application, this method should be invoked from
     * the bundle's installation directory using "<tt>java -jar</tt>".
     * @param argv The command-line arguments.
     * @throws Exception If anything goes wrong.
    **/
    public static void main(String[] argv) throws Exception
    {
        // Create a temporary bundle cache directory and
        // make sure to clean it up on exit.
        final File cachedir = File.createTempFile("felix.example.extenderbased", null);
        cachedir.delete();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run()
            {
                deleteFileOrDir(cachedir);
            }
        });

        Map configMap = new StringMap(false);
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
            "org.apache.felix.example.extenderbased.host.extension; version=1.0.0");
        configMap.put(AutoActivator.AUTO_START_PROP + ".1",
            "file:../extenderbased.circle/target/extenderbased.circle-1.0.0.jar " +
            "file:../extenderbased.square/target/extenderbased.square-1.0.0.jar " +
            "file:../extenderbased.triangle/target/extenderbased.triangle-1.0.0.jar");
        configMap.put(FelixConstants.LOG_LEVEL_PROP, "4");
        configMap.put(Constants.FRAMEWORK_STORAGE, cachedir.getAbsolutePath());

        // Create list to hold custom framework activators.
        List list = new ArrayList();
        // Add activator to process auto-start/install properties.
        list.add(new AutoActivator(configMap));
        // Add our own activator.
        list.add(new Activator());
        // Add our custom framework activators to the configuration map.
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

        try
        {
            // Now create an instance of the framework.
            Felix felix = new Felix(configMap);
            felix.start();
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Utility method used to delete the profile directory when run as
     * a stand-alone application.
     * @param file The file to recursively delete.
    **/
    private static void deleteFileOrDir(File file)
    {
        if (file.isDirectory())
        {
            File[] childs = file.listFiles();
            for (int i = 0;i < childs.length;i++)
            {
                deleteFileOrDir(childs[i]);
            }
        }
        file.delete();
    }
}