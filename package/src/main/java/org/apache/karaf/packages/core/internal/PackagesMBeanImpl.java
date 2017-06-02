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
package org.apache.karaf.packages.core.internal;

import java.util.List;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.KeyAlreadyExistsException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.packages.core.PackageRequirement;
import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.packages.core.PackageVersion;
import org.apache.karaf.packages.core.PackagesMBean;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Packages MBean.
 */
public class PackagesMBeanImpl extends StandardMBean implements PackagesMBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(PackagesMBeanImpl.class);

    private final PackageService packageService;

    public PackagesMBeanImpl(PackageService packageService) throws NotCompliantMBeanException {
        super(PackagesMBean.class);
        this.packageService = packageService;
    }

    public TabularData getExports() {
        try {
            String[] names = new String[] {"Name", "Version", "ID", "Bundle Name"};
            CompositeType bundleType = new CompositeType("PackageExport", "Exported packages", names,
                                                         new String[] {"Package name", "Version of the Package",
                                                                       "ID of the Bundle", "Bundle symbolic name"},
                                                         new OpenType[] {SimpleType.STRING, SimpleType.STRING,
                                                                         SimpleType.LONG, SimpleType.STRING});
            TabularType tableType = new TabularType("PackageExports", "Exported packages", bundleType,
                                                    new String[] {"Name", "Version", "ID"});
            TabularData table = new TabularDataSupport(tableType);

            List<PackageVersion> exports = packageService.getExports();

            for (PackageVersion export : exports) {
                for (Bundle bundle : export.getBundles()) {
                    Object[] data = new Object[] {
                                         export.getPackageName(),
                                         export.getVersion().toString(), 
                                         bundle.getBundleId(),
                                         bundle.getSymbolicName()};
                    CompositeData comp = new CompositeDataSupport(bundleType, names, data);
                    LOGGER.debug("Adding CompositeDataSupport {}", comp);
                    table.put(comp);
                }
            }
            return table;
        } catch (RuntimeException e) {
            // To avoid the exception gets swallowed by jmx
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (OpenDataException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public TabularData getImports() {
        try {
            String[] names = new String[] {"PackageName", "Filter", "Optional", "ID", "Bundle Name", "Resolvable"};
            CompositeType bundleType = new CompositeType("PackageImports", "Imported packages", 
                                                         names,
                                                         names,
                                                         new OpenType[] {SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN,
                                                                         SimpleType.LONG, SimpleType.STRING, SimpleType.BOOLEAN});
            TabularType tableType = new TabularType("PackageImports", "Imported packages", bundleType,
                                                    new String[] {"Filter", "ID"});
            TabularData table = new TabularDataSupport(tableType);

            List<PackageRequirement> imports = packageService.getImports();

            for (PackageRequirement req : imports) {
                Object[] data = new Object[] {
                                         req.getPackageName(),
                                         req.getFilter(),
                                         req.isOptional(), 
                                         req.getBundle().getBundleId(),
                                         req.getBundle().getSymbolicName(),
                                         req.isResolveable()};
                CompositeData comp = new CompositeDataSupport(bundleType, names, data);
                try {
                    table.put(comp);
                }catch (KeyAlreadyExistsException e) {
                    throw new RuntimeException("Id: " + req.getBundle().getBundleId() + ", filter: " + req.getFilter(), e);
                }
             }
            return table;
        } catch (RuntimeException e) {
            // To avoid the exception gets swallowed by jmx
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (OpenDataException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

	@Override
	public List<String> getExports(long bundleId) {
		return packageService.getExports(bundleId);
		
	}

	@Override
	public List<String> getImports(long bundleId) {
		return packageService.getImports(bundleId);
	}

}
