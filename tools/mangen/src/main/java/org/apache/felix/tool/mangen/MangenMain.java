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

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @version $Revision: 29 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class MangenMain
{    
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    public static final String  PROP_FILE = "mangen.properties";
    
    public static ArrayList     jarList = new ArrayList();
    public static HashMap       fileMap = new HashMap();
    
    public static RuleHandler[] ruleSets;
    public static ReportHandler reportHandler;

    public static boolean optTrace;   
    public static boolean optFailOnError;
    public static boolean optFailOnWarning;
    
    public static int errorCount = 0;
    public static int warningCount = 0;
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    /** 
     * Main entry point to mangen from command line. Argument list is expected to 
     * be a list of jars, or directories which will be scanned for jar, or a
     * combination of both.
     */
    public static void main(String[] args)
            throws Exception
    {
        long start = System.currentTimeMillis();
        
        PropertyManager.initProperties(PROP_FILE);
        processOptions();
        
        ruleSets = RuleHandler.initRuleSets();
        reportHandler = new ReportHandler();
        
        initBundleJarList(args);
        
        RuleHandler.runRuleSets(ruleSets, jarList);
        reportHandler.runReports(System.out, jarList);
        
        long stop = System.currentTimeMillis();
        System.out.println("Time elapsed: " + ((double) (stop - start)/1000));

        int exitCode = 0;        
        if (optFailOnError && errorCount > 0)
        {
            exitCode = 3;
        }
        else if (optFailOnWarning && warningCount > 0)
        {
            exitCode = 5;
        }
        
        System.exit(exitCode);
    }
        
    /**
     * Increment count of errors raised and send string to output stream. Also
     * show message on <code>stderr</code> if set to fail on errors.
     */
    public static void error(PrintStream out, String msg)
    {
        errorCount++;
        out.println(msg);
        
        if (optFailOnError)
        {
            System.err.println(msg);
        }
    }

    /**
     * Increment count of warnings raised and send string to output stream. Also
     * show message on <code>stderr</code> if set to fail on warnings.
     */
    public static void warning(PrintStream out, String msg)
    {
        warningCount++;
        out.println(msg);
        
        if (optFailOnWarning)
        {
            System.err.println(msg);
        }
    }
    
    /**
     * Generate trace output if enabled
     */
    public static void trace(String msg)
    {
        if (optTrace)
        {
            System.out.println("TRACE - " + msg);
        }
    }
    
    //////////////////////////////////////////////////
    // STATIC PRIVATE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Process each of the command line arguments. Any files ending .jar are treated
     * as bundle jars. Any directories will be descended into to 
     * process any .jar files contained in the directory tree beneath. 
     */
    private static void initBundleJarList(String[] args)
            throws Exception
    {
        // params are an array of jar files which may have partially compelete manifests
        for (int ix = 0; ix < args.length; ix++)
        {
            if (args[ix].endsWith(".jar"))
            {
                initJar(args[ix]);
            }
            else
            {
                File f = new File(args[ix]);
                if (f.isDirectory())
                {
                    processDir(f);
                }
            }
        }
    }
    
    /**
     * Process a directory argument. Process all files and subdirectories.
     */
    private static void processDir(File dir)
            throws Exception
    {
        //TODO: exception handling cases?
        String[] files = dir.list();
        for (int ix = 0; ix < files.length; ix++)
        {
            files[ix] = dir.getPath() + File.separator + files[ix];
        }
        initBundleJarList(files);
    }
    
    /**
    * Create a new {@see BundleJar} and add it to the list of bundle JARs. 
     */
    private static void initJar(String jarName)
            throws Exception
    {
        try
        {   
            // normalize path separators for checking
            String absName = new File(jarName).getAbsolutePath();
            if (!fileMap.containsKey(absName))
            {
                BundleJar bund = new BundleJar(absName);
                fileMap.put(absName, bund);
                jarList.add(bund);
            }
            else
            {
                trace("skipping repeated filename: " + absName);
            }
        }
        catch (RuntimeException re)
        {
            System.err.println("Exception: " + re + ", skipping bundle jar: " + jarName);
            re.printStackTrace(System.err);
        }
        catch (Exception e)
        {
            System.err.println("Exception: " + e + ", skipping bundle jar: " + jarName);
        }
    }
    
    /**
     * Process runtime options and defaults.
     */
    private static void processOptions()
    {
        String opt = PropertyManager.getProperty("mangen.trace", "off");
        if (opt.compareToIgnoreCase("on") == 0)
        {
            optTrace = true;
        }
        else
        {
            optTrace = false;
        }
        
        opt = PropertyManager.getProperty("mangen.failonerror", "on");
        if (opt.compareToIgnoreCase("on") == 0)
        {
            optFailOnError = true;
        }
        else
        {
            optFailOnError = false;
        }
        
        opt = PropertyManager.getProperty("mangen.failonwarning", "off");
        if (opt.compareToIgnoreCase("on") == 0)
        {
            optFailOnWarning = true;
        }
        else
        {
            optFailOnWarning = false;
        }
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

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
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
