/*
 * Copyright 2011 The Apache Software Foundation.
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
package org.apache.karaf.webconsole.http;

public class WebDetail {

	private long bundleId;
	private String contextPath;
	private String state;
	private String webState;

	/**
	 * @return the bundleId
	 */
	public long getBundleId() {
		return bundleId;
	}

	/**
	 * @param bundleId the bundleId to set
	 */
	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * @return the contextPath
	 */
	public String getContextPath() {
		return contextPath;
	}

	/**
	 * @param contextPath the contextPath to set
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the webState
	 */
	public String getWebState() {
		return webState;
	}

	/**
	 * @param webState the webState to set
	 */
	public void setWebState(String webState) {
		this.webState = webState;
	}

}
