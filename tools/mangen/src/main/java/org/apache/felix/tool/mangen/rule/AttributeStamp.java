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

import org.apache.felix.tool.mangen.MangenMain;
import org.apache.felix.tool.mangen.OsgiPackage;
import org.apache.felix.tool.mangen.OsgiStringPackage;
import org.apache.felix.tool.mangen.OsgiR4Package;
import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.Rule;

/**
 * Rule to match package name strings against a wildcard pattern and them stamp the
 * package with additional OSGi attributes e.g. version information.
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class AttributeStamp
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

    public AttributeStamp()
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

            stampPackages(bund.getPossibleImports(), "imports");
            stampPackages(bund.getPossibleExports(), "exports");
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
     * Any string packages from the specified set which match package patterns in the
     * specified qualifier set will have the additional OSGi attributes "stamped"
     * onto their package specification. Packages that already have attributes which
     * match will generate warnings and be skipped. 
     *
     */
    protected void stampPackages(Set set, String qualName)
    {
        rptOut.println("");
        rptOut.println("... stamping packages in " + qualName);
        for(Iterator i = set.iterator(); i.hasNext(); )
        {
            OsgiPackage pkg = (OsgiPackage) i.next();
            String stamp = getMatchingPatternString(pkg, qualName, true);
            if (stamp != null)
            {
                stamp(pkg, stamp, set);
            }
        }
    }
    
    /**
     * Stamp the supplied package with the specified attributes. This will be 
     * an error if the package is already an R4 pakage with conflicting attributes.
     */
    protected void stamp(OsgiPackage pkg, String stamp, Set set)
    {
        int delim = stamp.indexOf(";");
        
        if (delim == -1)
        {
            MangenMain.warning(rptOut, "*** WARNING *** stamp has no attributes: " + stamp);
            return;
        }
        
        // simple thing is to rebuild an OSGi header with attributes and use OsgiPackage 
        // methods to parse this into an R4 package.
        String hdr = pkg.getName() + stamp.substring(delim);
        OsgiPackage[] newPkgs = (OsgiPackage[]) OsgiPackage.createFromHeaders(hdr).toArray(new OsgiPackage[0]);
        if (newPkgs.length != 1)
        {
            MangenMain.error(rptOut, "*** ERROR *** stamp doesn't create a single package : " + stamp);
            return;
        }
        OsgiPackage stampedPkg = newPkgs[0];
        
        // replace a simple string package with the stamped package
        if (pkg instanceof OsgiStringPackage)
        {
            set.remove(pkg);
            set.add(stampedPkg);
            rptOut.println("   > replacing : " + pkg + "  with : " + stampedPkg);
            return;
            
        }
        
        // it's an error to try and stamp an existing package with different details
        if (pkg instanceof OsgiR4Package && pkg.compareTo(stampedPkg) != 0)
        {
            MangenMain.error(rptOut, "*** ERROR *** stamp has conflicting package details: " + pkg
                                     + "  !=  " +  stampedPkg);
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
