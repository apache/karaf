package org.osgi.impl.bundle.obr.resource;

import java.util.*;

import org.osgi.impl.bundle.obr.resource.VersionImpl;


public class ManifestEntry implements Comparable {
	String		name;
	VersionImpl	version;
	Map			attributes;
	public Map	directives;
	public Set	uses;

	public ManifestEntry(String name) {
		this.name = name;
	}

	public ManifestEntry(String name, VersionImpl version) {
		this.name = name;
		this.version = version;
	}

	public String toString() {
		if (version == null)
			return name;
		return name + " ;version=" + version;
	}

	public String getName() {
		return name;
	}

	public VersionImpl getVersion() {
		if (version != null)
			return version;
		return new VersionImpl("0");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		ManifestEntry p = (ManifestEntry) o;
		return name.compareTo(p.name);
	}

	/**
	 * @return
	 */
	public Object getPath() {
		return getName().replace('.', '/');
	}

	public Map getDirectives() {
		return directives;
	}

	public Map getAttributes() {
		return attributes;
	}

	/**
	 * @param parameter
	 */
	public void addParameter(Parameter parameter) {
		switch (parameter.type) {
			case Parameter.ATTRIBUTE :
				if (attributes == null)
					attributes = new HashMap();
				attributes.put(parameter.key, parameter.value);
				if (parameter.key.equalsIgnoreCase("version")
						|| parameter.key
								.equalsIgnoreCase("specification-version"))
					this.version = new VersionImpl(parameter.value);
				break;

			case Parameter.DIRECTIVE :
				if (directives == null)
					directives = new HashMap();
				directives.put(parameter.key, parameter.value);
				break;
		}
	}

	public ManifestEntry getAlias(String key) {
		ManifestEntry me = new ManifestEntry(key);
		me.attributes = attributes;
		me.directives = directives;
		me.version = version;
		return me;
	}

}
