/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.searchpolicy;

import java.net.URL;

import org.apache.felix.moduleloader.*;

public class R4SearchPolicy implements IR4SearchPolicy
{
    private R4SearchPolicyCore m_policyCore = null;
    private IModule m_module = null;

    public R4SearchPolicy(R4SearchPolicyCore policyCore, IModule module)
    {
        m_policyCore = policyCore;
        m_module = module;
    }

    public Object[] definePackage(String name)
    {
        return m_policyCore.definePackage(m_module, name);
    }

    public Class findClass(String name)
        throws ClassNotFoundException
    {
        return m_policyCore.findClass(m_module, name);
    }

    public URL findResource(String name)
        throws ResourceNotFoundException
    {
        return m_policyCore.findResource(m_module, name);
    }

    public String findLibrary(String name)
    {
        return m_policyCore.findLibrary(m_module, name);
    }

    public R4Export[] getExports()
    {
        return m_policyCore.getExports(m_module);
    }

    public void setExports(R4Export[] exports)
    {
        m_policyCore.setExports(m_module, exports);
    }

    public R4Import[] getImports()
    {
        return m_policyCore.getImports(m_module);
    }

    public void setImports(R4Import[] imports)
    {
        m_policyCore.setImports(m_module, imports);
    }

    public R4Import[] getDynamicImports()
    {
        return m_policyCore.getDynamicImports(m_module);
    }

    public void setDynamicImports(R4Import[] imports)
    {
        m_policyCore.setDynamicImports(m_module, imports);
    }

    public R4Library[] getLibraries()
    {
        return m_policyCore.getLibraries(m_module);
    }

    public void setLibraries(R4Library[] libraries)
    {
        m_policyCore.setLibraries(m_module, libraries);
    }

    public R4Wire[] getWires()
    {
        return m_policyCore.getWires(m_module);
    }

    public void setWires(R4Wire[] wires)
    {
        m_policyCore.setWires(m_module, wires);
    }

    public boolean isResolved()
    {
        return m_policyCore.isResolved(m_module);
    }

    public void setResolved(boolean resolved)
    {
        m_policyCore.setResolved(m_module, resolved);
    }

    public void resolve() throws ResolveException
    {
        m_policyCore.resolve(m_module);
    }

    public boolean isRemovalPending()
    {
        return m_policyCore.isRemovalPending(m_module);
    }

    public void setRemovalPending(boolean removalPending)
    {
        m_policyCore.setRemovalPending(m_module, removalPending);
    }

    public void addResolverListener(ResolveListener l)
    {
        m_policyCore.addResolverListener(l);
    }

    public void removeResolverListener(ResolveListener l)
    {
        m_policyCore.removeResolverListener(l);
    }

    public String toString()
    {
        return m_module.toString();
    }
}