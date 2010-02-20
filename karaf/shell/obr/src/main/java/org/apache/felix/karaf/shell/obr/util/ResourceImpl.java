/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.karaf.shell.obr.util;

import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

import java.net.URL;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class ResourceImpl implements Resource
{

  private final Map properties;
  private final String symbolicName;
  private final String presentationName;
  private final Version version;
  private final String id;
  private final URL url;
  private final Requirement[] requirements;
  private final Capability[] capabilities;
  private final String[] categories;
  private final Repository repository;


  public ResourceImpl(Map properties, String symbolicName, String presentationName, Version version, String id, URL url, Requirement[] requirements, Capability[] capabilities, String[] categories, Repository repository)
  {
    this.properties = properties;
    this.symbolicName = symbolicName;
    this.presentationName = presentationName;
    this.version = version;
    this.id = id;
    this.url = url;
    this.requirements = requirements;
    this.capabilities = capabilities;
    this.categories = categories;
    this.repository = repository;
  }

  public Map getProperties()
  {
    return properties;
  }

  public String getSymbolicName()
  {
    return symbolicName;
  }

  public String getPresentationName()
  {
    return presentationName;
  }

  public Version getVersion()
  {
    return version;
  }

  public String getId()
  {
    return id;
  }

  public URL getURL()
  {
    return url;
  }

  public Requirement[] getRequirements()
  {
    return requirements;
  }

  public Capability[] getCapabilities()
  {
    return capabilities;
  }

  public String[] getCategories()
  {
    return categories;
  }

  public Repository getRepository()
  {
    return repository;
  }
}
