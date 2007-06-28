/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http;

/**
 * Define constants for the HTTP request processing
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public class HttpConstants
{
  /**
   * Server info header
   */
  public final static String SERVER_INFO = "MX4J-HTTPD/1.0";

  /**
   * HTTP implemented version
   */
  public final static String HTTP_VERSION = "HTTP/1.0 ";

  /**
   * Get method header
   */
  public final static String METHOD_GET = "GET";

  /**
   * Post method header
   */
  public final static String METHOD_POST = "POST";

  /**
   * Status code OK
   */
  public final static int STATUS_OKAY = 200;

  /**
   * Status code NO CONTENT
   */
  public final static int STATUS_NO_CONTENT = 204;

  /**
   * Status code MOVED PERMANENTLY
   */
  public final static int STATUS_MOVED_PERMANENTLY = 301;

  /**
   * Status code MOVED TEMPORARILY
   */
  public final static int STATUS_MOVED_TEMPORARILY = 302;

  /**
   * Status code BAD REQUEST
   */
  public final static int STATUS_BAD_REQUEST = 400;

  /**
   * Status code AUTHENTICATE
   */
  public final static int STATUS_AUTHENTICATE = 401;

  /**
   * Status code FORBIDDEN
   */
  public final static int STATUS_FORBIDDEN = 403;

  /**
   * Status code NOT FOUND
   */
  public final static int STATUS_NOT_FOUND = 404;

  /**
   * Status code NOT ALLOWED
   */
  public final static int STATUS_NOT_ALLOWED = 405;

  /**
   * Status code INTERNAL ERROR
   */
  public final static int STATUS_INTERNAL_ERROR = 500;

  /**
   * Status code NOT IMPLEMENTED
   */
  public final static int STATUS_NOT_IMPLEMENTED = 501;

}
