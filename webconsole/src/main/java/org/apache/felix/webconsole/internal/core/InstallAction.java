/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>InstallAction</code> TODO
 */
public class InstallAction extends BundleAction {

    public static final String NAME = "install";

    public static final String LABEL = "Install or Update";

    public static final String FIELD_STARTLEVEL = "bundlestartlevel";

    public static final String FIELD_START = "bundlestart";

    public static final String FIELD_BUNDLEFILE = "bundlefile";

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return LABEL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) {

        // get the uploaded data
        Map params = (Map) request.getAttribute(Util.ATTR_FILEUPLOAD);
        if (params == null) {
            return true;
        }

        FileItem startItem = this.getFileItem(params, FIELD_START, true);
        FileItem startLevelItem = this.getFileItem(params, FIELD_STARTLEVEL,
            true);
        FileItem bundleItem = this.getFileItem(params, FIELD_BUNDLEFILE, false);

        // don't care any more if not bundle item
        if (bundleItem == null || bundleItem.getSize() <= 0) {
            return true;
        }

        // default values
        boolean start = startItem != null; // don't care for the value, as long
        // it exists
        int startLevel = -1;
        String bundleLocation = "inputstream:";

        // convert the start level value
        if (startLevelItem != null) {
            try {
                startLevel = Integer.parseInt(startLevelItem.getString());
            } catch (NumberFormatException nfe) {
                getLog().log(
                    LogService.LOG_INFO,
                    "Cannot parse start level parameter " + startLevelItem
                        + " to a number, not setting start level");
            }
        }

        // write the bundle data to a temporary file to ease processing
        File tmpFile = null;
        try {
            // copy the data to a file for better processing
            tmpFile = File.createTempFile("install", ".tmp");
            bundleItem.write(tmpFile);
        } catch (Exception e) {
            getLog().log(LogService.LOG_ERROR,
                "Problem accessing uploaded bundle file", e);

            // remove the tmporary file
            if (tmpFile != null) {
                tmpFile.delete();
                tmpFile = null;
            }
        }

        // install or update the bundle now
        if (tmpFile != null) {
            bundleLocation = "inputstream:" + bundleItem.getName();
            installBundle(bundleLocation, tmpFile, startLevel, start);
        }

        return true;
    }

    private FileItem getFileItem(Map params, String name,
            boolean isFormField) {
        FileItem[] items = (FileItem[]) params.get(name);
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].isFormField() == isFormField) {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }

    private void installBundle(String location, File bundleFile,
            int startLevel, boolean start) {
        if (bundleFile != null) {

            // try to get the bundle name, fail if none
            String symbolicName = this.getSymbolicName(bundleFile);
            if (symbolicName == null) {
                bundleFile.delete();
                return;
            }

            // check for existing bundle first
            Bundle updateBundle = null;
            Bundle[] bundles = this.getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++) {
                if ((bundles[i].getLocation() != null && bundles[i].getLocation().equals(
                    location))
                    || (bundles[i].getSymbolicName() != null && bundles[i].getSymbolicName().equals(
                        symbolicName))) {
                    updateBundle = bundles[i];
                    break;
                }
            }

            if (updateBundle != null) {

                updateBackground(updateBundle, bundleFile);

            } else {

                installBackground(bundleFile, location, startLevel, start);

            }
        }
    }

    private String getSymbolicName(File bundleFile) {
        JarFile jar = null;
        try {
            jar = new JarFile(bundleFile);
            Manifest m = jar.getManifest();
            if (m != null) {
                return m.getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
            }
        } catch (IOException ioe) {
            getLog().log(LogService.LOG_WARNING,
                "Cannot extract symbolic name of bundle file " + bundleFile,
                ioe);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // fall back to "not found"
        return null;
    }

    private void installBackground(final File bundleFile,
            final String location, final int startlevel, final boolean doStart) {

        Thread t = new InstallHelper(this, "Background Install " + bundleFile,
            bundleFile) {

            protected void doRun(InputStream bundleStream)
                    throws BundleException {
                Bundle bundle = getBundleContext().installBundle(location,
                    bundleStream);

                StartLevel sl = getStartLevel();
                if (sl != null) {
                    sl.setBundleStartLevel(bundle, startlevel);
                }

                if (doStart) {
                    bundle.start();
                }
            }
        };

        t.start();
    }

    private void updateBackground(final Bundle bundle, final File bundleFile) {
        Thread t = new InstallHelper(this, "Background Update"
            + bundle.getSymbolicName() + " (" + bundle.getBundleId() + ")",
            bundleFile) {

            protected void doRun(InputStream bundleStream)
                    throws BundleException {
                bundle.update(bundleStream);
            }
        };

        t.start();
    }

    private static abstract class InstallHelper extends Thread {

        private final InstallAction installAction;

        private final File bundleFile;

        InstallHelper(InstallAction installAction, String name, File bundleFile) {
            super(name);
            setDaemon(true);

            this.installAction = installAction;
            this.bundleFile = bundleFile;
        }

        protected abstract void doRun(InputStream bundleStream)
                throws BundleException;

        public void run() {
            // wait some time for the request to settle
            try {
                sleep(500L);
            } catch (InterruptedException ie) {
                // don't care
            }

            // now deploy the resolved bundles
            InputStream bundleStream = null;
            try {
                bundleStream = new FileInputStream(bundleFile);
                doRun(bundleStream);
            } catch (IOException ioe) {
                installAction.getLog().log(LogService.LOG_ERROR,
                    "Cannot install or update bundle from " + bundleFile, ioe);
            } catch (BundleException be) {
                installAction.getLog().log(LogService.LOG_ERROR,
                    "Cannot install or update bundle from " + bundleFile, be);
            } finally {
                if (bundleStream != null) {
                    try {
                        bundleStream.close();
                    } catch (IOException ignore) {
                    }
                }
                bundleFile.delete();
            }
        }
    }
}
