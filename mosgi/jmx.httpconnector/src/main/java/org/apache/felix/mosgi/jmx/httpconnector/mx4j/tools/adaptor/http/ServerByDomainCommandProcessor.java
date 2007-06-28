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

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.JMException;
import javax.management.MBeanInfo;
import java.io.IOException;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ServerByDomainCommandProcessor, processes a request for getting all the
 * MBeans of the current server grouped by domains
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ServerByDomainCommandProcessor extends HttpCommandProcessorAdaptor
{
  public ServerByDomainCommandProcessor()
  {
  }

  public Document executeRequest(HttpInputStream in) throws IOException, JMException
  {
    Document document = builder.newDocument();

    Element root = document.createElement("Server");
    document.appendChild(root);

    String targetClass = in.getVariable("instanceof");
    String queryNames = in.getVariable("querynames");
    ObjectName query = null;
    if (queryNames != null)
    {
      try
      {
        query = new ObjectName(queryNames);
      }
      catch (MalformedObjectNameException e)
      {
        Element exceptionElement = document.createElement("Exception");
        exceptionElement.setAttribute("errorMsg", e.getMessage());
        root.appendChild(exceptionElement);
        return document;
      }
    }
    Set mbeans = server.queryMBeans(query, null);
    Iterator i = mbeans.iterator();
    // this will order the domains
    Map domains = new TreeMap();
    while (i.hasNext())
    {
      ObjectInstance instance = (ObjectInstance)i.next();
      ObjectName name = instance.getObjectName();
      String domain = name.getDomain();
      if (domains.containsKey(domain))
      {
        ((Set)domains.get(domain)).add(name);
      }
      else
      {
        Set objects = new TreeSet(CommandProcessorUtil.createObjectNameComparator());
        objects.add(name);
        domains.put(domain, objects);
      }
    }
    i = domains.keySet().iterator();
    while (i.hasNext())
    {
      String domain = (String)i.next();
      Element domainElement = document.createElement("Domain");
      root.appendChild(domainElement);
      domainElement.setAttribute("name", domain);
      Set names = (Set)domains.get(domain);
      Iterator j = names.iterator();
      while (j.hasNext())
      {
        ObjectName targetName = (ObjectName)j.next();
        if (targetClass != null && !server.isInstanceOf(targetName, targetClass))
        {
          continue;
        }
        Element mBeanElement = document.createElement("MBean");
        mBeanElement.setAttribute("objectname", targetName.toString());
        MBeanInfo info = server.getMBeanInfo(targetName);
        mBeanElement.setAttribute("description", info.getDescription());
        mBeanElement.setAttribute("classname", info.getClassName());
        domainElement.appendChild(mBeanElement);
      }
    }
    return document;
  }

}
