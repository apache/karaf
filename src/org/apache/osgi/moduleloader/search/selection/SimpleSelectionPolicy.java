/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.osgi.moduleloader.search.selection;

import java.util.*;

import org.apache.osgi.moduleloader.*;
import org.apache.osgi.moduleloader.search.*;

/**
 * This class implements a reasonably simple selection policy for the
 * <tt>ImportSearchPolicy</tt>. When given a choice, this selection
 * policy will always select the newest version of the available
 * candidates to satisfy the import identifier. In the case where
 * a candidate has already been selected for a given import identifier,
 * then the previously selected module will be returned, if possible.
 * If it is not possible to return the previously selected module, then
 * a <tt>null</tt> is returned. This policy assumes that classes are
 * shared globally.
**/
public class SimpleSelectionPolicy implements SelectionPolicy, ModuleListener
{
    private Map m_resolvedPackageMap = new HashMap();
    private Map m_resolvedModuleMap = new HashMap();

    /**
     * Selects a single module to resolve the specified import identifier
     * from the array of compatible candidate modules. If the import
     * identifier has not been resolved before, then this selection policy
     * chooses the module that exports the newest version of the
     * import identifer. If the import identifier has been resolved already,
     * then the same module that was chosen before is chosen again.
     * This ensures that all modules use the same version of all
     * exported classes.
     * @param module the module that is importing the target.
     * @param identifier the identifier of the import target.
     * @param version the version number of the import target.
     * @param candidates array of compatible candidate modules from which to choose.
     * @return the selected module or <tt>null</tt> if no module
     *         can be selected.
    **/
    public synchronized Module select(Module module, Object identifier,
        Object version, Module[] candidates, CompatibilityPolicy compatPolicy)
    {
        // See if package is already resolved.
        Module selModule = (Module) m_resolvedPackageMap.get(identifier);

        // If no module was previously selected to export the package,
        // then try to choose one now.
        if (selModule == null)
        {
            Object selVersion = null;

            // Examine all exported instances of the identifier and
            // choose the one with the newest version number. If
            // there is more than one source for the newest version,
            // then just select the first one found.
            for (int i = 0; i < candidates.length; i++)
            {
                Object tmpVersion =
                    ImportSearchPolicy.getExportVersion(candidates[i], identifier);

                // If this is the first comparison, then
                // just record it.
                if (selVersion == null)
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                }
                // If the current export package version is greater
                // than the selected export package version, then
                // record it instead.
                else if (compatPolicy.compare(identifier, tmpVersion, identifier, selVersion) >= 0)
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                }
            }

            m_resolvedPackageMap.put(identifier, selModule);
            m_resolvedModuleMap.put(selModule, selModule);
        }
        // See if the previously selected export module satisfies
        // the current request, otherwise return null.
        else
        {
            Object selVersion =
                ImportSearchPolicy.getExportVersion(selModule, identifier);
            Module tmpModule = selModule;
            selModule = null;
            if (compatPolicy.isCompatible(identifier, selVersion, identifier, version))
            {
                selModule = tmpModule;
            }
        }

        return selModule;
    }

    public void moduleAdded(ModuleEvent event)
    {
    }

    public void moduleReset(ModuleEvent event)
    {
        moduleRemoved(event);
    }

    public synchronized void moduleRemoved(ModuleEvent event)
    {
        // If the module that was removed was chosen for
        // exporting identifier, then flush it from our
        // data structures; we assume here that the application
        // will flush references to the removed module's classes.
        if (m_resolvedModuleMap.get(event.getModule()) != null)
        {
            // Remove from module map.
            m_resolvedModuleMap.remove(event.getModule());
            // Remove each exported package from package map.
            Iterator iter = m_resolvedPackageMap.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry) iter.next();
                if (entry.getValue() == event.getModule())
                {
                    iter.remove();
                }
            }
        }
    }
}