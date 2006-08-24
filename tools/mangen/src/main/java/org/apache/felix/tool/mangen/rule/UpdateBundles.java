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

import java.io.IOException;

import java.util.List;
import java.util.Iterator;

import org.apache.felix.tool.mangen.MangenMain;
import org.apache.felix.tool.mangen.BundleJar;

/**
 * Rule to request each bundle to update it's manifest, based on the mangen
 * rule-based set of imports and exports.
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class UpdateBundles
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

    public UpdateBundles()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    public boolean overwrite = false;
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Rule
    //////////////////////////////////////////////////
    
    /**
     * Iterate over the list of bundles and ask each bundle to update itself.
     */
    public void execute(List jarList)
    {   
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            try
            {
                rptOut.println("Updating bundle: " + bund.getName() + " :");
                bund.update(overwrite);
            }
            catch (IOException ioe)
            {
                MangenMain.error(rptOut, "IO Exception occured during update: " + ioe);                
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
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Overrides superclass version to look for 'overwrite' option qualifiers
     */
    protected void processNonStandardQualifier(String qual)
    {
        if (qual.compareToIgnoreCase("overwrite") == 0 )
        {
            overwrite = true;
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
