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
package org.apache.felix.tool.mangen.rule;

import java.util.List;
import java.util.Iterator;
import java.util.Set;

import org.apache.felix.tool.mangen.OsgiPackage;
import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.Rule;

/**
 * Rule to exclude each bundle's own exports from it's list of possible imports.
 * <p>
 * Any packages which are exported by a bundle in general do not need to also
 * be imported and hence can be excluded from the list of possible imports.
 * <p>
 * Note that with OSGi R4 comes support for a bundle to import versions of it's 
 * own packages from an alternate bundle. This may require enhancements to this
 * rule, or possibly extra rules. 
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class DontImportOwnExports
        extends GenericRule
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public DontImportOwnExports()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Rule
    //////////////////////////////////////////////////
    
    /**
     * Iterate over the list of bundles and for each bundle remove any import
     * which matches an export in the same bundle.
     */
    public void execute(List jarList)
    {   
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            rptOut.println("");            
            rptOut.println("> " + bund.getName() + " :");            
            
            Set exports = bund.getPossibleExports();
            Set imports = bund.getPossibleImports();
            for(Iterator j = exports.iterator(); j.hasNext(); )
            {
                remove(imports, (OsgiPackage) j.next());
            }
        }
    }
    
    /**
     * This rule can be used globally.
     */
    public boolean isUsableGlobally()
    {
        return true;
    }
    
    /**
     * This rule can be used locally.
     */
    public boolean isUsableLocally()
    {
        return true;
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - GenericHandlerItem
    //////////////////////////////////////////////////

    /**
     * No required options for this rule.
     */
    public void setOptions(String options)
    {
    }
    
    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Removes any import which is an exact match for the supplied package.
     */
    protected void remove(Set set, OsgiPackage pkg)
    {
        if (set.contains(pkg))
        {
            rptOut.println("... removing import of own export: " + pkg);
            set.remove(pkg);
        }
    }
    
    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    
}
