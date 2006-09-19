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

import org.apache.felix.moduleloader.IModule;

/**
 * <p>
 * This exception is thrown if a module cannot be resolved. The module
 * that failed to be resolved is recorded, along with the failed import target
 * identifier and version number. If the error was a result of a propagation
 * conflict, then the propagation error flag is set.
 * </p>
 * @see org.apache.felix.moduleloader.search.ImportSearchPolicy#validate(org.apache.felix.moduleloader.Module)
**/
public class ResolveException extends Exception
{
    private IModule m_module = null;
    private R4Package m_pkg = null;

    /**
     * Constructs an exception with the specified message, module,
     * import identifier, import version number, and propagation flag.
    **/
    public ResolveException(String msg, IModule module, R4Package pkg)
    {
        super(msg);
        m_module = module;
        m_pkg = pkg;
    }

    /**
     * Returns the module that was being resolved.
     * @return the module that was being resolved.
    **/
    public IModule getModule()
    {
        return m_module;
    }

    public R4Package getPackage()
    {
        return m_pkg;
    }
}