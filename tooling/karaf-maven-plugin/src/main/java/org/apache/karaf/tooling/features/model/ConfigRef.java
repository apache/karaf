package org.apache.karaf.tooling.features.model;

import java.util.Hashtable;
import java.util.Map;

public class ConfigRef {

	private String name;
	private Map<String, String> properties;
	private boolean append;

	public ConfigRef(String name, Map<String, String> hashtable, String append) {
		this.name = name;
		this.properties = hashtable;
		this.append = Boolean.parseBoolean(append);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}
}
