package org.apache.karaf.tooling.utils;

public class LocalDependency {
	private String scope;
	private Object artifact;
	private Object parent;
	
	LocalDependency(final String scope, final Object artifact, Object parent) {
		this.scope = scope;
		this.artifact = artifact;
		this.parent = parent;
	}

	public String getScope() {
		return scope;
	}

	public Object getArtifact() {
		return artifact;
	}

	public Object getParent() {
		return parent;
	}
	
	@Override
	public int hashCode() {
		return artifact.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return artifact.equals(obj);
	}
}
