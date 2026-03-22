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
package org.apache.karaf.http.core.internal.proxy;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.cookie.SM;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.http.core.BalancingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.HttpCookie;
import java.net.URI;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;

/**
 * This is a simple servlet acting as a HTTP reverse proxy/gateway. It works with any webcontainer as it's a regular servlet.
 */
public class ProxyServlet extends HttpServlet {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyServlet.class);

    protected String proxyTo;
    protected boolean doForwardIP = true;
    protected boolean doSendUrlFragment = true;
    protected BalancingPolicy balancingPolicy;

    private HttpClient proxyClient;

    public void setIPForwarding(boolean ipForwarding) {
        this.doForwardIP = ipForwarding;
    }

    public void setProxyTo(String proxyTo) {
        this.proxyTo = proxyTo;
    }

    public void setBalancingPolicy(BalancingPolicy balancingPolicy) {
        this.balancingPolicy = balancingPolicy;
    }

    @Override
    public String getServletInfo() {
        return "Apache Karaf Proxy Servlet";
    }

    @Override
    public void init() throws ServletException {
        HttpParams hcParams = new BasicHttpParams();
        hcParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        proxyClient = createHttpClient(hcParams);
    }

    protected HttpClient createHttpClient(HttpParams hcParams) {
        try {
            Class clientClazz = Class.forName("org.apache.http.impl.client.SystemDefaultHttpClient");
            Constructor constructor = clientClazz.getConstructor(HttpParams.class);
            return (HttpClient) constructor.newInstance(hcParams);
        } catch (ClassNotFoundException e) {
            // not a problem, we fallback on the "old" client
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // fallback on using "old" client style
        return new DefaultHttpClient(new ThreadSafeClientConnManager(), hcParams);
    }

    @Override
    public void destroy() {
        // starting for HttpComponents 4.3, clients implements Closeable
        if (proxyClient instanceof Closeable) {
            try {
                ((Closeable) proxyClient).close();
            } catch (IOException e) {
                log("Error occurred when closing HTTP client in the proxy servlet destroy", e);
            }
        } else {
            // older HttpComponents version requires to close the client
            if (proxyClient != null) {
                proxyClient.getConnectionManager().shutdown();
            }
        }
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        String[] proxyTos = proxyTo.split(",");
        String actualProxy;
        if (balancingPolicy != null) {
            actualProxy = balancingPolicy.selectHost(proxyTos);
        } else {
            actualProxy = proxyTos[0];
        }

        URI locationUri = URI.create(actualProxy);
        HttpHost host = URIUtils.extractHost(locationUri);

        LOGGER.debug("Proxy to {} (host {})", locationUri, host);

        String method = servletRequest.getMethod();
        String proxyRequestUri = rewriteUrlFromRequest(servletRequest, actualProxy);
        HttpRequest proxyRequest;

        // spec: RFC 2616, sec 4.3: either of these two headers means there is a message body
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            HttpEntityEnclosingRequest entityProxyRequest = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
            entityProxyRequest.setEntity(new InputStreamEntity(servletRequest.getInputStream(), servletRequest.getContentLength()));
            proxyRequest = entityProxyRequest;
        } else {
            proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
        }

        copyRequestHeaders(servletRequest, proxyRequest, host);

        setXForwardedForHeader(servletRequest, proxyRequest);

        HttpResponse proxyResponse = null;
        try {
            // execute the request
            proxyResponse = proxyClient.execute(host, proxyRequest);

            // process the response
            int statusCode = proxyResponse.getStatusLine().getStatusCode();

            // copying response headers to make sure SESSIONID or other Cookie which comes from the remote host
            // will be saved in client when the proxied URL was redirect to another one.
            copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

            if (doResponseRedirect(servletRequest, servletResponse, proxyResponse, statusCode, actualProxy)) {
                // the response is already "committed" now without any body to send
                return;
            }

            // pass the response code
            servletResponse.setStatus(statusCode);

            // send the content to the client
            copyResponseEntity(proxyResponse, servletResponse);
        } catch (Exception e) {
            // abort request
            if (proxyRequest instanceof AbortableHttpRequest) {
                AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
                abortableHttpRequest.abort();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof ServletException) {
                throw (ServletException) e;
            }
            throw new RuntimeException(e);
        } finally {
            if (proxyResponse != null) {
                consumeQuietly(proxyResponse.getEntity());
            }
        }
    }

    protected boolean doResponseRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse, HttpResponse proxyResponse, int statusCode, String proxyTo) throws ServletException, IOException {
        // check if the proxy is a redirect
        if (statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES && statusCode < HttpServletResponse.SC_NOT_MODIFIED) {
            Header locationHeader = proxyResponse.getLastHeader(HttpHeaders.LOCATION);
            if (locationHeader == null) {
                throw new ServletException("Received a redirect (" + statusCode + ") but without location (" + HttpHeaders.LOCATION + " header)");
            }
            // modify the redirect to go to this proxy servlet rather than the proxied host
            String locationString = rewriteUrlFromResponse(servletRequest, locationHeader.getValue(), proxyTo);
            servletResponse.sendRedirect(locationString);
            return true;
        }
        // 304 needs special handling.  See:
        // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
        // We get a 304 whenever passed an 'If-Modified-Since'
        // header and the data on disk has not changed; server
        // responds w/ a 304 saying I'm not going to send the
        // body because the file has not changed.
        if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
            servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
            servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        return false;
    }

    protected void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log(e.getMessage(), e);
        }
    }

    protected void consumeQuietly(HttpEntity httpEntity) {
        try {
            EntityUtils.consume(httpEntity);
        } catch (IOException e) {
            log(e.getMessage(), e);
        }
    }

    /**
     * These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * We use an HttpClient HeaderGroup class instead of Set of String because this
     * approach does case insensitive lookup faster.
     */
    protected static final HeaderGroup hopByHopHeaders;

    static {
        hopByHopHeaders = new HeaderGroup();
        String[] headers = new String[] {"Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers", "Transfer-Encoding", "Upgrade"};
        for (String header : headers) {
            hopByHopHeaders.addHeader(new BasicHeader(header, null));
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     */
    protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest, HttpHost host) {
        Enumeration headerNames = servletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            // instead the content-length is effectively set via InputStreamEntity
            if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
            if (hopByHopHeaders.containsHeader(headerName)) {
                continue;
            }

            Enumeration headers = servletRequest.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                String headerValue = (String) headers.nextElement();
                // in case the proxy host is running multiple virtual servers, rewrite the Host header to guarantee we get content from the correct virtual server
                if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                    headerValue = host.getHostName();
                    if (host.getPort() != -1) {
                        headerValue += ":" + host.getPort();
                    }
                } else if (headerName.equalsIgnoreCase(SM.COOKIE)) {
                    headerValue = getRealCookie(headerValue);
                }
                proxyRequest.addHeader(headerName, headerValue);
            }
        }
    }

    private void setXForwardedForHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
        if (doForwardIP) {
            String newHeader = servletRequest.getRemoteAddr();
            String existingHeader = servletRequest.getHeader("X-Forwarded-For");
            if (existingHeader != null) {
                newHeader = existingHeader + ", " + newHeader;
            }
            proxyRequest.setHeader("X-Forwarded-For", newHeader);
        }
    }

    protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        for (Header header : proxyResponse.getAllHeaders()) {
            if (hopByHopHeaders.containsHeader(header.getName())) {
                continue;
            }
            if (header.getName().equalsIgnoreCase(SM.SET_COOKIE) || header.getName().equalsIgnoreCase(SM.SET_COOKIE2)) {
                copyProxyCookie(servletRequest, servletResponse, header);
            } else {
                servletResponse.setHeader(header.getName(), header.getValue());
            }
        }
    }

    /**
     * Copy cookie from the proxy to the servlet client.
     * Replaces cookie path to local path and renames cookie to avoid collisions.
     */
    protected void copyProxyCookie(HttpServletRequest servletRequest,
                                   HttpServletResponse servletResponse, Header header) {
        List<HttpCookie> cookies = HttpCookie.parse(header.getValue());
        String path = servletRequest.getContextPath(); // path starts with / or is empty string
        path += servletRequest.getServletPath(); // servlet path starts with / or is empty string

        for (HttpCookie cookie : cookies) {
            //set cookie name prefixed w/ a proxy value so it won't collide w/ other cookies
            String proxyCookieName = getCookieNamePrefix() + cookie.getName();
            Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
            servletCookie.setComment(cookie.getComment());
            servletCookie.setMaxAge((int) cookie.getMaxAge());
            servletCookie.setPath(path); //set to the path of the proxy servlet
            // don't set cookie domain
            servletCookie.setSecure(cookie.getSecure());
            servletCookie.setVersion(cookie.getVersion());
            servletResponse.addCookie(servletCookie);
        }
    }

    /**
     * Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy.  This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4.
     * This also blocks any local cookies from being sent to the proxy.
     */
    protected String getRealCookie(String cookieValue) {
        StringBuilder escapedCookie = new StringBuilder();
        String cookies[] = cookieValue.split("; ");
        for (String cookie : cookies) {
            String cookieSplit[] = cookie.split("=");
            if (cookieSplit.length == 2) {
                String cookieName = cookieSplit[0];
                if (cookieName.startsWith(getCookieNamePrefix())) {
                    cookieName = cookieName.substring(getCookieNamePrefix().length());
                    if (escapedCookie.length() > 0) {
                        escapedCookie.append("; ");
                    }
                    escapedCookie.append(cookieName).append("=").append(cookieSplit[1]);
                }
            }

            cookieValue = escapedCookie.toString();
        }
        return cookieValue;
    }

    /**
     * The string prefixing rewritten cookies.
     */
    protected String getCookieNamePrefix() {
        return "!Proxy!" + getServletConfig().getServletName();
    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     */
    protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            OutputStream servletOutputStream = servletResponse.getOutputStream();
            entity.writeTo(servletOutputStream);
        }
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri.
     * It's used to make the new request.
     */
    protected String rewriteUrlFromRequest(HttpServletRequest servletRequest, String location) {
        StringBuilder uri = new StringBuilder(500);
        uri.append(location);
        // Handle the path given to the servlet
        if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
            uri.append(encodeUriQuery(servletRequest.getPathInfo()));
        }
        // Handle the query string & fragment
        String queryString = servletRequest.getQueryString();//ex:(following '?'): name=value&foo=bar#fragment
        String fragment = null;
        //split off fragment from queryString, updating queryString if found
        if (queryString != null) {
            int fragIdx = queryString.indexOf('#');
            if (fragIdx >= 0) {
                fragment = queryString.substring(fragIdx + 1);
                queryString = queryString.substring(0, fragIdx);
            }
        }

        queryString = rewriteQueryStringFromRequest(servletRequest, queryString);
        if (queryString != null && queryString.length() > 0) {
            uri.append('?');
            uri.append(encodeUriQuery(queryString));
        }

        if (doSendUrlFragment && fragment != null) {
            uri.append('#');
            uri.append(encodeUriQuery(fragment));
        }
        return uri.toString();
    }

    protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return queryString;
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    protected String rewriteUrlFromResponse(HttpServletRequest servletRequest, String theUrl, String location) {
        if (theUrl.startsWith(location)) {
            String curUrl = servletRequest.getRequestURL().toString(); // no query
            String pathInfo = servletRequest.getPathInfo();
            if (pathInfo != null) {
                assert curUrl.endsWith(pathInfo);
                curUrl = curUrl.substring(0, curUrl.length() - pathInfo.length()); // take pathInfo off
            }
            theUrl = curUrl + theUrl.substring(location.length());
        }
        return theUrl;
    }

    /**
     * Encodes characters in the query or fragment part of the URI.
     *
     * <p>Unfortunately, an incoming URI sometimes has characters disallowed by the spec.  HttpClient
     * insists that the outgoing proxied request has a valid URI because it uses Java's {@link URI}.
     * To be more forgiving, we must escape the problematic characters.  See the URI class for the
     * spec.</p>
     *
     * @param in The {@link CharSequence} to encode.
     */
    protected static CharSequence encodeUriQuery(CharSequence in) {
        //Note that I can't simply use URI.java to encode because it will escape pre-existing escaped things.
        StringBuilder outBuf = null;
        Formatter formatter = null;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            boolean escape = true;
            if (c < 128) {
                if (asciiQueryChars.get(c)) {
                    escape = false;
                }
            } else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {//not-ascii
                escape = false;
            }
            if (!escape) {
                if (outBuf != null)
                    outBuf.append(c);
            } else {
                //escape
                if (outBuf == null) {
                    outBuf = new StringBuilder(in.length() + 5 * 3);
                    outBuf.append(in, 0, i);
                    formatter = new Formatter(outBuf);
                }
                //leading %, 0 padded, width 2, capital hex
                formatter.format("%%%02X", (int) c);//TODO
            }
        }
        return outBuf != null ? outBuf : in;
    }

    protected static final BitSet asciiQueryChars;

    static {
        char[] c_unreserved = "_-!.~'()*".toCharArray();//plus alphanum
        char[] c_punct = ",;:$&+=".toCharArray();
        char[] c_reserved = "?/[]@".toCharArray();//plus punct

        asciiQueryChars = new BitSet(128);
        for (char c = 'a'; c <= 'z'; c++) asciiQueryChars.set(c);
        for (char c = 'A'; c <= 'Z'; c++) asciiQueryChars.set(c);
        for (char c = '0'; c <= '9'; c++) asciiQueryChars.set(c);
        for (char c : c_unreserved) asciiQueryChars.set(c);
        for (char c : c_punct) asciiQueryChars.set(c);
        for (char c : c_reserved) asciiQueryChars.set(c);

        asciiQueryChars.set('%');//leave existing percent escapes in place
    }

}
