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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.felix.tool.mangen.MangenMain;
import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.OsgiPackage;

/**
 * Rule to resolve the set of imports and exports across all bundles. 
 * <p>
 * Exports that are not required to satisfy any bundle import will be removed. Imports
 * which have no matching exports will be reported, as will duplicate exports.
 * <p>
 * At present duplicate package names are detected solely on name. In future this
 * needs to be extended to include a version, or version range match.
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class ResolveImportsToExports
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
    
    public Set          allImports = OsgiPackage.getNewSet();
    public Set          resolvedImports = OsgiPackage.getNewSet();
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public ResolveImportsToExports()
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
    
    
    public void execute(List jarList)
    {   
        // build up complete set of required imports
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            allImports.addAll(bund.getPossibleImports());
        }
        
        // for each bundle, resolve exports to imports and remove exports 
        // that don't match a required import
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            rptOut.println("");            
            rptOut.println("> " + bund.getName() + " :");   
            
            Set exports = bund.getPossibleExports();
            for(Iterator j = exports.iterator(); j.hasNext(); )
            {
                OsgiPackage pkg = (OsgiPackage) j.next();
                if (allImports.contains(pkg))
                {
                    // exports matches imports, move it to resolved list or report
                    // duplicate if already there
                    allImports.remove(pkg);
                    resolvedImports.add(pkg);
                    rptOut.println(" ... resolved export: "  + pkg);
                }
                else if (resolvedImports.contains(pkg))
                {
                    MangenMain.warning(rptOut, "*** WARNING *** duplicate export, removing: " + pkg);
                    //TODO: for now we'll suppress the duplicate, which means first
                    //      seen becomes the exporter. Probably need to handle
                    //      better e.g. using versions or wilcard rules in manifest
                    //      to decide who exports
                    j.remove();
                }
                else
                {
                    // export doesn't match any imports, so remove it
                    rptOut.println(" ... removing un-needed export: "  + pkg);
                    j.remove();
                }
            }
        }
        
        rptOut.println("");
        
        // report any unresolved imports
        for(Iterator i = allImports.iterator(); i.hasNext(); )
        {
            OsgiPackage pkg = (OsgiPackage) i.next();
            if (!isPackageMatch(pkg, "sys-packages"))
            {
                MangenMain.warning(rptOut, "*** WARNING *** unresolved import: "  + pkg);
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
     * This rule cannot be used locally.
     */
    public boolean isUsableLocally()
    {
        return false;
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
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
