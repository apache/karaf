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
import org.osgi.framework.Version;

/** 
 * Wrapper for a simple string based package name.
 *
 * @version $Revision: 31 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class OsgiStringPackage
        extends OsgiPackage
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

    public String pkgname;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    public OsgiStringPackage(String pkgname)
    {
        this.pkgname = pkgname;
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    public String getName()
    {
        return pkgname;
    }
    
    public Version getVersion()
    {
        return null;   
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
        return pkgname;
    }

    /**
     * Important to override for hash table/set key lookups. Basically just need 
     * to return hashcode of our underlying object.
     */
    public int hashCode()
    {
        return pkgname.hashCode();
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
            // R4 package comparison is complicated, so invert the compare and
            // let the R4 package do the work
            retval = ((OsgiR4Package) o).compareTo(this);
        }
        else if (o instanceof OsgiStringPackage || o instanceof String)
        {
            // simple string compare will suffice
            retval = pkgname.compareTo(o.toString());
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
