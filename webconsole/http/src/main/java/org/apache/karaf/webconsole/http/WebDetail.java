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
