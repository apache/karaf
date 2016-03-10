/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.karaf.diagnostic.management.internal;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.diagnostic.core.Dump;
import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.management.DiagnosticDumpMBean;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of diagnostic mbean.
 */
public class DiagnosticDumpMBeanImpl extends StandardMBean implements DiagnosticDumpMBean {

    /**
     * Dump providers.
     */
    private BundleContext bundleContext;

    private SimpleDateFormat dumpFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

    private final static Logger LOGGER = LoggerFactory.getLogger(DiagnosticDumpMBeanImpl.class);

    /**
     * Create new diagnostic MBean.
     *
     * @throws NotCompliantMBeanException If the MBean is not valid.
     */
    public DiagnosticDumpMBeanImpl() throws NotCompliantMBeanException {
        super(DiagnosticDumpMBean.class);
    }

    /**
     * Create dump witch given name.
     *
     * @param name Name of the dump.
     */
    public void createDump(String name) {
        createDump(false, name, false, false);
    }

    /**
     * {@inheritDoc}
     */
    public void createDump(boolean directory, String name, boolean noThreadDump, boolean noHeapDump) {
        if (name == null || name.trim().length() == 0) {
            name = dumpFormat.format(new Date());
            if (!directory) {
                name += ".zip";
            }
        }
        File target = new File(name);

        DumpDestination destination;
        if (directory) {
            destination = Dump.directory(target);
        } else {
            destination = Dump.zip(target);
        }

        Dump.dump(bundleContext, destination, noThreadDump, noHeapDump);
        LOGGER.info("Created dump " + destination.toString());
    }

    /**
     * Set the bundle context.
     *
     * @param bundleContext The bundle context to use in the MBean.
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
