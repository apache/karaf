package org.apache.karaf.http.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.karaf.http.core.ServletInfo;
import org.apache.karaf.http.core.ServletService;
import org.ops4j.pax.web.service.spi.ServletEvent;

public class ServletServiceImpl implements ServletService {
    private ServletEventHandler servletEventHandler;

    public ServletServiceImpl(ServletEventHandler servletEventHandler) {
        this.servletEventHandler = servletEventHandler;
    }

    @Override
    public List<ServletInfo> getServlets() {
        List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
        Collection<ServletEvent> events = servletEventHandler.getServletEvents();
        for (ServletEvent event : events) {
            Servlet servlet = event.getServlet();
            String servletClassName = " ";
            if (servlet != null) {
                servletClassName = servlet.getClass().getName();
                servletClassName = servletClassName.substring(servletClassName.lastIndexOf(".") + 1,
                                                              servletClassName.length());
            }
            String servletName = event.getServletName() != null ? event.getServletName() : " ";
            if (servletName.contains(".")) {
                servletName = servletName.substring(servletName.lastIndexOf(".") + 1, servletName.length());
            }

            String alias = event.getAlias() != null ? event.getAlias() : " ";

            String[] urls = (String[])(event.getUrlParameter() != null ? event.getUrlParameter() : new String[] {""});
            ServletInfo info = new ServletInfo();
            info.setBundle(event.getBundle());
            info.setName(servletName);
            info.setClassName(servletClassName);
            info.setState(event.getType());
            info.setAlias(alias);
            info.setUrls(urls);
            servletInfos.add(info);
        }
        return servletInfos;
    }

}
