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

import org.apache.felix.tool.mangen.BundleJar;
import org.apache.felix.tool.mangen.GenericHandlerItemImpl;
import org.apache.felix.tool.mangen.OsgiPackage;
import org.apache.felix.tool.mangen.PropertyManager;
import org.apache.felix.tool.mangen.Report;

/**
 *
 * Produce a report for each bundle that can be used as an OBR descriptor
 *
 * @version $Revision: 14 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class ObrReport
        extends GenericHandlerItemImpl
        implements Report
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    public static String getObrProperty(String key)
    {
        String descrKey = key + "." + PropertyManager.getProperty("mangen.obr.ver", "1");
        return PropertyManager.getProperty(descrKey, "");        
    }

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public ObrReport()
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
            // only process JARs that don't match exclusion names
            if (!isJarNameMatch(bund.getName(), "skip-jars"))
            {
                doReport(rpt, bund);
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
        String descrText = getObrProperty("mangen.obr.descr");
        rpt.println(expandTags(descrText, bund, null));
    }

    /**
     * Expands the tags in the OBR descriptor text. General method for 
     * tag expansion. Will be called either from main bundle iteration
     * loop to expand the complete OBR descriptor template, and also recursively from
     * within individual tags to expand package specific template details.
     */
    protected String expandTags(String descr, BundleJar bund, OsgiPackage pkg)
    {
        StringBuffer expanded = new StringBuffer();
        
        int off = 0;
        int tagPos = descr.indexOf("@@", off);
        
        while (tagPos != -1)
        {
            expanded.append(descr.substring(off, tagPos));
            off = descr.indexOf("@@", tagPos+2);
            processTag(descr.substring(tagPos+2, off), expanded, bund, pkg);
            off += 2;
            tagPos = descr.indexOf("@@", off);
        }
        // final segment
        expanded.append(descr.substring(off));
        return expanded.toString();
    }
    
    
    /**
     * Expand single tag in the OBR descriptor text
     */
    protected void processTag(String tag, StringBuffer buf, BundleJar bund, OsgiPackage pkg)
    {
        String tagVal = "";
        
        if (tag.startsWith("hdr:"))
        {
            tagVal = bund.getManifestHeader(tag.substring(4), true);
        }
        else if (tag.equals("imports"))
        {
            Set imports = bund.getPossibleImports();
            String template = getObrProperty("mangen.obr.import");
            tagVal = expandPackageRefs(imports, template);
        }
        else if (tag.equals("exports"))
        {
            Set exports = bund.getPossibleExports();
            String template = getObrProperty("mangen.obr.export");
            tagVal = expandPackageRefs(exports, template);
        }
        else if (tag.equals("import-ver"))
        {
            // only include this if the version is non-null
            if (pkg != null && pkg.getVersion() != null)
            {
                // recurse for version template
                String template = getObrProperty("mangen.obr.import.ver");
                tagVal = expandTags(template, null, pkg);
            }
        }
        else if (tag.equals("export-ver"))
        {
            if (pkg != null && pkg.getVersion() != null)
            {
                // recurse for version template
                String template = getObrProperty("mangen.obr.export.ver");
                tagVal = expandTags(template, null, pkg);
             }
        }
        else if (tag.equals("pkg:name"))
        {
            // this tag should only appear in templates where we've a package
            if (pkg != null)
            {
                tagVal = pkg.getName();
            }
        }
        else if (tag.equals("pkg:ver"))
        {
            // this tag should only appear in templates where we've a package
            if (pkg != null && pkg.getVersion() != null)
            {
                tagVal = pkg.getVersion().toString();
            }
        }
        
        if (tagVal != null)
        {
            buf.append(tagVal);
        }
    }
    
    /**
     * Iterate over a set of package refs and expand each one using the
     * supplied template.
     */
    protected String expandPackageRefs(Set pkgSet, String template)
    {
        StringBuffer expanded = new StringBuffer();
        
        for(Iterator i = pkgSet.iterator(); i.hasNext(); )
        {
            OsgiPackage pkg = (OsgiPackage) i.next();
            // recurse to expand tags, but this time only need to supply package
            expanded.append(expandTags(template, null, pkg));
        }
        
        return expanded.toString();
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
