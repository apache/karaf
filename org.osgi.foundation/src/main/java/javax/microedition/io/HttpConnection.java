/*
 * $Header: /cvshome/build/ee.foundation/src/javax/microedition/io/HttpConnection.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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

package javax.microedition.io;
public abstract interface HttpConnection extends javax.microedition.io.ContentConnection {
	public abstract long getDate() throws java.io.IOException;
	public abstract long getExpiration() throws java.io.IOException;
	public abstract java.lang.String getFile();
	public abstract java.lang.String getHeaderField(int var0) throws java.io.IOException;
	public abstract java.lang.String getHeaderField(java.lang.String var0) throws java.io.IOException;
	public abstract long getHeaderFieldDate(java.lang.String var0, long var1) throws java.io.IOException;
	public abstract int getHeaderFieldInt(java.lang.String var0, int var1) throws java.io.IOException;
	public abstract java.lang.String getHeaderFieldKey(int var0) throws java.io.IOException;
	public abstract java.lang.String getHost();
	public abstract long getLastModified() throws java.io.IOException;
	public abstract int getPort();
	public abstract java.lang.String getProtocol();
	public abstract java.lang.String getQuery();
	public abstract java.lang.String getRef();
	public abstract java.lang.String getRequestMethod();
	public abstract java.lang.String getRequestProperty(java.lang.String var0);
	public abstract int getResponseCode() throws java.io.IOException;
	public abstract java.lang.String getResponseMessage() throws java.io.IOException;
	public abstract java.lang.String getURL();
	public abstract void setRequestMethod(java.lang.String var0) throws java.io.IOException;
	public abstract void setRequestProperty(java.lang.String var0, java.lang.String var1) throws java.io.IOException;
	public final static java.lang.String GET = "GET";
	public final static java.lang.String HEAD = "HEAD";
	public final static java.lang.String POST = "POST";
	public final static int HTTP_ACCEPTED = 202;
	public final static int HTTP_BAD_GATEWAY = 502;
	public final static int HTTP_BAD_METHOD = 405;
	public final static int HTTP_BAD_REQUEST = 400;
	public final static int HTTP_CLIENT_TIMEOUT = 408;
	public final static int HTTP_CONFLICT = 409;
	public final static int HTTP_CREATED = 201;
	public final static int HTTP_ENTITY_TOO_LARGE = 413;
	public final static int HTTP_EXPECT_FAILED = 417;
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
	public final static int HTTP_TEMP_REDIRECT = 307;
	public final static int HTTP_UNAUTHORIZED = 401;
	public final static int HTTP_UNAVAILABLE = 503;
	public final static int HTTP_UNSUPPORTED_RANGE = 416;
	public final static int HTTP_UNSUPPORTED_TYPE = 415;
	public final static int HTTP_USE_PROXY = 305;
	public final static int HTTP_VERSION = 505;
}

