/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.utils;

import java.util.Objects;

public class LocalDependency {
	private String scope;
	private boolean isTransitive;
	private Object artifact;
	private Object parent;
	
	LocalDependency(final String scope, final boolean isTransitive, final Object artifact, Object parent) {
		this.scope = scope;
		this.isTransitive = isTransitive;
		this.artifact = artifact;
		this.parent = parent;
	}

	public String getScope() {
		return scope;
	}

	public boolean isTransitive() {
		return isTransitive;
	}

	public Object getArtifact() {
		return artifact;
	}

	public Object getParent() {
		return parent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocalDependency that = (LocalDependency) o;
		return Objects.equals(scope, that.scope) &&
				Objects.equals(artifact, that.artifact) &&
				Objects.equals(parent, that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scope, artifact, parent);
	}
}
