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

/**
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */ 
public interface Rule
        extends GenericHandlerItem
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // SIGNATURES
    //////////////////////////////////////////////////

    /**
     * Execute the Rule on the supplied list of BundleJars.
     */
    public void execute(List jarList);
    
    /**
     * Send any output for the Rule to the specified PrintStream.
     */
    public void report(PrintStream rpt)
            throws IOException;
    
    /**
     * Should return true if Rule can be used globally (i.e.&nbsp;in mangen.properties). 
     */
    public boolean isUsableGlobally();
    
    /**
     * Should return true if Rule can be used locally (i.e.&nbsp;in a bundle's manifest). 
     */
    public boolean isUsableLocally();
    
}
