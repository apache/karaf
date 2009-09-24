package org.apache.felix.http.api;

public interface WebContextManager
{
    public WebContext getDefaultContext();

    public WebContext createContext(String contextName);

    public void removeContext(String contextName);
}
