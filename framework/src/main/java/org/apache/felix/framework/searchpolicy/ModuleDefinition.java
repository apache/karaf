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
package org.apache.felix.framework.searchpolicy;

import org.apache.felix.moduleloader.IModuleDefinition;

public class ModuleDefinition implements IModuleDefinition
{
    private R4Export[] m_exports = null;
    private R4Import[] m_imports = null;
    private R4Import[] m_dynamicImports = null;
    private R4Library[] m_libraries = null;

    public ModuleDefinition(
        R4Export[] exports, R4Import[] imports,
        R4Import[] dynamicImports, R4Library[] libraries)
    {
        m_exports = exports;
        m_imports = imports;
        m_dynamicImports = dynamicImports;
        m_libraries = libraries;
    }

    public R4Export[] getExports()
    {
// TODO: ML - These should probably all return copies of the array.
        return m_exports;
    }

    public R4Import[] getImports()
    {
        return m_imports;
    }

    public R4Import[] getDynamicImports()
    {
        return m_dynamicImports;
    }

    public R4Library[] getLibraries()
    {
        return m_libraries;
    }
}