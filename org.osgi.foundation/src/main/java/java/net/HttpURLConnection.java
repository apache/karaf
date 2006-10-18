/*
 * $Header: /cvshome/build/ee.foundation/src/java/net/HttpURLConnection.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
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

package java.net;
public abstract class HttpURLConnection extends java.net.URLConnection {
	protected HttpURLConnection(java.net.URL var0) { super((java.net.URL) null); }
	public abstract void disconnect();
	public java.io.InputStream getErrorStream() { return null; }
	public static boolean getFollowRedirects() { return false; }
	public java.security.Permission getPermission() throws java.io.IOException { return null; }
	public java.lang.String getRequestMethod() { return null; }
	public int getResponseCode() throws java.io.IOException { return 0; }
	public java.lang.String getResponseMessage() throws java.io.IOException { return null; }
	public static void setFollowRedirects(boolean var0) { }
	public void setRequestMethod(java.lang.String var0) throws java.net.ProtocolException { }
	public abstract boolean usingProxy();
	public boolean getInstanceFollowRedirects() { return false; }
	public void setInstanceFollowRedirects(boolean var0) { }
	public long getHeaderFieldDate(java.lang.String var0, long var1) { return 0l; }
	protected java.lang.String method;
	protected int responseCode;
	protected java.lang.String responseMessage;
	protected boolean instanceFollowRedirects;
	public final static int HTTP_ACCEPTED = 202;
	public final static int HTTP_BAD_GATEWAY = 502;
	public final static int HTTP_BAD_METHOD = 405;
	public final static int HTTP_BAD_REQUEST = 400;
	public final static int HTTP_CLIENT_TIMEOUT = 408;
	public final static int HTTP_CONFLICT = 409;
	public final static int HTTP_CREATED = 201;
	public final static int HTTP_ENTITY_TOO_LARGE = 413;
	public final static int HTTP_FORBIDDEN = 403;
	public final static int HTTP_GATEWAY_TIMEOUT = 504;
	public final static int HTTP_GONE = 410;
	public final static int HTTP_INTERNAL_ERROR = 500;
	public final static int HTTP_LENGTH_REQUIRED = 411;
	public final static int HTTP_MOVED_PERM = 301;
	public final static int HTTP_MOVED_TEMP = 302;
	public final static int HTTP_MULT_CHOICE = 300;
	public final static int HTTP_NO_CONTENT = 204;
	public final static int HTTP_NOT_ACCEPTABLE = 406;
	public final static int HTTP_NOT_AUTHORITATIVE = 203;
	public final static int HTTP_NOT_FOUND = 404;
	public final static int HTTP_NOT_IMPLEMENTED = 501;
	public final static int HTTP_NOT_MODIFIED = 304;
	public final static int HTTP_OK = 200;
	public final static int HTTP_PARTIAL = 206;
	public final static int HTTP_PAYMENT_REQUIRED = 402;
	public final static int HTTP_PRECON_FAILED = 412;
	public final static int HTTP_PROXY_AUTH = 407;
	public final static int HTTP_REQ_TOO_LONG = 414;
	public final static int HTTP_RESET = 205;
	public final static int HTTP_SEE_OTHER = 303;
	public final static int HTTP_USE_PROXY = 305;
	public final static int HTTP_UNAUTHORIZED = 401;
	public final static int HTTP_UNSUPPORTED_TYPE = 415;
	public final static int HTTP_UNAVAILABLE = 503;
	public final static int HTTP_VERSION = 505;
}

