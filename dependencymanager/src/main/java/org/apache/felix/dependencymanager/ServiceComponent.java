package org.apache.felix.dependencymanager;

public interface ServiceComponent {
    public static final String[] STATE_NAMES = { "unregistered", "registered" };
    public static final int STATE_UNREGISTERED = 0;
    public static final int STATE_REGISTERED = 1;
    public ServiceComponentDependency[] getComponentDependencies();
    public String getName();
    public int getState();
}
