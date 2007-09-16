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

public interface IModuleFactory
{
    public IModule[] getModules();
    public IModule getModule(String id);

    public IModule createModule(String id, IModuleDefinition md);
    public void removeModule(IModule module);

    public void setContentLoader(IModule module, IContentLoader contentLoader);

    public void addModuleListener(ModuleListener l);
    public void removeModuleListener(ModuleListener l);

    public void setSecurityContext(IModule module, Object securityContext);

    /**
     * This is an experimental method that is likely to change or go
     * away - so don't use it for now.
     *
     * Note to self, we need to think about what the implications of
     * this are and whether we are fine with them.
     */
    public void refreshModule(IModule currentModule);
}