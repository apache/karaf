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
package org.apache.felix.tool.mangen.report;

import java.io.IOException;
import java.io.PrintStream;

import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.regex.Pattern;

import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.OsgiPackage;
import org.apache.felix.tool.mangen.Report;

/**
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class BundleReport
        implements Report
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    public static Pattern[] pkgPatterns;
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    public boolean optShowDiffs = false;
    public boolean optShowLocalRules = false;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public BundleReport()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // INTERFACE METHODS - Report
    //////////////////////////////////////////////////
    
    /**
     */
    public void run(PrintStream rpt, List jarList)
            throws IOException
    {
        for(Iterator i = jarList.iterator(); i.hasNext(); )
        {
            BundleJar bund = (BundleJar) i.next();
            doReport(rpt, bund);
        }
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - GenericHandlerItem
    //////////////////////////////////////////////////
    
    /**
     * Process the option set as a a space separated list of options.
     * 
     *      opt1 opt2
     */
    public void setOptions(String options)
    {
        for (StringTokenizer tok = new StringTokenizer(options, " "); tok.hasMoreTokens(); )
        {
            String opt = tok.nextToken().trim();
            if (opt.compareToIgnoreCase("show-differences") == 0)
            {
                optShowDiffs = true;
            }
            else if (opt.compareToIgnoreCase("show-local-rules") == 0)
            {
                optShowLocalRules = true;
            }
        }
    }
    
    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Produce a simple report on the current state of the imports and exports
     * for the specified bundle's jar.
     */
    protected void doReport(PrintStream rpt, BundleJar bund)
            throws IOException
    {
        rpt.println("");
        rpt.println("============================================================");
        rpt.println(bund.getName());        
        rpt.println("============================================================");
        rpt.println("");
        
        if (optShowLocalRules && bund.bundleRuleHandler != null)
        {
            rpt.println("Local bundle rules:");
            bund.bundleRuleHandler.runReports(rpt, false);            
        }
        
        if (optShowDiffs)
        {
            rpt.println("");
            rpt.println("mangen import differences:");
            showDiffs(bund.getPossibleImports(), bund.getCurrentImports(), rpt);
            
            rpt.println("");
            rpt.println("mangen export differences:");
            showDiffs(bund.getPossibleExports(), bund.getCurrentExports(), rpt);
        }
        else
        {
            rpt.println("");
            rpt.println("mangen proposed imports:");
            Set imports = bund.getPossibleImports();
            for(Iterator i = imports.iterator(); i.hasNext(); )
            {
                rpt.println("   " + (OsgiPackage) i.next());
            }
            
            rpt.println("");
            rpt.println("mangen proposed exports:");
            Set exports = bund.getPossibleExports();
            for(Iterator i = exports.iterator(); i.hasNext(); )
            {
                rpt.println("   " + (OsgiPackage) i.next());
            }
    
            rpt.println("");
        }
    }

    /**
     * Show the differences in the generated Set of packages and the current Set.
     */
    protected void showDiffs(Set genSet, Set currSet, PrintStream rpt)
    {
        for(Iterator i = genSet.iterator(); i.hasNext(); )
        {
            OsgiPackage newPkg = (OsgiPackage) i.next();
            if (!currSet.contains(newPkg))
            {
                rpt.println("   +++ ADDED   :" + newPkg);
            }
        }
        
        for(Iterator i = currSet.iterator(); i.hasNext(); )
        {
            OsgiPackage oldPkg = (OsgiPackage) i.next();
            if (!genSet.contains(oldPkg))
            {
                rpt.println("   --- REMOVED :" + oldPkg);
            }
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
