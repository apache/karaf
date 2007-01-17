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

import java.util.*;

import org.apache.felix.framework.util.manifestparser.R4Package;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.osgi.framework.Version;

/** 
 * The OsgiPackage class is a wrapper for either simple string based package
 * names or full Osgi package objects containing attributes and versions etc.
 *
 * @version $Revision: 31 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public abstract class OsgiPackage
        implements Comparable
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    public static Comparator pkgComparator = new PackageComparator();
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    /**
     * Create a package wrapper for a simple package name string.
     */     
    public static OsgiPackage createStringPackage(String pkgName)
    {
        return new OsgiStringPackage(pkgName);
    }

    /**
     * Create a set of OSGi package wrappers by parsing the supplied import
     * or export headers. At present, we only create true R4 packages for header
     * strings which contain ";" (the header attribute separator char). All
     * others we create as simple string packages
     */
    public static Set createFromHeaders(String headers, boolean export)
    {
        Set set = getNewSet();
        
        if (headers != null)
        {
            for (StringTokenizer tok = new StringTokenizer(headers, ","); tok.hasMoreTokens(); )
            {
                String pkgString = tok.nextToken().trim();
                
                // look for presence of package attribute separator ';'
                if (pkgString.indexOf(';') != -1)
                {
                    // parse and add all R4 packages contained (can be multiple)
                    R4Package[]  pkgs = ManifestParser.parseImportExportHeader(pkgString, export);
                    for (int ix = 0; ix < pkgs.length; ix++)
                    {
                        set.add(new OsgiR4Package(pkgs[ix]));
                    }
                }
                else
                {
                    // treat as a simple string package name
                    set.add(new OsgiStringPackage(pkgString));
                }
            }
        }
        
        return set;
    }

    /**
     * Creates and returns a new Set for holding package entries.
     */
    public static Set getNewSet()
    {
        return new TreeSet(pkgComparator);
    }
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    /**
     * Return simple package name form without attributes
     */
    public abstract String getName();

    /**
     * Return specified package version, or <code>null</code> if none defined. 
     */
    public abstract Version getVersion();
    
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////

    /**
     * Very important to override standard equals method, since set based operations
     * work using this method for comparisons
     */
    public boolean equals(Object o)
    {
        return compareTo(o) == 0 ? true : false;
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Comparable
    //////////////////////////////////////////////////

    /**
     * Just used for sorting in reports
     */
    public int compareTo(Object o)
    {
        return -1;
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

    public static class PackageComparator
            implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            int retval = -1;
            
            if (o1 instanceof OsgiPackage)
            {
                retval = ((Comparable) o1).compareTo(o2);
            }
            else if (o2 instanceof OsgiPackage)
            {
                retval = ((Comparable) o2).compareTo(o1);
            }
            else
            {
                throw new ClassCastException("can't compare, neither o1 or o2 is an OsgiPackage");
            }
            
            return retval;
        }
    }
    
    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    
}
