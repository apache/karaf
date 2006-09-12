package org.apache.felix.mishell;

public class EngineNotFoundException extends Exception {

	public EngineNotFoundException(String language) {
		super(language);
	}

}
