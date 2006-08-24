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

import java.util.regex.Pattern;

import org.apache.felix.tool.mangen.MangenMain;
import org.apache.felix.tool.mangen.Report;
import org.apache.felix.tool.mangen.RuleHandler;

/**
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class RuleReport
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
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public RuleReport()
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
        //TODO: bit of a hack for now!
        RuleHandler.runRuleSetReports(MangenMain.ruleSets, rpt, true);
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - GenericHandlerItem
    //////////////////////////////////////////////////

    /**
     */
    public void setOptions(String options)
    {
        //TODO: including checking of wildcard matching options
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
