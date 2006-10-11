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
import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.management.JMException;
import javax.management.loading.DefaultLoaderRepository;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ConstructorsCommandProcessor, processes a request to query the available
 * constructors for a classname
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @author <a href="mailto:l.dobrev@lsnsoft.com">Lachezar Dobrev</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ConstructorsCommandProcessor extends HttpCommandProcessorAdaptor
{
  public ConstructorsCommandProcessor()
  {
  }

  public Document executeRequest(HttpInputStream in) throws IOException, JMException
  {
    Document document = builder.newDocument();

    String classname = in.getVariable("classname");
    if (classname == null || classname.trim().length() == 0)
    {
      return createException(document, "", "classname parameter required");
    }
    else
    {
      // look class in default classloader
      Class targetClass = null;
      try
      {
        targetClass = DefaultLoaderRepository.loadClass(classname);
      }
      catch (ClassNotFoundException e) {}
      try
      {
        if (targetClass == null)
        {
          targetClass = ClassLoader.getSystemClassLoader().loadClass(classname);
        }
      }
      catch (ClassNotFoundException e) {}
      try
      {
        if (targetClass == null)
        {
          targetClass = getClass().getClassLoader().loadClass(classname);
        }
      }
      catch (ClassNotFoundException e) {}

      if (targetClass == null) {
        return createException(document, classname, "class " + classname + " not found");
      }

      Element root = document.createElement("Class");
      root.setAttribute("classname", classname);
      document.appendChild(root);
      Constructor[] constructors = targetClass.getConstructors();
      Arrays.sort(constructors, CommandProcessorUtil.createConstructorComparator());
      for (int i=0;i<constructors.length;i++)
      {
        System.out.println("Constructor " + constructors[i]);
        Element constructor = document.createElement("Constructor");
        constructor.setAttribute("name", constructors[i].getName());
        addParameters(constructor, document, constructors[i].getParameterTypes());
        root.appendChild(constructor);
      }
    }
    return document;
  }

  protected void addParameters(Element node, Document document, Class[] parameters)
  {
    for (int j=0;j<parameters.length;j++)
    {
      Element parameter = document.createElement("Parameter");
      parameter.setAttribute("type", parameters[j].getName());
      parameter.setAttribute("strinit", String.valueOf(CommandProcessorUtil.canCreateParameterValue(parameters[j].getName())));
      // add id since order is relevant
      parameter.setAttribute("id", ""+j);
      node.appendChild(parameter);
     }
  }

  private Document createException(Document document, String classname, String message)
  {
    Element exceptionElement = document.createElement("Exception");
    document.appendChild(exceptionElement);
    exceptionElement.setAttribute("classname", classname);
    exceptionElement.setAttribute("errorMsg", message);
    return document;
  }

}
