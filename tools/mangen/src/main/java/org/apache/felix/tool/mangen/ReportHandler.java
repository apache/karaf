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
package org.apache.felix.tool.mangen;

import java.io.IOException;
import java.io.PrintStream;

import java.util.List;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */ 
public class ReportHandler
        extends GenericHandler
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    /** Classname prefix for basic report classes. */ 
    public static final String  REPORT_PACKAGE = "org.apache.felix.tool.mangen.report";
    /** Report property prefix. **/
    public static final String  REPORT_KEY = "mangen-report-";
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    /**
     * Initialise the set of reports based on the REPORT_KEY.<n> properties defined.
     * Each reports is either a full classname implementing the Report interface, or a 
     * classname in REPORT_PACKAGE. The actual work to process the properties and
     * create the report objects is done by our parent class, GenericHandler. 
     */
    public ReportHandler()
    {
        super(REPORT_KEY, Report.class, REPORT_PACKAGE); 
    }
    
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Retrieve each defined report and run it.
     */
    public void runReports(PrintStream rpt, List jarList)
            throws IOException
    {
        for(Iterator i = handlerList.iterator(); i.hasNext(); )
        {
            Report report = (Report) i.next();
            report.run(rpt, jarList);
        }
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
