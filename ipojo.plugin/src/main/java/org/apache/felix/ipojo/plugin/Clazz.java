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
package org.apache.felix.ipojo.plugin;

import java.io.*;
import java.util.*;

public class Clazz {
	static byte	SkipTable[]	= {0, // 0 non existent
		-1, // 1 CONSTANT_utf8 UTF 8, handled in
		// method
		-1, // 2
		4, // 3 CONSTANT_Integer
		4, // 4 CONSTANT_Float
		8, // 5 CONSTANT_Long (index +=2!)
		8, // 6 CONSTANT_Double (index +=2!)
		-1, // 7 CONSTANT_Class
		2, // 8 CONSTANT_String
		4, // 9 CONSTANT_FieldRef
		4, // 10 CONSTANT_MethodRef
		4, // 11 CONSTANT_InterfaceMethodRef
		4, // 12 CONSTANT_NameAndType
						};

	Set			imports = new HashSet();
	String		path;
	Jar			jar;
	
	public Clazz(Jar jar, String path, InputStream in) throws IOException {
		this.path = path;
		this.jar = jar;
		DataInputStream din = new DataInputStream(in);
		parseClassFile(din);
	}


	void parseClassFile(DataInputStream in)
			throws IOException {
		Set classes = new HashSet();
		Set descriptors = new HashSet();
		Hashtable pool = new Hashtable();
		try {
			int magic = in.readInt();
			if (magic != 0xCAFEBABE)
				throw new IOException(
						"Not a valid class file (no CAFEBABE header)");
			in.readShort(); // minor version
			in.readShort(); // major version
			int count = in.readUnsignedShort();
			process: for (int i = 1; i < count; i++) {
				byte tag = in.readByte();
				switch (tag) {
					case 0 :
						break process;
					case 1 :
						// CONSTANT_Utf8
						String name = in.readUTF();
						pool.put(new Integer(i), name);
						break;
					// A Class constant is just a short reference in
					// the constant pool
					case 7 :
						// CONSTANT_Class
						Integer index = new Integer(in.readShort());
						classes.add(index);
						break;
					// For some insane optimization reason are
					// the long and the double two entries in the
					// constant pool. See 4.4.5
					case 5 :
						// CONSTANT_Long
					case 6 :
						// CONSTANT_Double
						in.skipBytes(8);
						i++;
						break;

					// Interface Method Ref
					case 12 :
						in.readShort(); // Name index
						int descriptorIndex = in.readShort();
						descriptors.add(new Integer(descriptorIndex));
						break;

					// We get the skip count for each record type
					// from the SkipTable. This will also automatically
					// abort when
					default :
						if (tag == 2)
							throw new IOException("Invalid tag " + tag);
						in.skipBytes(SkipTable[tag]);
						break;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		//
		// Now iterate over all classes we found and
		// parse those as well. We skip duplicates
		//

		for (Iterator e = classes.iterator(); e.hasNext();) {
			Integer n = (Integer) e.next();
			String next = (String) pool.get(n);
			if (next != null) {
				String normalized = normalize(next);
				if (normalized != null) {
                    // For purposes of trying to guess the activator class, we assume
                    // that any class that references the BundleActivator interface
                    // must be a BundleActivator implementation.
					if ( normalized.startsWith("org/osgi/framework/BundleActivator")) {
						String cname = path.replace('/', '.');
						cname = cname.substring(0,cname.length()-".class" .length());
						jar.addActivator(cname);
					}
					String pack = getPackage(normalized);
					if (!pack.startsWith("java."))
						imports.add(pack);
				}
			}
			else
				throw new IllegalArgumentException("Invalid class, parent=");
		}
		for (Iterator e = descriptors.iterator(); e.hasNext();) {
			Integer n = (Integer) e.next();
			String prototype = (String) pool.get(n);
			if (prototype != null)
				parseDescriptor(prototype);
		}
	}

	void parseDescriptor(String prototype) {
		addReference(prototype);
		StringTokenizer st = new StringTokenizer(prototype, "(;)", true);
		while (st.hasMoreTokens()) {
			if (st.nextToken().equals("(")) {
				String token = st.nextToken();
				while (!token.equals(")")) {
					addReference(token);
					token = st.nextToken();
				}
				token = st.nextToken();
				addReference(token);
			}
		}
	}

	private void addReference(String token) {
		if (token.startsWith("L")) {
			String clazz = normalize(token.substring(1));
			if (clazz.startsWith("java/"))
				return;
			String pack = getPackage(clazz);
			imports.add(pack);
		}
	}

	static String normalize(String s) {
		if (s.startsWith("[L"))
			return normalize(s.substring(2));
		if (s.startsWith("["))
			if (s.length() == 2)
				return null;
			else
				return normalize(s.substring(1));
		if (s.endsWith(";"))
			return normalize(s.substring(0, s.length() - 1));
		return s + ".class";
	}

	static String getPackage(String clazz) {
		int n = clazz.lastIndexOf('/');
		if (n < 0)
			return ".";
		return clazz.substring(0, n).replace('/', '.');
	}


	public Collection getReferred() {
		return imports;
	}


	public Object getPath() {
		return path;
	}


}
