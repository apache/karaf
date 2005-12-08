/*
 * Copyright 1999,2005 The Apache Software Foundation.
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


package javax.servlet.http;

import javax.servlet.ServletRequest;
import java.util.Enumeration;

/**
 *
 * Extends the {@link javax.servlet.ServletRequest} interface
 * to provide request information for HTTP servlets.
 *
 * <p>The servlet container creates an <code>HttpServletRequest</code>
 * object and passes it as an argument to the servlet's service
 * methods (<code>doGet</code>, <code>doPost</code>, etc).
 *
 *
 * @author 	Various
 * @version	$Version$
 *
 *
 */

public interface HttpServletRequest extends ServletRequest {

    /**
     * Returns the name of the authentication scheme used to protect
     * the servlet, for example, "BASIC" or "SSL".
     * If the servlet is not authenticated <code>null</code> is returned.
     *
     * <p>Same as the value of the CGI variable AUTH_TYPE.
     *
     *
     * @return		a <code>String</code> specifying the name of
     *			the authentication scheme, or
     *			<code>null</code> if the request was not
     *			authenticated
     *
     */

    public String getAuthType();




    /**
     *
     * Returns an array containing all of the <code>Cookie</code>
     * objects the client sent with this request.
     * This method returns <code>null</code> if no cookies were sent.
     *
     * @return		an array of all the <code>Cookies</code>
     *			included with this request, or <code>null</code>
     *			if the request has no cookies
     *
     *
     */

    public Cookie[] getCookies();




    /**
     *
     * Returns the value of the specified request header
     * as a <code>long</code> value that represents a
     * <code>Date</code> object. Use this method with
     * headers that contain dates, such as
     * <code>If-Modified-Since</code>.
     *
     * <p>The date is returned as
     * the number of milliseconds since January 1, 1970 GMT.
     * The header name is case insensitive.
     *
     * <p>If the request did not have a header of the
     * specified name, this method returns -1. If the header
     * can't be converted to a date, the method throws
     * an <code>IllegalArgumentException</code>.
     *
     * @param name		a <code>String</code> specifying the
     *				name of the header
     *
     * @return			a <code>long</code> value
     *				representing the date specified
     *				in the header expressed as
     *				the number of milliseconds
     *				since January 1, 1970 GMT,
     *				or -1 if the named header
     *				was not included with the
     *				reqest
     *
     * @exception	IllegalArgumentException	If the header value
     *							can't be converted
     *							to a date
     *
     */

    public long getDateHeader(String name);




    /**
     *
     * Returns the value of the specified request header
     * as a <code>String</code>. If the request did not include a header
     * of the specified name, this method returns <code>null</code>.
     * The header name is case insensitive. You can use
     * this method with any request header.
     *
     * @param name		a <code>String</code> specifying the
     *				header name
     *
     * @return			a <code>String</code> containing the
     *				value of the requested
     *				header, or <code>null</code>
     *				if the request does not
     *				have a header of that name
     *
     */			

    public String getHeader(String name);




    /**
     *
     * Returns an enumeration of all the header names
     * this request contains. If the request has no
     * headers, this method returns an empty enumeration.
     *
     * <p>Some servlet containers do not allow do not allow
     * servlets to access headers using this method, in
     * which case this method returns <code>null</code>
     *
     * @return			an enumeration of all the
     *				header names sent with this
     *				request; if the request has
     *				no headers, an empty enumeration;
     *				if the servlet container does not
     *				allow servlets to use this method,
     *				<code>null</code>
     *				
     *
     */

    public Enumeration getHeaderNames();




    /**
     *
     * Returns the value of the specified request header
     * as an <code>int</code>. If the request does not have a header
     * of the specified name, this method returns -1. If the
     * header cannot be converted to an integer, this method
     * throws a <code>NumberFormatException</code>.
     *
     * <p>The header name is case insensitive.
     *
     * @param name		a <code>String</code> specifying the name
     *				of a request header
     *
     * @return			an integer expressing the value
     * 				of the request header or -1
     *				if the request doesn't have a
     *				header of this name
     *
     * @exception	NumberFormatException		If the header value
     *							can't be converted
     *							to an <code>int</code>
     */

    public int getIntHeader(String name);




    /**
     *
     * Returns the name of the HTTP method with which this
     * request was made, for example, GET, POST, or PUT.
     * Same as the value of the CGI variable REQUEST_METHOD.
     *
     * @return			a <code>String</code>
     *				specifying the name
     *				of the method with which
     *				this request was made
     *
     */

    public String getMethod();




    /**
     *
     * Returns any extra path information associated with
     * the URL the client sent when it made this request.
     * The extra path information follows the servlet path
     * but precedes the query string.
     * This method returns <code>null</code> if there
     * was no extra path information.
     *
     * <p>Same as the value of the CGI variable PATH_INFO.
     *
     *
     * @return		a <code>String</code>, decoded by the
     *			web container, specifying
     *			extra path information that comes
     *			after the servlet path but before
     *			the query string in the request URL;
     *			or <code>null</code> if the URL does not have
     *			any extra path information
     *
     */

    public String getPathInfo();




    /**
     *
     * Returns any extra path information after the servlet name
     * but before the query string, and translates it to a real
     * path. Same as the value of the CGI variable PATH_TRANSLATED.
     *
     * <p>If the URL does not have any extra path information,
     * this method returns <code>null</code>.
     *
     * The web container does not decode this string.
     *
     *
     * @return		a <code>String</code> specifying the
     *			real path, or <code>null</code> if
     *			the URL does not have any extra path
     *			information
     *
     *
     */

    public String getPathTranslated();




    /**
     *
     * Returns the query string that is contained in the request
     * URL after the path. This method returns <code>null</code>
     * if the URL does not have a query string. Same as the value
     * of the CGI variable QUERY_STRING.
     *
     * @return		a <code>String</code> containing the query
     *			string or <code>null</code> if the URL
     *			contains no query string. The value is not
     *			decoded by the container.
     *
     */

    public String getQueryString();




    /**
     *
     * Returns the login of the user making this request, if the
     * user has been authenticated, or <code>null</code> if the user
     * has not been authenticated.
     * Whether the user name is sent with each subsequent request
     * depends on the browser and type of authentication. Same as the
     * value of the CGI variable REMOTE_USER.
     *
     * @return		a <code>String</code> specifying the login
     *			of the user making this request, or <code>null</code
     *			if the user login is not known
     *
     */

    public String getRemoteUser();




    /**
     *
     * Returns the session ID specified by the client. This may
     * not be the same as the ID of the actual session in use.
     * For example, if the request specified an old (expired)
     * session ID and the server has started a new session, this
     * method gets a new session with a new ID. If the request
     * did not specify a session ID, this method returns
     * <code>null</code>.
     *
     *
     * @return		a <code>String</code> specifying the session
     *			ID, or <code>null</code> if the request did
     *			not specify a session ID
     *
     * @see		#isRequestedSessionIdValid
     *
     */

    public String getRequestedSessionId();




    /**
     *
     * Returns the part of this request's URL from the protocol
     * name up to the query string in the first line of the HTTP request.
     * The web container does not decode this String.
     * For example:
     *
     *

     * <table>
     * <tr align=left><th>First line of HTTP request      </th>
     * <th>     Returned Value</th>
     * <tr><td>POST /some/path.html HTTP/1.1<td><td>/some/path.html
     * <tr><td>GET http://foo.bar/a.html HTTP/1.0
     * <td><td>/a.html
     * <tr><td>HEAD /xyz?a=b HTTP/1.1<td><td>/xyz
     * </table>
     *
     * <p>To reconstruct an URL with a scheme and host, use
     * {@link HttpUtils#getRequestURL}.
     *
     * @return		a <code>String</code> containing
     *			the part of the URL from the
     *			protocol name up to the query string
     *
     * @see		HttpUtils#getRequestURL
     *
     */

    public String getRequestURI();

    /**
     *
     * Returns the part of this request's URL that calls
     * the servlet. This includes either the servlet name or
     * a path to the servlet, but does not include any extra
     * path information or a query string. Same as the value
     * of the CGI variable SCRIPT_NAME.
     *
     *
     * @return		a <code>String</code> containing
     *			the name or path of the servlet being
     *			called, as specified in the request URL,
     *			decoded.
     *
     *
     */

    public String getServletPath();




    /**
     *
     * Returns the current <code>HttpSession</code>
     * associated with this request or, if if there is no
     * current session and <code>create</code> is true, returns
     * a new session.
     *
     * <p>If <code>create</code> is <code>false</code>
     * and the request has no valid <code>HttpSession</code>,
     * this method returns <code>null</code>.
     *
     * <p>To make sure the session is properly maintained,
     * you must call this method before
     * the response is committed. If the container is using cookies
     * to maintain session integrity and is asked to create a new session
     * when the response is committed, an IllegalStateException is thrown.
     *
     *
     *
     *
     * @param	create	<code>true</code> to create
     *			a new session for this request if necessary;
     *			<code>false</code> to return <code>null</code>
     *			if there's no current session
     *			
     *
     * @return 		the <code>HttpSession</code> associated
     *			with this request or <code>null</code> if
     * 			<code>create</code> is <code>false</code>
     *			and the request has no valid session
     *
     * @see	#getSession()
     *
     *
     */

    public HttpSession getSession(boolean create);





    /**
     *
     * Returns the current session associated with this request,
     * or if the request does not have a session, creates one.
     *
     * @return		the <code>HttpSession</code> associated
     *			with this request
     *
     * @see	#getSession(boolean)
     *
     */

    public HttpSession getSession();






    /**
     *
     * Checks whether the requested session ID is still valid.
     *
     * @return			<code>true</code> if this
     *				request has an id for a valid session
     *				in the current session context;
     *				<code>false</code> otherwise
     *
     * @see			#getRequestedSessionId
     * @see			#getSession(boolean)
     * @see			HttpSessionContext
     *
     */

    public boolean isRequestedSessionIdValid();




    /**
     *
     * Checks whether the requested session ID came in as a cookie.
     *
     * @return			<code>true</code> if the session ID
     *				came in as a
     *				cookie; otherwise, <code>false</code>
     *
     *
     * @see			#getSession(boolean)
     *
     */

    public boolean isRequestedSessionIdFromCookie();




    /**
     *
     * Checks whether the requested session ID came in as part of the
     * request URL.
     *
     * @return			<code>true</code> if the session ID
     *				came in as part of a URL; otherwise,
     *				<code>false</code>
     *
     *
     * @see			#getSession(boolean)
     *
     */

    public boolean isRequestedSessionIdFromURL();





    /**
     *
     * @deprecated		As of Version 2.1 of the Java Servlet
     *				API, use {@link #isRequestedSessionIdFromURL}
     *				instead.
     *
     */

    public boolean isRequestedSessionIdFromUrl();



}
