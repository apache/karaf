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
package org.apache.felix.bundlerepository.impl;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.filter.FilterImpl;
import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

public class DataModelHelperImpl implements DataModelHelper
{

    public static final String BUNDLE_LICENSE = "Bundle-License";
    public static final String BUNDLE_SOURCE = "Bundle-Source";

    public Requirement requirement(String name, String filter)
    {
        RequirementImpl req = new RequirementImpl();
        req.setName(name);
        if (filter != null)
        {
            req.setFilter(filter);
        }
        return req;
    }

    public Filter filter(String filter)
    {
        try
        {
            return FilterImpl.newInstance(filter);
        }
        catch (InvalidSyntaxException e)
        {
            IllegalArgumentException ex = new IllegalArgumentException();
            ex.initCause(e);
            throw ex;
        }
    }

    public Repository repository(final URL url) throws Exception
    {
        InputStream is = null;
        BufferedReader br = null;

        try
        {
            if (url.getPath().endsWith(".zip"))
            {
                ZipInputStream zin = new ZipInputStream(FileUtil.openURL(url));
                ZipEntry entry = zin.getNextEntry();
                while (entry != null)
                {
                    if (entry.getName().equals("repository.xml"))
                    {
                        is = zin;
                        break;
                    }
                    entry = zin.getNextEntry();
                }
            }
            else
            {
                is = FileUtil.openURL(url);
            }

            if (is != null)
            {
                RepositoryImpl repository = repository(is);
                repository.setURI(url.toExternalForm());
                return repository;
            }
            else
            {
                // This should not happen.
                throw new Exception("Unable to get input stream for repository.");
            }
        }
        finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }
    }

    public RepositoryImpl repository(InputStream is) throws Exception
    {
        RepositoryParser parser = RepositoryParser.getParser();
        RepositoryImpl repository = parser.parseRepository(is);
        return repository;
    }

    public Repository repository(Resource[] resources)
    {
        return new RepositoryImpl(resources);
    }

    public Capability capability(String name, Map properties)
    {
        CapabilityImpl cap = new CapabilityImpl(name);
        for (Iterator it = properties.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            cap.addProperty((String) e.getKey(), (String) e.getValue());
        }
        return cap;
    }

    public String writeRepository(Repository repository)
    {
        try
        {
            StringWriter sw = new StringWriter();
            writeRepository(repository, sw);
            return sw.toString();
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public void writeRepository(Repository repository, Writer writer) throws IOException
    {
        XmlWriter w = new XmlWriter(writer);
        toXml(w, repository);
    }

    public String writeResource(Resource resource)
    {
        try
        {
            StringWriter sw = new StringWriter();
            writeResource(resource, sw);
            return sw.toString();
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public void writeResource(Resource resource, Writer writer) throws IOException
    {
        XmlWriter w = new XmlWriter(writer);
        toXml(w, resource);
    }

    public String writeCapability(Capability capability)
    {
        try
        {
            StringWriter sw = new StringWriter();
            writeCapability(capability, sw);
            return sw.toString();
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public void writeCapability(Capability capability, Writer writer) throws IOException
    {
        XmlWriter w = new XmlWriter(writer);
        toXml(w, capability);
    }

    public String writeRequirement(Requirement requirement)
    {
        try
        {
            StringWriter sw = new StringWriter();
            writeRequirement(requirement, sw);
            return sw.toString();
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public void writeRequirement(Requirement requirement, Writer writer) throws IOException
    {
        XmlWriter w = new XmlWriter(writer);
        toXml(w, requirement);
    }

    public String writeProperty(Property property)
    {
        try
        {
            StringWriter sw = new StringWriter();
            writeProperty(property, sw);
            return sw.toString();
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public void writeProperty(Property property, Writer writer) throws IOException
    {
        XmlWriter w = new XmlWriter(writer);
        toXml(w, property);
    }

    private static void toXml(XmlWriter w, Repository repository) throws IOException
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss.SSS");
        w.element(RepositoryParser.REPOSITORY)
            .attribute(RepositoryParser.NAME, repository.getName())
            .attribute(RepositoryParser.LASTMODIFIED, format.format(new Date(repository.getLastModified())));

        if (repository instanceof RepositoryImpl)
        {
            Referral[] referrals = ((RepositoryImpl) repository).getReferrals();
            for (int i = 0; referrals != null && i < referrals.length; i++)
            {
                w.element(RepositoryParser.REFERRAL)
                    .attribute(RepositoryParser.DEPTH, new Integer(referrals[i].getDepth()))
                    .attribute(RepositoryParser.URL, referrals[i].getUrl())
                    .end();
            }
        }

        Resource[] resources = repository.getResources();
        for (int i = 0; resources != null && i < resources.length; i++)
        {
            toXml(w, resources[i]);
        }

        w.end();
    }

    private static void toXml(XmlWriter w, Resource resource) throws IOException
    {
        w.element(RepositoryParser.RESOURCE)
            .attribute(Resource.ID, resource.getId())
            .attribute(Resource.SYMBOLIC_NAME, resource.getSymbolicName())
            .attribute(Resource.PRESENTATION_NAME, resource.getPresentationName())
            .attribute(Resource.URI, getRelativeUri(resource, Resource.URI))
            .attribute(Resource.VERSION, resource.getVersion().toString());

        w.textElement(Resource.DESCRIPTION, resource.getProperties().get(Resource.DESCRIPTION))
            .textElement(Resource.SIZE, resource.getProperties().get(Resource.SIZE))
            .textElement(Resource.DOCUMENTATION_URI, getRelativeUri(resource, Resource.DOCUMENTATION_URI))
            .textElement(Resource.SOURCE_URI, getRelativeUri(resource, Resource.SOURCE_URI))
            .textElement(Resource.JAVADOC_URI, getRelativeUri(resource, Resource.JAVADOC_URI))
            .textElement(Resource.LICENSE_URI, getRelativeUri(resource, Resource.LICENSE_URI));

        String[] categories = resource.getCategories();
        for (int i = 0; categories != null && i < categories.length; i++)
        {
            w.element(RepositoryParser.CATEGORY)
                .attribute(RepositoryParser.ID, categories[i])
                .end();
        }
        Capability[] capabilities = resource.getCapabilities();
        for (int i = 0; capabilities != null && i < capabilities.length; i++)
        {
            toXml(w, capabilities[i]);
        }
        Requirement[] requirements = resource.getRequirements();
        for (int i = 0; requirements != null && i < requirements.length; i++)
        {
            toXml(w, requirements[i]);
        }
        w.end();
    }

    private static String getRelativeUri(Resource resource, String name) 
    {
        String uri = (String) resource.getProperties().get(name);
        if (resource instanceof ResourceImpl)
        {
            try
            {
                uri = URI.create(((ResourceImpl) resource).getRepository().getURI()).relativize(URI.create(uri)).toASCIIString();
            }
            catch (Throwable t)
            {
            }
        }
        return uri;
    }

    private static void toXml(XmlWriter w, Capability capability) throws IOException
    {
        w.element(RepositoryParser.CAPABILITY)
            .attribute(RepositoryParser.NAME, capability.getName());
        Property[] props = capability.getProperties();
        for (int j = 0; props != null && j < props.length; j++)
        {
            toXml(w, props[j]);
        }
        w.end();
    }

    private static void toXml(XmlWriter w, Property property) throws IOException
    {
        w.element(RepositoryParser.P)
            .attribute(RepositoryParser.N, property.getName())
            .attribute(RepositoryParser.T, property.getType())
            .attribute(RepositoryParser.V, property.getValue())
            .end();
    }

    private static void toXml(XmlWriter w, Requirement requirement) throws IOException
    {
        w.element(RepositoryParser.REQUIRE)
            .attribute(RepositoryParser.NAME, requirement.getName())
            .attribute(RepositoryParser.FILTER, requirement.getFilter())
            .attribute(RepositoryParser.EXTEND, Boolean.toString(requirement.isExtend()))
            .attribute(RepositoryParser.MULTIPLE, Boolean.toString(requirement.isMultiple()))
            .attribute(RepositoryParser.OPTIONAL, Boolean.toString(requirement.isOptional()))
            .text(requirement.getComment().trim())
            .end();
    }

    public Resource createResource(final Bundle bundle)
    {
        final Dictionary dict = bundle.getHeaders();
        return createResource(new Headers()
        {
            public String getHeader(String name)
            {
                return (String) dict.get(name);
            }
        });
    }

    public Resource createResource(final URL bundleUrl) throws IOException
    {
        ResourceImpl resource = createResource(new Headers()
        {
            private final Manifest manifest;
            private Properties localization;
            {
                // Do not use a JarInputStream so that we can read the manifest even if it's not
                // the first entry in the JAR.
                byte[] man = loadEntry(JarFile.MANIFEST_NAME);
                if (man == null)
                {
                    throw new IllegalArgumentException("The specified url is not a valid jar (can't read manifest): " + bundleUrl);
                }
                manifest = new Manifest(new ByteArrayInputStream(man));
            }
            public String getHeader(String name)
            {
                String value = manifest.getMainAttributes().getValue(name);
                if (value != null && value.startsWith("%"))
                {
                    if (localization == null)
                    {
                        try
                        {
                            localization = new Properties();
                            String path = manifest.getMainAttributes().getValue(Constants.BUNDLE_LOCALIZATION);
                            if (path == null)
                            {
                                path = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
                            }
                            path += ".properties";
                            byte[] loc = loadEntry(path);
                            if (loc != null)
                            {
                                localization.load(new ByteArrayInputStream(loc));
                            }
                        }
                        catch (IOException e)
                        {
                            // TODO: ?
                        }
                    }
                    value = value.substring(1);
                    value = localization.getProperty(value, value);
                }
                return value;
            }
            private byte[] loadEntry(String name) throws IOException
            {
                InputStream is = FileUtil.openURL(bundleUrl);
                try
                {
                    ZipInputStream jis = new ZipInputStream(is);
                    for (ZipEntry e = jis.getNextEntry(); e != null; e = jis.getNextEntry())
                    {
                        if (name.equalsIgnoreCase(e.getName()))
                        {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int n;
                            while ((n = jis.read(buf, 0, buf.length)) > 0)
                            {
                                baos.write(buf, 0, n);
                            }
                            return baos.toByteArray();
                        }
                    }
                }
                finally
                {
                    is.close();
                }
                return null;
            }
        });
        if (resource != null)
        {
            if ("file".equals(bundleUrl.getProtocol()))
            {
                try {
                    File f = new File(bundleUrl.toURI());
                    resource.put(Resource.SIZE, Long.toString(f.length()), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            resource.put(Resource.URI, bundleUrl.toExternalForm(), null);
        }
        return resource;
    }

    public Resource createResource(final Attributes attributes)
    {
        return createResource(new Headers()
        {
            public String getHeader(String name)
            {
                return attributes.getValue(name);
            }
        });
    }

    public ResourceImpl createResource(Headers headers)
    {
        String bsn = headers.getHeader(Constants.BUNDLE_SYMBOLICNAME);
        if (bsn == null)
        {
            return null;
        }
        ResourceImpl resource = new ResourceImpl();
        populate(headers, resource);
        return resource;
    }

    static void populate(Headers headers, ResourceImpl resource)
    {
        String bsn = getSymbolicName(headers);
        String v = getVersion(headers);

        resource.put(Resource.ID, bsn + "/" + v);
        resource.put(Resource.SYMBOLIC_NAME, bsn);
        resource.put(Resource.VERSION, v);
        if (headers.getHeader(Constants.BUNDLE_NAME) != null)
        {
            resource.put(Resource.PRESENTATION_NAME, headers.getHeader(Constants.BUNDLE_NAME));
        }
        if (headers.getHeader(Constants.BUNDLE_DESCRIPTION) != null)
        {
            resource.put(Resource.DESCRIPTION, headers.getHeader(Constants.BUNDLE_DESCRIPTION));
        }
        if (headers.getHeader(BUNDLE_LICENSE) != null)
        {
            resource.put(Resource.LICENSE_URI, headers.getHeader(BUNDLE_LICENSE));
        }
        if (headers.getHeader(Constants.BUNDLE_COPYRIGHT) != null)
        {
            resource.put(Resource.COPYRIGHT, headers.getHeader(Constants.BUNDLE_COPYRIGHT));
        }
        if (headers.getHeader(Constants.BUNDLE_DOCURL) != null)
        {
            resource.put(Resource.DOCUMENTATION_URI, headers.getHeader(Constants.BUNDLE_DOCURL));
        }
        if (headers.getHeader(BUNDLE_SOURCE) != null)
        {
            resource.put(Resource.SOURCE_URI, headers.getHeader(BUNDLE_SOURCE));
        }

        doCategories(resource, headers);
        doBundle(resource, headers);
        doImportExportServices(resource, headers);
        doFragment(resource, headers);
        doRequires(resource, headers);
        doExports(resource, headers);
        doImports(resource, headers);
        doExecutionEnvironment(resource, headers);
    }

    private static void doCategories(ResourceImpl resource, Headers headers)
    {
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.BUNDLE_CATEGORY));
        for (int i = 0; clauses != null && i < clauses.length; i++)
        {
            resource.addCategory(clauses[i].getName());
        }
    }

    private static void doImportExportServices(ResourceImpl resource, Headers headers)
    {
        Clause[] imports = Parser.parseHeader(headers.getHeader(Constants.IMPORT_SERVICE));
        for (int i = 0; imports != null && i < imports.length; i++) {
            RequirementImpl ri = new RequirementImpl(Capability.SERVICE);
            ri.setFilter(createServiceFilter(imports[i]));
            ri.addText("Import Service " + imports[i].getName());

            String avail = imports[i].getDirective("availability");
            ri.setOptional("optional".equalsIgnoreCase(avail));
            ri.setMultiple(true);
            resource.addRequire(ri);
        }

        Clause[] exports = Parser.parseHeader(headers.getHeader(Constants.EXPORT_SERVICE));
        for (int i = 0; exports != null && i < exports.length; i++) {
            CapabilityImpl cap = createServiceCapability(exports[i]);
            resource.addCapability(cap);
        }
    }

    private static String createServiceFilter(Clause clause) {
        String f = clause.getAttribute("filter");
		StringBuffer filter = new StringBuffer();
        if (f != null) {
            filter.append("(&");
        }
        filter.append("(");
        filter.append(Capability.SERVICE);
        filter.append("=");
		filter.append(clause.getName());
		filter.append(")");
        if (f != null) {
            if (!f.startsWith("("))
            {
                filter.append("(").append(f).append(")");
            }
            else
            {
                filter.append(f);
            }
            filter.append(")");
        }
		return filter.toString();
    }

    private static CapabilityImpl createServiceCapability(Clause clause) {
        CapabilityImpl capability = new CapabilityImpl(Capability.SERVICE);
        capability.addProperty(Capability.SERVICE, clause.getName());
        Attribute[] attributes = clause.getAttributes();
        for (int i = 0; attributes != null && i < attributes.length; i++)
        {
            capability.addProperty(attributes[i].getName(), attributes[i].getValue());
        }
        return capability;
    }

    private static void doFragment(ResourceImpl resource, Headers headers)
    {
        // Check if we are a fragment
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.FRAGMENT_HOST));
        if (clauses != null && clauses.length == 1)
        {
            // We are a fragment, create a requirement
            // to our host.
            RequirementImpl r = new RequirementImpl(Capability.BUNDLE);
            StringBuffer sb = new StringBuffer();
            sb.append("(&(symbolicname=");
            sb.append(clauses[0].getName());
            sb.append(")");
            appendVersion(sb, VersionRange.parseVersionRange(clauses[0].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)));
            sb.append(")");
            r.setFilter(sb.toString());
            r.addText("Required Host " + clauses[0].getName());
            r.setExtend(true);
            r.setOptional(false);
            r.setMultiple(false);
            resource.addRequire(r);

            // And insert a capability that we are available
            // as a fragment. ### Do we need that with extend?
            CapabilityImpl capability = new CapabilityImpl(Capability.FRAGMENT);
            capability.addProperty("host", clauses[0].getName());
            capability.addProperty("version", Property.VERSION, getVersion(clauses[0]));
            resource.addCapability(capability);
        }
    }

    private static void doRequires(ResourceImpl resource, Headers headers)
    {
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.REQUIRE_BUNDLE));
        for (int i = 0; clauses != null && i < clauses.length; i++) {
            RequirementImpl r = new RequirementImpl(Capability.BUNDLE);

            VersionRange v = VersionRange.parseVersionRange(clauses[i].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));

            StringBuffer sb = new StringBuffer();
            sb.append("(&(symbolicname=");
            sb.append(clauses[i].getName());
            sb.append(")");
            appendVersion(sb, v);
            sb.append(")");
            r.setFilter(sb.toString());

            r.addText("Require Bundle " + clauses[i].getName() + "; " + v);
            r.setOptional(Constants.RESOLUTION_OPTIONAL.equalsIgnoreCase(clauses[i].getDirective(Constants.RESOLUTION_DIRECTIVE)));
            resource.addRequire(r);
        }
    }

    private static void doBundle(ResourceImpl resource, Headers headers) {
        CapabilityImpl capability = new CapabilityImpl(Capability.BUNDLE);
        capability.addProperty(Resource.SYMBOLIC_NAME, getSymbolicName(headers));
        if (headers.getHeader(Constants.BUNDLE_NAME) != null)
        {
            capability.addProperty(Resource.PRESENTATION_NAME, headers.getHeader(Constants.BUNDLE_NAME));
        }
        capability.addProperty(Resource.VERSION, Property.VERSION, getVersion(headers));
        capability.addProperty(Resource.MANIFEST_VERSION, getManifestVersion(headers));
        resource.addCapability(capability);
    }

    private static void doExports(ResourceImpl resource, Headers headers)
    {
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.EXPORT_PACKAGE));
        for (int i = 0; clauses != null && i < clauses.length; i++)
        {
            CapabilityImpl capability = createCapability(Capability.PACKAGE, clauses[i]);
            resource.addCapability(capability);
        }
    }

    private static CapabilityImpl createCapability(String name, Clause clause)
    {
        CapabilityImpl capability = new CapabilityImpl(name);
        capability.addProperty(name, clause.getName());
        capability.addProperty(Resource.VERSION, Property.VERSION, getVersion(clause));
        Attribute[] attributes = clause.getAttributes();
        for (int i = 0; attributes != null && i < attributes.length; i++)
        {
            String key = attributes[i].getName();
            if (key.equalsIgnoreCase(Constants.PACKAGE_SPECIFICATION_VERSION) || key.equalsIgnoreCase(Constants.VERSION_ATTRIBUTE))
            {
                continue;
            }
            else
            {
                String value = attributes[i].getValue();
                capability.addProperty(key, value);
            }
        }
        Directive[] directives = clause.getDirectives();
        for (int i = 0; directives != null && i < directives.length; i++)
        {
            String key = directives[i].getName();
            String value = directives[i].getValue();
            capability.addProperty(key + ":", value);
        }
        return capability;
    }

    private static void doImports(ResourceImpl resource, Headers headers)
    {
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.IMPORT_PACKAGE));
        for (int i = 0; clauses != null && i < clauses.length; i++)
        {
            RequirementImpl requirement = new RequirementImpl(Capability.PACKAGE);

            createImportFilter(requirement, Capability.PACKAGE, clauses[i]);
            requirement.addText("Import package " + clauses[i]);
            requirement.setOptional(Constants.RESOLUTION_OPTIONAL.equalsIgnoreCase(clauses[i].getDirective(Constants.RESOLUTION_DIRECTIVE)));
            resource.addRequire(requirement);
        }
    }

    private static void createImportFilter(RequirementImpl requirement, String name, Clause clause)
    {
        StringBuffer filter = new StringBuffer();
        filter.append("(&(");
        filter.append(name);
        filter.append("=");
        filter.append(clause.getName());
        filter.append(")");
        appendVersion(filter, getVersionRange(clause));
        Attribute[] attributes = clause.getAttributes();
        Set attrs = doImportPackageAttributes(requirement, filter, attributes);

        // The next code is using the subset operator
        // to check mandatory attributes, it seems to be
        // impossible to rewrite. It must assert that whateber
        // is in mandatory: must be in any of the attributes.
        // This is a fundamental shortcoming of the filter language.
        if (attrs.size() > 0)
        {
            String del = "";
            filter.append("(mandatory:<*");
            for (Iterator i = attrs.iterator(); i.hasNext();)
            {
                filter.append(del);
                filter.append(i.next());
                del = ", ";
            }
            filter.append(")");
        }
        filter.append(")");
        requirement.setFilter(filter.toString());
    }

    private static Set doImportPackageAttributes(RequirementImpl requirement, StringBuffer filter, Attribute[] attributes)
    {
        HashSet set = new HashSet();
        for (int i = 0; attributes != null && i < attributes.length; i++)
        {
            String name = attributes[i].getName();
            String value = attributes[i].getValue();
            if (name.equalsIgnoreCase(Constants.PACKAGE_SPECIFICATION_VERSION) || name.equalsIgnoreCase(Constants.VERSION_ATTRIBUTE))
            {
                continue;
            }
            else if (name.equalsIgnoreCase(Constants.RESOLUTION_DIRECTIVE + ":"))
            {
                requirement.setOptional(Constants.RESOLUTION_OPTIONAL.equalsIgnoreCase(value));
            }
            if (name.endsWith(":"))
            {
                // Ignore
            }
            else
            {
                filter.append("(");
                filter.append(name);
                filter.append("=");
                filter.append(value);
                filter.append(")");
                set.add(name);
            }
        }
        return set;
    }

    private static void doExecutionEnvironment(ResourceImpl resource, Headers headers)
    {
        Clause[] clauses = Parser.parseHeader(headers.getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));
        if (clauses != null && clauses.length > 0)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("(|");
            for (int i = 0; i < clauses.length; i++)
            {
                sb.append("(");
                sb.append(Capability.EXECUTIONENVIRONMENT);
                sb.append("=");
                sb.append(clauses[i].getName());
                sb.append(")");
            }
            sb.append(")");
            RequirementImpl req = new RequirementImpl(Capability.EXECUTIONENVIRONMENT);
            req.setFilter(sb.toString());
            req.addText("Execution Environment " + sb.toString());
            resource.addRequire(req);
        }
    }

    private static String getVersion(Clause clause)
    {
        String v = clause.getAttribute(Constants.VERSION_ATTRIBUTE);
        if (v == null)
        {
            v = clause.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
        }
        if (v == null)
        {
            v = clause.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return VersionCleaner.clean(v);
    }

    private static VersionRange getVersionRange(Clause clause)
    {
        String v = clause.getAttribute(Constants.VERSION_ATTRIBUTE);
        if (v == null)
        {
            v = clause.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
        }
        if (v == null)
        {
            v = clause.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return VersionRange.parseVersionRange(v);
    }

    private static String getSymbolicName(Headers headers)
    {
        String bsn = headers.getHeader(Constants.BUNDLE_SYMBOLICNAME);
        if (bsn == null)
        {
            bsn = headers.getHeader(Constants.BUNDLE_NAME);
            if (bsn == null)
            {
                bsn = "Untitled-" + headers.hashCode();
            }
        }
        Clause[] clauses = Parser.parseHeader(bsn);
        return clauses[0].getName();
    }

    private static String getVersion(Headers headers)
    {
        String v = headers.getHeader(Constants.BUNDLE_VERSION);
        return VersionCleaner.clean(v);
    }

    private static String getManifestVersion(Headers headers)
    {
        String v = headers.getHeader(Constants.BUNDLE_MANIFESTVERSION);
        if (v == null)
        {
            v = "1";
        }
        return v;
    }

    private static void appendVersion(StringBuffer filter, VersionRange version)
    {
        if (version != null)
        {
            if ( !version.isOpenFloor() )
            {
                if ( !Version.emptyVersion.equals(version.getFloor()) )
                {
                    filter.append("(");
                    filter.append(Constants.VERSION_ATTRIBUTE);
                    filter.append(">=");
                    filter.append(version.getFloor());
                    filter.append(")");
                }
            }
            else
            {
                filter.append("(!(");
                filter.append(Constants.VERSION_ATTRIBUTE);
                filter.append("<=");
                filter.append(version.getFloor());
                filter.append("))");
            }

            if (!VersionRange.INFINITE_VERSION.equals(version.getCeiling()))
            {
                if ( !version.isOpenCeiling() )
                {
                    filter.append("(");
                    filter.append(Constants.VERSION_ATTRIBUTE);
                    filter.append("<=");
                    filter.append(version.getCeiling());
                    filter.append(")");
                }
                else
                {
                    filter.append("(!(");
                    filter.append(Constants.VERSION_ATTRIBUTE);
                    filter.append(">=");
                    filter.append(version.getCeiling());
                    filter.append("))");
                }
            }
        }
    }

    interface Headers
    {
        String getHeader(String name);
    }

    public Repository readRepository(String xml) throws Exception
    {
        try
        {
            return readRepository(new StringReader(xml));
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public Repository readRepository(Reader reader) throws Exception
    {
        return RepositoryParser.getParser().parseRepository(reader);
    }

    public Resource readResource(String xml) throws Exception
    {
        try
        {
            return readResource(new StringReader(xml));
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public Resource readResource(Reader reader) throws Exception
    {
        return RepositoryParser.getParser().parseResource(reader);
    }

    public Capability readCapability(String xml) throws Exception
    {
        try
        {
            return readCapability(new StringReader(xml));
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public Capability readCapability(Reader reader) throws Exception
    {
        return RepositoryParser.getParser().parseCapability(reader);
    }

    public Requirement readRequirement(String xml) throws Exception
    {
        try
        {
            return readRequirement(new StringReader(xml));
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public Requirement readRequirement(Reader reader) throws Exception
    {
        return RepositoryParser.getParser().parseRequirement(reader);
    }

    public Property readProperty(String xml) throws Exception
    {
        try
        {
            return readProperty(new StringReader(xml));
        }
        catch (IOException e)
        {
            IllegalStateException ex = new IllegalStateException(e);
            ex.initCause(e);
            throw ex;
        }
    }

    public Property readProperty(Reader reader) throws Exception
    {
        return RepositoryParser.getParser().parseProperty(reader);
    }
}
