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
package org.apache.felix.mosgi.jmx.agent.mx4j.loading;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.management.ObjectName;

/**
 * Represents an MLET tag, as documented in the JMX specification.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MLetTag
{
   private String code;
   private String object;
   private String archive;
   private String codebase;
   private ObjectName objectName;
   private String version;
   private ArrayList signature = new ArrayList();
   private ArrayList arguments = new ArrayList();

   /**
    * Normalizes the codebase held by this MLetTag (specified in the MLet file) using the
    * URL of the MLet file as default.
    * This means that if the codebase in the MLet file is not provided or it is relative, then
    * the URL of the MLet file will be taken as base for computing the normalized codebase;
    * otherwise, if a full URL has been specified as codebase in the MLet file, that URL is taken
    * and the URL of the MLet file is discarded.
    *
    * @param mletFileURL The URL of the MLet file
    * @return The normalized codebase
    */
   public URL normalizeCodeBase(URL mletFileURL)
   {
      // If the codebase specified in the MLet file is relative, or null,
      // then the codebase is the one of the mletFileURL, otherwise
      // the given codebase must be used.

      URL codebaseURL = null;
      String codebase = getCodeBase();
      if (codebase != null)
      {
         // Try to see if it's a URL
         try
         {
            codebaseURL = new URL(codebase);
         }
         catch (MalformedURLException ignored)
         {
            // Not a complete URL, use the mletFileURL as a base
            try
            {
               codebaseURL = new URL(mletFileURL, codebase);
            }
            catch (MalformedURLException alsoIgnored)
            {
            }
         }
      }

      // Either codebase not provided or failed to be created, use the mletFileURL
      if (codebaseURL == null)
      {
         String path = mletFileURL.getPath();
         int index = path.lastIndexOf('/');

         try
         {
            codebaseURL = new URL(mletFileURL, path.substring(0, index + 1));
         }
         catch (MalformedURLException ignored)
         {
            // Cannot fail, we just remove the mlet file name from the path
            // leaving the directory where it resides as a codebase
         }
      }
      return codebaseURL;
   }

   /**
    * Returns the jars file names specified in the ARCHIVE attribute of the MLet tag.
    */
   public String[] parseArchive()
   {
      String archive = getArchive();
      ArrayList archives = new ArrayList();
      StringTokenizer tokenizer = new StringTokenizer(archive, ",");
      while (tokenizer.hasMoreTokens())
      {
         String token = tokenizer.nextToken().trim();
         if (token.length() > 0)
         {
            token.replace('\\', '/');
            archives.add(token);
         }
      }
      return (String[])archives.toArray(new String[0]);
   }

   /**
    * Returns the URL for the given archive file name using the provided URL as a codebase,
    * or null if the URL cannot be created.
    */
   public URL createArchiveURL(URL codebase, String archive)
   {
      try
      {
         return new URL(codebase, archive);
      }
      catch (MalformedURLException ignored)
      {
      }
      return null;
   }

   public String getVersion()
   {
       return version;
   }

   public String getCodeBase()
   {
      return codebase;
   }

   public String getArchive()
   {
      return archive;
   }

   public String getCode()
   {
      return code;
   }

   public ObjectName getObjectName()
   {
      return objectName;
   }

   public String getObject()
   {
      return object;
   }

   public String[] getSignature()
   {
      return signature == null ? new String[0] : (String[])signature.toArray(new String[signature.size()]);
   }

   public Object[] getArguments()
   {
      return arguments == null ? new Object[0] : (Object[])arguments.toArray(new Object[arguments.size()]);
   }

   //
   // Setters, called by MLetParser
   //

   void setArchive(String archive)
   {
      this.archive = archive;
   }

   void setCode(String code)
   {
      this.code = code;
   }

   void setCodeBase(String codebase)
   {
      // Important that the codebase ends with a slash, see usages of getCodeBase()

      codebase = codebase.replace('\\', '/');
      if (!codebase.endsWith("/")) codebase += "/";
      this.codebase = codebase;
   }

   void setName(ObjectName name)
   {
      objectName = name;
   }

   void setObject(String object)
   {
      this.object = object;
   }

   void setVersion(String version)
   {
      this.version = version;
   }

   void addArg(String type, Object value)
   {
      signature.add(type);
      arguments.add(value);
   }
}
