/*
 * $Id: BundleInfo.java 96 2009-08-26 13:44:04Z peter.kriens@aqute.biz $
 * 
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.impl.bundle.obr.resource;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import org.osgi.service.obr.Resource;

/**
 * Convert a bundle to a generic resource description and store its local
 * dependencies (like for example a license file in the JAR) in a zip file.
 * 
 * @version $Revision: 96 $
 */
public class BundleInfo {
	Manifest manifest;
	File bundleJar;
	ZipFile jar;
	String license;
	Properties localization;
	RepositoryImpl repository;

	/**
	 * Parse a zipFile from the file system. We only need the manifest and the
	 * localization. So a zip file is used to minimze memory consumption.
	 * 
	 * @param bundleJar
	 *            Path name
	 * @throws Exception
	 *             Any errors that occur
	 */
	public BundleInfo(RepositoryImpl repository, File bundleJar)
			throws Exception {
		this.bundleJar = bundleJar;
		this.repository = repository;

		if (!this.bundleJar.exists())
			throw new FileNotFoundException(bundleJar.toString());

		jar = new ZipFile(bundleJar);
		ZipEntry entry = jar.getEntry("META-INF/MANIFEST.MF");
		if (entry == null)
			throw new FileNotFoundException("No Manifest in "
					+ bundleJar.toString());
		manifest = new Manifest(jar.getInputStream(entry));
	}

	public BundleInfo(Manifest manifest) throws Exception {
		this.manifest = manifest;
	}

	/**
	 * Convert the bundle to a Resource. All URIs are going to be abslute, but
	 * could be local.
	 * 
	 * @return the resource
	 * @throws Exception
	 */
	public ResourceImpl build() throws Exception {
		ResourceImpl resource;
		// Setup the manifest
		// and create a resource
		resource = new ResourceImpl(repository, manifest.getSymbolicName(),
				manifest.getVersion());

		try {

			// Calculate the location URL of the JAR
			URL location = new URL("jar:" + bundleJar.toURL().toString() + "!/");
			resource.setURL(bundleJar.toURL());
			resource.setFile(bundleJar);

			doReferences(resource, location);
			doSize(resource);
			doCategories(resource);
			doImportExportServices(resource);
			doDeclarativeServices(resource);
			doFragment(resource);
			doRequires(resource);
			doBundle(resource);
			doExports(resource);
			doImports(resource);
			doExecutionEnvironment(resource);

			return resource;
		} finally {
			try {
				jar.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Check the size and add it.
	 * 
	 * @param resource
	 */
	void doSize(ResourceImpl resource) {
		long size = bundleJar.length();
		if (size > 0)
			resource.setSize(size);
	}

	/**
	 * Find the categories, break them up and add them.
	 * 
	 * @param resource
	 */
	void doCategories(ResourceImpl resource) {
		for (int i = 0; i < manifest.getCategories().length; i++) {
			String category = manifest.getCategories()[i];
			resource.addCategory(category);
		}
	}

	void doReferences(ResourceImpl resource, URL location) {
		// Presentation name
		String name = translated("Bundle-Name");
		if (name != null)
			resource.setPresentationName(name);

		// Handle license. -l allows a global license
		// set when no license is included.

		String license = translated("Bundle-License");
		if (license != null)
			resource.setLicense(toURL(location, license));
		else if (this.license != null)
			resource.setLicense(toURL(location, this.license));

		String description = translated("Bundle-Description");
		if (description != null)
			resource.setDescription(description);

		String copyright = translated("Bundle-Copyright");
		if (copyright != null)
			resource.setCopyright(copyright);

		String documentation = translated("Bundle-DocURL");
		if (documentation != null)
			resource.setDocumentation(toURL(location, documentation));

		String source = manifest.getValue("Bundle-Source");
		if (source != null)
			resource.setSource(toURL(location, source));
	}

	URL toURL(URL location, String source) {
		try {
			return new URL(location, source);
		} catch (Exception e) {
			System.err.println("Error in converting url: " + location + " : "
					+ source);
			return null;
		}
	}

	void doDeclarativeServices(ResourceImpl resource) throws Exception {
		String serviceComponent = manifest.getValue("service-component");
		if (serviceComponent == null)
			return;

		StringTokenizer st = new StringTokenizer(serviceComponent, " ,\t");
		String parts[] = new String[st.countTokens()];
		for (int i = 0; i < parts.length; i++)
			parts[i] = st.nextToken();

		for (int i = 0; i < parts.length; i++) {
			ZipEntry entry = jar.getEntry(parts[i]);
			if (entry == null) {
				System.err.println("Bad Service-Component header: "
						+ serviceComponent + ", no such file " + parts[i]);
			}
			InputStream in = jar.getInputStream(entry);
			// TODO parse declarative services files.
			in.close();
		}
	}

	void doImportExportServices(ResourceImpl resource) throws IOException {
		String importServices = manifest.getValue("import-service");
		if (importServices != null) {
			List entries = manifest.getEntries(importServices);
			for (Iterator i = entries.iterator(); i.hasNext();) {
				ManifestEntry entry = (ManifestEntry) i.next();
				RequirementImpl ri = new RequirementImpl("service");
				ri.setFilter(createServiceFilter(entry));
				ri.setComment("Import Service " + entry.getName());

				// TODO the following is arbitrary
				ri.setOptional(false);
				ri.setMultiple(true);
				resource.addRequirement(ri);
			}
		}

		String exportServices = manifest.getValue("export-service");
		if (exportServices != null) {
			List entries = manifest.getEntries(exportServices);
			for (Iterator i = entries.iterator(); i.hasNext();) {
				ManifestEntry entry = (ManifestEntry) i.next();
				CapabilityImpl cap = createServiceCapability(entry);
				resource.addCapability(cap);
			}
		}
	}

	String translated(String key) {
		return translate(manifest.getValue(key));
	}

	void doFragment(ResourceImpl resource) {
		// Check if we are a fragment
		ManifestEntry entry = manifest.getHost();
		if (entry == null) {
			return;
		} else {
			// We are a fragment, create a requirement
			// to our host.
			RequirementImpl r = new RequirementImpl("bundle");
			StringBuffer sb = new StringBuffer();
			sb.append("(&(symbolicname=");
			sb.append(entry.getName());
			sb.append(")");
			appendVersion(sb, entry.getVersion());
			sb.append(")");
			r.setFilter(sb.toString());
			r.setComment("Required Host " + entry.getName());
			r.setExtend(true);
			r.setOptional(false);
			r.setMultiple(false);
			resource.addRequirement(r);

			// And insert a capability that we are available
			// as a fragment. ### Do we need that with extend?
			CapabilityImpl capability = new CapabilityImpl("fragment");
			capability.addProperty("host", entry.getName());
			capability.addProperty("version", entry.getVersion());
			resource.addCapability(capability);
		}
	}

	void doRequires(ResourceImpl resource) {
		List entries = manifest.getRequire();
		if (entries == null)
			return;

		for (Iterator i = entries.iterator(); i.hasNext();) {
			ManifestEntry entry = (ManifestEntry) i.next();
			RequirementImpl r = new RequirementImpl("bundle");

			Map attrs = entry.getAttributes();
			String version = "0";
			if (attrs != null) {
				if (attrs.containsKey("bundle-version"))
					version = (String) attrs.get("bundle-version");
				else
					version = "0";
			}
			VersionRange v = new VersionRange(version);

			StringBuffer sb = new StringBuffer();
			sb.append("(&(symbolicname=");
			sb.append(entry.getName());
			sb.append(")");
			appendVersion(sb, v);
			sb.append(")");
			r.setFilter(sb.toString());

			r.setComment("Require Bundle " + entry.getName() + "; " + v);
			if (entry.directives != null
					&& "optional".equalsIgnoreCase((String) entry.directives
							.get("resolution")))
				r.setOptional(true);
			else
				r.setOptional(false);
			resource.addRequirement(r);
		}
	}

	void doExecutionEnvironment(ResourceImpl resource) {
		String[] parts = manifest.getRequiredExecutionEnvironments();
		if (parts == null)
			return;

		StringBuffer sb = new StringBuffer();
		sb.append("(|");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			sb.append("(ee=");
			sb.append(part);
			sb.append(")");
		}
		sb.append(")");

		RequirementImpl req = new RequirementImpl("ee");
		req.setFilter(sb.toString());
		req.setComment("Execution Environment " + sb.toString());
		resource.addRequirement(req);
	}

	void doImports(ResourceImpl resource) {
		List requirements = new ArrayList();
		List packages = manifest.getImports();
		if (packages == null)
			return;

		for (Iterator i = packages.iterator(); i.hasNext();) {
			ManifestEntry pack = (ManifestEntry) i.next();
			RequirementImpl requirement = new RequirementImpl("package");

			createImportFilter(requirement, "package", pack);
			requirement.setComment("Import package " + pack);
			String resolution = pack.getDirective("resolution");
			requirement.setOptional("optional".equals(resolution));
			requirements.add(requirement);
		}
		for (Iterator i = requirements.iterator(); i.hasNext();)
			resource.addRequirement((RequirementImpl) i.next());
	}

	String createServiceFilter(ManifestEntry pack) {
		StringBuffer filter = new StringBuffer();
		filter.append("(service=");
		filter.append(pack.getName());
		filter.append(")");
		return filter.toString();
	}

	void createImportFilter(RequirementImpl req, String name, ManifestEntry pack) {
		StringBuffer filter = new StringBuffer();
		filter.append("(&(");
		filter.append(name);
		filter.append("=");
		filter.append(pack.getName());
		filter.append(")");
		appendVersion(filter, pack.getVersion());
		Map attributes = pack.getAttributes();
		Set attrs = doImportPackageAttributes(req, filter, attributes);
		
		// The next code is using the subset operator 
		// to check mandatory attributes, it seems to be
		// impossible to rewrite. It must assert that whateber
		// is in mandatory: must be in any of the attributes.
		// This is a fundamental shortcoming of the filter language.
		if (attrs.size() > 0) {
			String del = "";
			filter.append("(mandatory:<*");
			for (Iterator i = attrs.iterator(); i.hasNext();) {
				filter.append(del);
				filter.append(i.next());
				del = ", ";
			}
			filter.append(")");
		}
		filter.append(")");
		req.setFilter(filter.toString());
	}

	private void appendVersion(StringBuffer filter, VersionRange version) {
		if (version != null) {
			if (version.isRange()) {
				if (version.includeLow()) {
					filter.append("(version");
					filter.append(">=");
					filter.append(version.low);
					filter.append(")");
				}
				else {
					filter.append("(!(version");
					filter.append("<=");
					filter.append(version.low);
					filter.append("))");
				}

				if ( version.includeHigh() ) {
					filter.append("(version");
					filter.append("<=");
					filter.append(version.high);
					filter.append(")");					
				}
				else {
					filter.append("(!(version");
					filter.append(">=");
					filter.append(version.high);
					filter.append("))");
				}
			} else {
				filter.append("(version>=");
				filter.append(version);
				filter.append(")");
			}
		}
	}

	Set doImportPackageAttributes(RequirementImpl req, StringBuffer filter,
			Map attributes) {
		HashSet set = new HashSet();

		if (attributes != null)
			for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
				String attribute = (String) i.next();
				String value = (String) attributes.get(attribute);
				if (attribute.equalsIgnoreCase("specification-version")
						|| attribute.equalsIgnoreCase("version"))
					continue;
				else if (attribute.equalsIgnoreCase("resolution:")) {
					req.setOptional(value.equalsIgnoreCase("optional"));
				}
				if (attribute.endsWith(":")) {
					// Ignore
				} else {
					filter.append("(");
					filter.append(attribute);
					filter.append("=");
					filter.append(attributes.get(attribute));
					filter.append(")");
					set.add(attribute);
				}
			}
		return set;
	}

	void doBundle(ResourceImpl resource) {
		CapabilityImpl capability = new CapabilityImpl("bundle");
		capability.addProperty("symbolicname", manifest.getSymbolicName());
		if (manifest.getValue("Bundle-Name") != null)
			capability.addProperty(Resource.PRESENTATION_NAME,
					translated("Bundle-Name"));
		capability.addProperty("version", manifest.getVersion());
		capability
				.addProperty("manifestversion", manifest.getManifestVersion());

		/**
		 * Is this needed TODO
		 */
		ManifestEntry host = manifest.getHost();
		if (host != null) {
			capability.addProperty("host", host.getName());
			if (host.getVersion() != null)
				capability.addProperty("version", host.getVersion());
		}
		resource.addCapability(capability);
	}

	void doExports(ResourceImpl resource) {
		List capabilities = new ArrayList();
		List packages = manifest.getExports();
		if (packages != null) {
			for (Iterator i = packages.iterator(); i.hasNext();) {
				ManifestEntry pack = (ManifestEntry) i.next();
				CapabilityImpl capability = createCapability("package", pack);
				capabilities.add(capability);
			}
		}
		for (Iterator i = capabilities.iterator(); i.hasNext();)
			resource.addCapability((CapabilityImpl) i.next());
	}

	CapabilityImpl createServiceCapability(ManifestEntry pack) {
		CapabilityImpl capability = new CapabilityImpl("service");
		capability.addProperty("service", pack.getName());
		return capability;
	}

	CapabilityImpl createCapability(String name, ManifestEntry pack) {
		CapabilityImpl capability = new CapabilityImpl(name);
		capability.addProperty(name, pack.getName());
		capability.addProperty("version", pack.getVersion());
		Map attributes = pack.getAttributes();
		if (attributes != null)
			for (Iterator at = attributes.keySet().iterator(); at.hasNext();) {
				String key = (String) at.next();
				if (key.equalsIgnoreCase("specification-version")
						|| key.equalsIgnoreCase("version"))
					continue;
				else {
					Object value = attributes.get(key);
					capability.addProperty(key, value);
				}
			}
		Map directives = pack.getDirectives();
		if (directives != null)
			for (Iterator at = directives.keySet().iterator(); at.hasNext();) {
				String key = (String) at.next();
				Object value = directives.get(key);
				capability.addProperty(key, value);
			}
		return capability;
	}

	String translate(String s) {
		if (s == null)
			return null;

		if (!s.startsWith("%")) {
			return s;
		}

		if (localization == null)
			try {
				localization = new Properties();
				String path = manifest
						.getValue("Bundle-Localization", "bundle");
				path += ".properties";
				InputStream in = jar.getInputStream(new ZipEntry(path));
				if (in != null) {
					localization.load(in);
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		s = s.substring(1);
		return localization.getProperty(s, s);
	}

	File getZipFile() {
		return bundleJar;
	}
}