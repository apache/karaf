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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ServerCommandProcessor, processes a request for getting all the
 * MBeans of the current server
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ServerCommandProcessor extends HttpCommandProcessorAdaptor
{
  public ServerCommandProcessor()
  {
  }

  public Document executeRequest(HttpInputStream in) throws IOException, JMException
  {
    Document document = builder.newDocument();

    Element root = document.createElement("Server");
    document.appendChild(root);

    String classVariable = in.getVariable("instanceof");
    String queryNames = in.getVariable("querynames");
    Set mbeans = null;
    ObjectName query = null;
    if (queryNames != null)
    {
      try
      {
        query = new ObjectName(queryNames);
        mbeans = new TreeSet(CommandProcessorUtil.createObjectInstanceComparator());
        mbeans.addAll(server.queryMBeans(query, null));
      }
      catch (MalformedObjectNameException e)
      {
        Element exceptionElement = document.createElement("Exception");
        exceptionElement.setAttribute("errorMsg", e.getMessage());
        root.appendChild(exceptionElement);
        return document;
      }
    } else {
      mbeans = new TreeSet(CommandProcessorUtil.createObjectInstanceComparator());
      mbeans.addAll(server.queryMBeans(null, null));
    }
    Iterator i = mbeans.iterator();
    while (i.hasNext())
    {
      ObjectInstance instance = (ObjectInstance)i.next();
      if (classVariable != null && !classVariable.equals(instance.getClassName()))
      {
        continue;
      }
      Element mBeanElement = document.createElement("MBean");
      mBeanElement.setAttribute("objectname", instance.getObjectName().toString());
      mBeanElement.setAttribute("classname", instance.getClassName());
      MBeanInfo info = server.getMBeanInfo(instance.getObjectName());
      mBeanElement.setAttribute("description", info.getDescription());
      root.appendChild(mBeanElement);
    }
    return document;
  }

}
