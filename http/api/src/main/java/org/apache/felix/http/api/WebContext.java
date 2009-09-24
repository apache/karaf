package org.apache.felix.http.api;

import javax.servlet.ServletContext;
import javax.servlet.Servlet;
import javax.servlet.Filter;
import java.util.EventListener;
import java.util.Collection;

public interface WebContext
{
    public ServletContext getServletContext();

    public void addListener(EventListener listener);

    public void removeListener(EventListener listener);

    public Collection<EventListener> getListeners();

    public void removeServlet(Servlet servlet);

    public void removeFilter(Filter filter);

    public void addFilter(Filter filter);

    public void addServlet(Servlet servlet);

    public void addParam(String name, String value);

    public void setSessionTimeout(int timeout);
}
