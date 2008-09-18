package org.apache.felix.dependencymanager;

public interface ServiceComponentDependency {
    public static final String[] STATE_NAMES = { "unavailable optional", "available optional", "unavailable required", "available required" };
    public static final int STATE_UNAVAILABLE_OPTIONAL = 0;
    public static final int STATE_AVAILABLE_OPTIONAL = 1;
    public static final int STATE_UNAVAILABLE_REQUIRED = 2;
    public static final int STATE_AVAILABLE_REQUIRED = 3;
    public String getName();
    public String getType();
    public int getState();
}
