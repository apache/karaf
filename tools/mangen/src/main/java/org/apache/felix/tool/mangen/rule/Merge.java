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

import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.OsgiPackage;
import org.apache.felix.tool.mangen.OsgiStringPackage;
import org.apache.felix.tool.mangen.OsgiR4Package;

/**
 * Rule to merge the current mangenn mangen created import and/or export package sets
 * with some other package set. 
 * <p>
 * At present the possible other package sets are:
 * <ul>
 * <li> existing    -  existing <code>Export-Package</code> or <code>Import-Package</code> statements in the 
 *                     current manifest
 * <li> fixed       -  <code>Export-Package</code> or <code>Import-Package</code> in the mangen section of 
 *                     the current manifest 
 * </ul>
 * Package names that match any of a list of regex based package name patterns
 * will be merged into the mangen generated list of possible imports or exports 
 * (as appropriate). 
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class Merge
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

    public Merge()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    public boolean existing = false;
    
    public boolean fixed  = false;
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Rule
    //////////////////////////////////////////////////
    
    /**
     * Iterate over the list of bundle jars, removing any imports
     * which match any of the specific package name Patterns
     */
    public void execute(List jarList)
    {
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            
            if (existing)
            {
                rptOut.println("");            
                rptOut.println("> " + bund.getName() + " existing package set:");            
                mergePackages(bund.getCurrentImports(), "imports", bund.getPossibleImports());
                mergePackages(bund.getCurrentExports(), "exports", bund.getPossibleExports());
            }
            
            if (fixed)
            {
                rptOut.println("");            
                rptOut.println("> " + bund.getName() + " fixed package set:");            
                mergePackages(bund.getFixedImports(), "imports", bund.getPossibleImports());
                mergePackages(bund.getFixedExports(), "exports", bund.getPossibleExports());
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
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Overrides superclass version to look for 'existing' and 'fixed' option
     * qualifiers
     */
    protected void processNonStandardQualifier(String qual)
    {
        if (qual.compareToIgnoreCase("existing") == 0 )
        {
            existing = true;
        }
        else if (qual.compareToIgnoreCase("fixed") == 0)
        {
            fixed = true;
        }
    }
    
    /**
     * Any packages from the specified current set which match package patterns in the
     * specified qualifier set will be merged into the specified mangen set. 
     *
     * In the case where the current package does not already exist in the mangen set, 
     * merging is a simple matter of adding the package. 
     *
     * In the case where the current package is already present in the mangen set, then 
     * a decision is needed on how to merge any version information present on the 
     * package entry in either set.
     */
    protected void mergePackages(Set currentSet, String qualName, Set mangenSet)
    {
        rptOut.println("");
        rptOut.println("... merge to " + qualName);
        for(Iterator i = currentSet.iterator(); i.hasNext(); )
        {
            OsgiPackage pkg = (OsgiPackage) i.next();
            if (isPackageMatch(pkg, qualName))
            {
                merge(pkg, mangenSet);
            }
        }
    }
    
    /**
     * Merge the supplied package into the specified set.
     */
    protected void merge(OsgiPackage pkgToMerge, Set set)
    {
        // oops, set doesn't have a "get". Guess we could move to using Map
        // but Sets are working for most other cases, so find by hand for now.
        OsgiPackage existingPkg = getPackageFromSet(pkgToMerge, set);

        // if not present in any form, then we can add
        if (existingPkg == null)
        {
            set.add(pkgToMerge);
            rptOut.println("   > adding : " + pkgToMerge);
            return;
        }
        
        // There's no added value in merging string packages to anything already present
        // since they carry no extra package attributes
        if (pkgToMerge instanceof OsgiStringPackage)
        {
            return;
        }
        
        // string packages can be replaced with R4 package, which will have
        // extended package attributes.
        if (existingPkg instanceof OsgiStringPackage)
        {
            set.remove(existingPkg);
            set.add(pkgToMerge);
            rptOut.println("   > replacing : " + existingPkg + "  with : " + pkgToMerge);
            return;
        }
        
        // if present and don't match then throw error
        if (existingPkg instanceof OsgiR4Package && existingPkg.compareTo(pkgToMerge) != 0)
        {
            rptOut.println("*** ERROR *** can't merge conflicting package details: " + pkgToMerge
                           + "  !=  " +  existingPkg);
            return;
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
