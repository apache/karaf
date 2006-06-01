/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tools.maven.plugin;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class Jar {
	Map			resources	= new TreeMap();
	Map			imports		= new HashMap();
	Map			exports		= new HashMap();
	Manifest	manifest;
	boolean		manifestFirst;
	String		name;
	Jar			parent;
	List		activators	= new ArrayList();

	public Jar(Jar parent, String name, InputStream in) throws IOException {
		this.name = name;
		this.parent = parent;
		ZipInputStream jar = new ZipInputStream(in);
		ZipEntry entry = jar.getNextEntry();
		boolean first = true;
		while (entry != null) {
			String path = entry.getName();

			if (path.endsWith(".class")) {
				Clazz clazz = new Clazz(this, path, jar);
				resources.put(path, clazz);
			}
			else if (path.endsWith(".jar")) {
				Jar pool = new Jar(this, path, jar);
				resources.put(path, pool);
			}
			else if (path.endsWith("/packageinfo")
					&& !path.startsWith("OSGI-OPT")) {
				String version = parsePackageInfo(jar, exports);
				resources.put(path, version);
			}
			else if (path.equals("META-INF/MANIFEST.MF")) {
				manifestFirst = first;
				manifest = new Manifest(jar);
			}
			else
				resources.put(path, null);

			entry = jar.getNextEntry();
			if (!path.endsWith("/"))
				first = false;
		}
	}

	public Jar(Jar parent, String name, File rootDir) throws IOException {
		this.name = name;
		this.parent = parent;
		traverse(rootDir.getAbsolutePath().length(), rootDir, rootDir.list());
	}

	void traverse(int root, File dir, String list[]) throws IOException {
		for (int i = 0; i < list.length; i++) {
			File sub = new File(dir, list[i]);
			if (sub.isDirectory())
				traverse(root, sub, sub.list());
			else {
				String path = sub.getAbsolutePath().substring(root + 1)
						.replace(File.separatorChar, '/');
				FileInputStream in = new FileInputStream(sub);

				if (path.endsWith(".class")) {
					Clazz clazz = new Clazz(this, path, in);
					resources.put(path, clazz);
				}
				else if (path.endsWith(".jar")) {
					Jar pool = new Jar(this, path, in);
					resources.put(path, pool);
				}
				else if (path.endsWith("/packageinfo")
						&& !path.startsWith("OSGI-OPT")) {
					String version = parsePackageInfo(in, exports);
					resources.put(path, version);
				}
				else if (path.endsWith("META-INF/MANIFEST.MF")) {
					manifest = new Manifest(in);
				}
				else
					resources.put(path, null);
			}
		}
	}

	private static String parsePackageInfo(InputStream jar, Map exports)
			throws IOException {
		try {
			byte[] buf = collect(jar, 0);
			String line = new String(buf);
			StringTokenizer qt = new StringTokenizer(line, " \r\n\t");
			if (qt.hasMoreElements()) {
				qt.nextToken();
				if (qt.hasMoreElements()) {
					String version = qt.nextToken();
					return version;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Convenience method to turn an inputstream into a byte array. The method
	 * uses a recursive algorithm to minimize memory usage.
	 * 
	 * @param in stream with data
	 * @param offset where we are in the stream
	 * @returns byte array filled with data
	 */
	private static byte[] collect(InputStream in, int offset)
			throws IOException {
		byte[] result;
		byte[] buffer = new byte[10000];
		int size = in.read(buffer);
		if (size <= 0)
			return new byte[offset];
		else
			result = collect(in, offset + size);
		System.arraycopy(buffer, 0, result, offset, size);
		return result;
	}

	public Manifest getManifest() {
		return manifest;
	}

	public Object getEntry(String resource) {
		return resources.get(resource);
	}

	public boolean exists(String jarOrDir) {
		return resources.keySet().contains(jarOrDir);
	}

	public Set getEntryPaths(String prefix) {
		Set result = new TreeSet();
		for (Iterator i = resources.keySet().iterator(); i.hasNext();) {
			String path = (String) i.next();
			if (path.startsWith(prefix))
				result.add(path);
		}
		return result;
	}

	String getName() {
		return name;
	}

	public String toString() {
		return getName();
	}

	public void addActivator(String path) {
		if (parent != null)
			parent.addActivator(path);
		else {
			activators.add(path);
		}

	}

    public boolean containsActivator(String path) {
        if (parent != null)
            return parent.containsActivator(path);
        return activators.contains(path);
    }
}
