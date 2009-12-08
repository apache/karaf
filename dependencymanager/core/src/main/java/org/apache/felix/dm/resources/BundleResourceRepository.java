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
package org.apache.felix.dm.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class BundleResourceRepository {

	private final Bundle bundle;

	public BundleResourceRepository(Bundle bundle) {
		this.bundle = bundle;
	}

	public synchronized void addHandler(ServiceReference ref, ResourceHandler handler) {

		String filter = (String) ref.getProperty("filter"); // "(&(repository=a)(path=b)(name=*.xml))"

		Filter filterObject = null;

		try {
			filterObject = FrameworkUtil.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return;
		}

		Enumeration entries = bundle.findEntries("/", null, true);
		while (entries.hasMoreElements()) {
			URL entry = (URL) entries.nextElement();

			Properties props = new Properties();
			props.setProperty(Resource.REPOSITORY, bundle.getSymbolicName() + "_" + bundle.getHeaders().get("Bundle-Version"));
			props.setProperty(Resource.PATH, entry.getPath());
			props.setProperty(Resource.NAME, entry.getFile());

			if (filterObject.match(props))
				handler.added(new EntryResource(entry));

		}
	}

	public synchronized void removeHandler(ServiceReference ref, ResourceHandler handler) {

		String filter = (String) ref.getProperty("filter"); // "(&(repository=a)(path=b)(name=*.xml))"

		Filter filterObject = null;

		try {
			filterObject = FrameworkUtil.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return;
		}

		Enumeration entries = bundle.findEntries("/", null, true);
		while (entries.hasMoreElements()) {
			URL entry = (URL) entries.nextElement();

			Properties props = new Properties();
			props.setProperty(Resource.REPOSITORY, bundle.getSymbolicName() + "_" + bundle.getHeaders().get("Bundle-Version"));
			props.setProperty(Resource.PATH, entry.getPath());
			props.setProperty(Resource.NAME, entry.getFile());

			if (filterObject.match(props))
				handler.removed(new EntryResource(entry));

		}
	}

	class EntryResource implements Resource {

		URL entry;

		EntryResource(URL entry) {
			this.entry = entry;
		}

		public String getName() {
			return entry.getFile();
		}

		public String getPath() {
			return entry.getPath();
		}

		public String getRepository() {

			return bundle.getSymbolicName() + "_" + bundle.getHeaders().get("Bundle-Version");
		}

		public InputStream openStream() throws IOException {
			return entry.openStream();
		}
	}
}
