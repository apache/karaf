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


import java.io.IOException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

/**
 * HttpCommandProcessor sets the structure of a command processor
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */

public interface HttpCommandProcessor 
{

  /**
   * Executes an HTTP request. It assumes the request is well formed
   *
   * @param out Input request
   * @return An XML Document
   * @exception IOException
   */
  public Document executeRequest(HttpInputStream in) throws IOException, JMException;

  /**
   * Sets the target MBeanServer
   */
  public void setMBeanServer(MBeanServer server);

  /**
   * Sets the Document Builder factory
   */
  public void setDocumentBuilder(DocumentBuilder builder);

}

