/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.region.persist.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.karaf.region.persist.RegionsPersistence;
import org.apache.karaf.region.persist.internal.model.FilterAttributeType;
import org.apache.karaf.region.persist.internal.model.FilterBundleType;
import org.apache.karaf.region.persist.internal.model.FilterNamespaceType;
import org.apache.karaf.region.persist.internal.model.FilterPackageType;
import org.apache.karaf.region.persist.internal.model.FilterType;
import org.apache.karaf.region.persist.internal.model.RegionBundleType;
import org.apache.karaf.region.persist.internal.model.RegionType;
import org.apache.karaf.region.persist.internal.model.RegionsType;
import org.apache.karaf.region.persist.internal.util.ManifestHeaderProcessor;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionsPersistenceImpl implements RegionsPersistence {

    private static final Logger log = LoggerFactory.getLogger(RegionsPersistenceImpl.class);

    private JAXBContext jaxbContext;
    private RegionDigraph regionDigraph;
    private Region kernel;
    private Bundle framework;

    public RegionsPersistenceImpl(RegionDigraph regionDigraph, Bundle framework) throws JAXBException, BundleException, IOException, InvalidSyntaxException {
        log.info("Loading region digraph persistence");
        this.framework = framework;
        this.regionDigraph = regionDigraph;
        kernel = regionDigraph.getRegion(0);
        jaxbContext = JAXBContext.newInstance(RegionsType.class);
        load();
    }

    @Override
    public void install(Bundle b, String regionName) throws BundleException {
        Region region = regionDigraph.getRegion(regionName);
        if (region == null) {
            region = regionDigraph.createRegion(regionName);
        }
        kernel.removeBundle(b);
        region.addBundle(b);
    }

    void save(RegionsType regionsType, Writer out) throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(regionsType, out);
    }

    void load() throws IOException, BundleException, JAXBException, InvalidSyntaxException {
        if (this.regionDigraph.getRegions().size() <= 1) {
            File base = new File(System.getProperty("karaf.base"));
            File regionsConfig = new File(new File(base, "etc"), "regions-config.xml");
            if (regionsConfig.exists()) {
                log.info("initializing region digraph from etc/regions-config.xml");
                Reader in = new FileReader(regionsConfig);
                try {
                        load(this.regionDigraph, in);
                    } finally {
                        in.close();
                    }
            } else {
                log.info("no regions config file");
            }
        }

    }

    void  load(RegionDigraph regionDigraph, Reader in) throws JAXBException, BundleException, InvalidSyntaxException {
        RegionsType regionsType = load(in);
        load(regionsType, regionDigraph);
    }

    RegionsType load(Reader in) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (RegionsType) unmarshaller.unmarshal(in);
    }

    void load(RegionsType regionsType, RegionDigraph regionDigraph) throws BundleException, InvalidSyntaxException {
        BundleContext frameworkContext = framework.getBundleContext();
        for (RegionType regionType: regionsType.getRegion()) {
            String name = regionType.getName();
            log.debug("Creating region: " + name);
            Region region = regionDigraph.createRegion(name);
            for (RegionBundleType bundleType: regionType.getBundle()) {
                if (bundleType.getId() != null) {
                    region.addBundle(bundleType.getId());
                } else {
                    Bundle b = frameworkContext.getBundle(bundleType.getLocation());
                    region.addBundle(b);
                }
            }
        }
        for (FilterType filterType: regionsType.getFilter()) {
            Region from = regionDigraph.getRegion(filterType.getFrom());
            Region to = regionDigraph.getRegion(filterType.getTo());
            log.debug("Creating filter between " + from.getName() + " to " + to.getName());
            RegionFilterBuilder builder = regionDigraph.createRegionFilterBuilder();
            for (FilterBundleType bundleType: filterType.getBundle()) {
                String symbolicName = bundleType.getSymbolicName();
                String version = bundleType.getVersion();
                if (bundleType.getId() != null) {
                    Bundle b = frameworkContext.getBundle(bundleType.getId());
                    symbolicName = b.getSymbolicName();
                    version = b.getVersion().toString();
                }
                String namespace = BundleRevision.BUNDLE_NAMESPACE;
                List<FilterAttributeType> attributeTypes = bundleType.getAttribute();
                buildFilter(symbolicName, version, namespace, attributeTypes, builder);
            }
            for (FilterPackageType packageType: filterType.getPackage()) {
                String packageName = packageType.getName();
                String version = packageType.getVersion();
                String namespace = BundleRevision.PACKAGE_NAMESPACE;
                List<FilterAttributeType> attributeTypes = packageType.getAttribute();
                buildFilter(packageName, version, namespace, attributeTypes, builder);
            }
            if (to == kernel) {
                //add framework exports
                BundleRevision rev = framework.adapt(BundleRevision.class);
                List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleCapability cap : caps) {
                    String filter = ManifestHeaderProcessor.generateFilter(filter(cap.getAttributes()));
                    builder.allow(BundleRevision.PACKAGE_NAMESPACE, filter);
                }
            }
            //TODO explicit services?
            for (FilterNamespaceType namespaceType: filterType.getNamespace()) {
                String namespace = namespaceType.getName();
                HashMap<String, Object> attributes = new HashMap<String, Object>();
                for (FilterAttributeType attributeType: namespaceType.getAttribute()) {
                    attributes.put(attributeType.getName(), attributeType.getValue());
                }
                String filter = ManifestHeaderProcessor.generateFilter(attributes);
                builder.allow(namespace, filter);
            }
            regionDigraph.connect(from, builder.build(), to);
        }
    }

    private Map<String, Object> filter(Map<String, Object> attributes) {
        Map<String, Object> result = new HashMap<String, Object>(attributes);
        result.remove("bundle-version");
        result.remove("bundle-symbolic-name");
        return result;
    }

    private void buildFilter(String packageName, String version, String namespace, List<FilterAttributeType> attributeTypes, RegionFilterBuilder builder) throws InvalidSyntaxException {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        if (namespace != null) {
            attributes.put(namespace, packageName);
        }
        if (version != null) {
            attributes.put("version", version);
        }
        for (FilterAttributeType attributeType: attributeTypes) {
            attributes.put(attributeType.getName(), attributeType.getValue());
        }
        String filter = ManifestHeaderProcessor.generateFilter(attributes);
        builder.allow(namespace, filter);
    }

}
