package org.apache.karaf.shell.commands.impl.info;

import java.util.Properties;

import org.apache.karaf.shell.commands.InfoProvider;

public class PojoInfoProvider implements InfoProvider {

	private String name;
	private Properties properties;

	public PojoInfoProvider() {
	}

	public PojoInfoProvider(String name, Properties properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
