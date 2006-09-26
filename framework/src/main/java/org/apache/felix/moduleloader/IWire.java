/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.moduleloader;

import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.framework.searchpolicy.R4Export;

public interface IWire
{
    public IModule getImporter();
    public IModule getExporter();
    public R4Export getExport();
    public Class getClass(String name) throws ClassNotFoundException;
    public URL getResource(String name) throws ResourceNotFoundException;
    public Enumeration getResources(String name) throws ResourceNotFoundException;
}