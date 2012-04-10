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
package org.apache.karaf.diagnostic.core.internal;

import java.io.File;
import java.util.List;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.diagnostic.core.DiagnosticDumpMBean;
import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.diagnostic.core.common.DirectoryDumpDestination;
import org.apache.karaf.diagnostic.core.common.ZipDumpDestination;

/**
 * Implementation of diagnostic MBean.
 */
public class DiagnosticDump extends StandardMBean implements 
    DiagnosticDumpMBean {

    /**
     * Dump providers.
     */
    private List<DumpProvider> providers;

    /**
     * Creates new diagnostic mbean.
     * 
     * @throws NotCompliantMBeanException
     */
    public DiagnosticDump() throws NotCompliantMBeanException {
        super(DiagnosticDumpMBean.class);
    }

    /**
     * Creates dump witch given name
     * 
     * @param name Name of the dump.
     */
    public void createDump(String name) throws Exception {
        createDump(false, name);
    }

    /**
     * {@inheritDoc}
     */
    public void createDump(boolean directory, String name) throws Exception {
        File target = new File(name);

        DumpDestination destination;
        if (directory) {
            destination = new DirectoryDumpDestination(target);
        } else {
            destination = new ZipDumpDestination(target);
        }

        for (DumpProvider provider : providers) {
            provider.createDump(destination);
        }

        destination.save();
    }

    /**
     * Sets dump providers.
     * 
     * @param providers Dump providers. 
     */
    public void setProviders(List<DumpProvider> providers) {
        this.providers = providers;
    }

}
