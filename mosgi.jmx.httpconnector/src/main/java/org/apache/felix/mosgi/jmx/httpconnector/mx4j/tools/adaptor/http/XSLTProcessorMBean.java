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

import java.util.Locale;

/**
 * Management interface for the XSLTProcessor MBean.
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface XSLTProcessorMBean extends ProcessorMBean
{
   /**
    * Sets the jar/zip file or the directory where to find the XSL files
    * @see #getFile
    */
   public void setFile(String file);

   /**
    * Returns the jar/zip file or the directory where XSL files are loaded
    * @see #setFile
    */
   public String getFile();

   /**
    * Returns the path of the XSL templates inside a jar file.
    * @see #setPathInJar
    */
   public String getPathInJar();

   /**
    * Specifies the path of the XSL templates inside a jar file.
    * @see #getPathInJar
    */
   public void setPathInJar(String path);

   /**
    * Returns the default start page
    * @see #setDefaultPage
    */
   public String getDefaultPage();

   /**
    * Sets the default start page, serverbydomain as a default
    * @see #getDefaultPage
    */
   public void setDefaultPage(String defaultPage);

   /**
    * Returns if the XSL files are contained in a jar/zip file.
    * @see #isUsePath
    * @see #setFile
    */
   boolean isUseJar();

   /**
    * Returns if the XSL files are contained in a path.
    * @see #isUseJar
    * @see #setFile
    */
   boolean isUsePath();

   /**
    * Maps a given extension with a specified MIME type
    */
   public void addMimeType(String extension, String type);

   /**
    * Sets the caching of the XSL Templates.
    */
   public void setUseCache(boolean useCache);

   /**
    * Returns if the XSL Templates are cached
    */
   boolean isUseCache();

   /**
    * Returns the Locale used to internationalize the output
    */
   public Locale getLocale();

   /**
    * Sets the locale used to internationalize the output
    */
   public void setLocale(Locale locale);

   /**
    * Sets the locale used to internationalize the output, as a string
    */
   public void setLocaleString(String locale);
}
