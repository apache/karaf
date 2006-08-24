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

import java.util.Iterator;
import java.util.Properties;

import org.apache.felix.framework.searchpolicy.*;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

//TODO: need to look at VersionRange handling to see whether version checking
//      should be modified to cater for version ranges.
/** 
 * Wrapper for a full R4 package name
 *
 * @version $Revision: 31 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class OsgiR4Package
        extends OsgiPackage
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    public static String       verString;

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    /**
     * Compare two sets of packages versions.
     */
    public static int compareVersions(Version v1, Version v2)
    {
        int retval = -1;
        
        if (v1 != null)
        {
            v1.compareTo(v2);
        }
        else if (v2 != null)
        {
            v2.compareTo(v1);
        }
        else
        {
            retval = 0;
        }
        
        return retval;
    }

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    public R4Package    pkg;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    public OsgiR4Package(R4Package pkg)
    {
        this.pkg = pkg;
        
        if (verString == null)
        {
            String val = PropertyManager.getProperty("mangen.osgi.level", "3");
            if (val.equals("4"))
            {
                verString = Constants.VERSION_ATTRIBUTE;
            }
            else
            {
                verString = Constants.PACKAGE_SPECIFICATION_VERSION;
            }
        }
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    /**
     * Return the wrapped Oscar R4 package.
     */
    public R4Package getPackage()
    {
        return pkg;
    }
    
    
    public String getName()
    {
        return pkg.getName();
    }

    
    public Version getVersion()
    {
        Version ver = null;

        // use 'version' attribute if present, fallback to getVersion if not        
        R4Attribute verAttr = getAttribute(verString);
        
        if (verAttr != null)
        {
            String sVer = verAttr.getValue().toString();
            ver = new Version(sVer);
        }
        else
        {
            Version v = pkg.getVersion();
            if (!v.equals(Version.emptyVersion))
            {
                ver = v;
            }
        }
        
        return ver;
    }
    
    
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Important to override standard toString method for pattern matching and
     * manifest header value generation.
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer(pkg.getName());
        
        // bit over the top this - but ensures we preserve a version attribute
        // specified as 1.2 rather than converting it to 1.2.0
        R4Attribute verAttr = getAttribute(verString);
        String sVer = null;
        
        if (verAttr != null)
        {
            sVer = verAttr.getValue().toString();
        }
        else
        {
            Version v = getVersion();
            if (v != null)
            {
                sVer = v.toString();
            }
        }
        
        if (sVer != null)
        {
            str.append(";" + verString + "=");
            str.append("\"" + sVer + "\"");
        }
        
        return str.toString();
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Comparable
    //////////////////////////////////////////////////

    /**
     * The Comparable interface is used as the basis for the OsgiPackage default
     * implementation of equals() and also for the Comparator interface used to
     * determine whether set members are present. 
     */
    public int compareTo(Object o)
    {
        int retval = -1;
        
        if (o instanceof OsgiR4Package)
        {
            OsgiR4Package otherPkg = (OsgiR4Package) o;
            // first check for name match
            retval = getName().compareTo(otherPkg.getName());
            
            // check low versions match
            if (retval == 0)
            {
                retval = compareVersions(getVersion(),
                                         otherPkg.getVersion());
            }
            //TODO: Original Oscar2 mangen handled version ranges, may need to 
            //      revisit this
            // check high versions match
            //if (retval == 0)
            //{
            //    retval = compareVersions(pkg.getVersionHigh(),
            //                             otherPkg.getVersionHigh());
            //}
        }
        else if (o instanceof OsgiStringPackage || o instanceof String)
        {
            // simple package name comparison
            retval = getName().compareTo(o.toString());
        }
        else 
        {
            // prob won't do any better a job than we do, but fallback just in case
            retval = super.compareTo(o);
        }
        
        return retval;
    }
    
    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Get a specific attribute of the package.
     */
    public R4Attribute getAttribute(String name)
    {
        R4Attribute attrib = null;
        R4Attribute[] attrs = pkg.getAttributes();

        if (attrs != null)
        {
            for (int i = 0; (attrib == null) && (i < attrs.length); i++)
            {
                if (attrs[i].getName().equals(name))
                {
                    attrib = attrs[i];
                }
            }
        }
        
        return attrib;
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
