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
 * Rule to caused each Bundle JAR to be processed. This will involved scanning
 * the classes and inner jars within each bundle JAR to determine the range of 
 * possible imports and exports.
 *
 * @version $Revision: 18 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class ProcessBundles
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

    public ProcessBundles()
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
     * Iterate over the list of bundles and ask each bundle to process it's
     * contained classes and inner JARs.
     */
    public void execute(List jarList)
    {   
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            try
            {
                bund.process();
            }
            catch (RuntimeException re)
            {
                MangenMain.error(rptOut, "Exception: " + re + ", skipping bundle jar: " + bund.getName());
                re.printStackTrace(rptOut);
            }
            catch (Exception e)
            {
                MangenMain.error(rptOut, "Exception: " + e + ", skipping bundle jar: " + bund.getName());
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
