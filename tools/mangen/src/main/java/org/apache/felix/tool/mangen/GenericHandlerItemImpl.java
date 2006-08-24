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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holder for common item handler methods, such as standard option processing
 *
 * @version $Revision: 14 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public abstract class GenericHandlerItemImpl
        implements GenericHandlerItem
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    /**
     * Makes perfect sense that JDK Set would have no "get" operation since, 
     * in theory to do a contains(), you must alreay have the Object. Since we
     * subvert that a little by allowing comparisons with different object types,
     * it doesn't quite work for our case though.
     */
    public static OsgiPackage getPackageFromSet(OsgiPackage pkg, Set set)
    {
        OsgiPackage retval = null;
        // do search based on package name only, to ensure we find occurences of
        // different types
        String name = pkg.getName();
        
        for(Iterator i = set.iterator(); retval == null && i.hasNext(); )
        {
            OsgiPackage setPkg = (OsgiPackage) i.next();
            if (setPkg.equals(name))
            {
                retval = setPkg;
            }
        }
        
        return retval;
    }
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    /** Map contain parsed options */
    public Map                      optionMap = new HashMap();
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////

    public GenericHandlerItemImpl()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Test whether the supplied package matches one of the set of patterns
     * for the specified qualifier.
     */
    protected boolean isPackageMatch(OsgiPackage pkg, String qualName)
    {
        return getMatchingPatternString(pkg, qualName, false) != null ? true : false;
    }
    
    /**
     * Get's the pattern string that matches the supplied package in the specified
     * qualifier set. Optionally, the matching algorithm can be instructed to only
     * match on package name, which will exclude matching any package attributes 
     * after the ";" specifier.
     */
    protected String getMatchingPatternString(OsgiPackage pkg, String qualName, boolean nameMatchOnly)
    {
        String pattString = null;
        Set pattSet = (Set) optionMap.get(qualName);

        if (pattSet != null)
        {
            for(Iterator i = pattSet.iterator(); pattString == null && i.hasNext(); )
            {
                Pattern origPatt = (Pattern) i.next();
                Pattern patt = origPatt;
                String matchString = pkg.toString();
                
                if (nameMatchOnly)
                {
                    matchString = pkg.getName();
                    // need to strip off any attributes and recompile pattern
                    String fullStr = patt.pattern();
                    int delim = fullStr.indexOf(';');
                    if (delim != -1)
                    {
                        patt = Pattern.compile(fullStr.substring(0, delim));
                    }
                    
                }
                
                Matcher matcher = patt.matcher(matchString);
                if (matcher.matches())
                {
                    pattString = origPatt.pattern();
                }
            }
        }
        
        return pattString;
    }
    
    /**
     * Test whether the supplied jar name matches one of the set of patterns
     * for the specified qualifier.
     */
    protected boolean isJarNameMatch(String jarName, String qualName)
    {
        boolean found = false;
        Set pattSet = (Set) optionMap.get(qualName);

        if (pattSet != null)
        {
            for(Iterator i = pattSet.iterator(); !found && i.hasNext(); )
            {
                Pattern patt = (Pattern) i.next();
                
                Matcher matcher = patt.matcher(jarName);
                if (matcher.matches())
                {
                    found = true;
                }
            }
        }
        
        return found;
    }
    
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - GenericHandlerItem
    //////////////////////////////////////////////////
    
    /**
     * Process the option set. The GenericRule will parse and separate a space 
     * separated list of qualifiers in the following format.
     * 
     *      qual1(<qual1-options>) qual2(<qual2-options>) 
     */
    public void setOptions(String options)
    {
        for (StringTokenizer tok = new StringTokenizer(options, " "); tok.hasMoreTokens(); )
        {
            String qualifier = tok.nextToken().trim();
            if (qualifier.startsWith("sys-packages") || 
                qualifier.startsWith("imports") ||
                qualifier.startsWith("exports") ||
                qualifier.startsWith("skip-jars") )
            {
                processStandardQualifier(qualifier);
            }
            else
            {
                processNonStandardQualifier(qualifier);
            }
        }
    }
    
    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Hook for subclasses to include own qualifier processing if they wish to
     * use this model with non-standard qualifiers
     */
    protected void processNonStandardQualifier(String qual)
    {
    }
    
    /**
     * Process a standard qualifier. This will contain a qualifier keyword, followed
     * by a comma separated list of patterns enclosed in brackets:
     *
     *      qual1(patt1, patt2, patt3)
     *
     */
    protected void processStandardQualifier(String qual)
    {
        int start = qual.indexOf('(');
        int end = qual.lastIndexOf(')');
        
        if (start == -1 || end == -1)
        {
            throw new IllegalArgumentException("badly formed rule qualifier: " + qual);
        }
            
        String qualName = qual.substring(0, start).trim();
        
        // Process the comma separated list of packages
        String list = qual.substring(start+1, end);
        
        // get any existing set for this option name, create new set if none
        Set pattSet = (Set) optionMap.get(qualName);
        if (pattSet == null)
        {
            pattSet = new HashSet();
        }
        
        for (StringTokenizer tok = new StringTokenizer(list, ","); tok.hasMoreTokens(); )
        {
            Pattern patt = Pattern.compile(tok.nextToken().trim());
            pattSet.add(patt);
        }
        
        optionMap.put(qualName, pattSet);
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
