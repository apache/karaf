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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipEntry;

import org.osgi.framework.Constants;

/**
 *
 * @version $Revision: 26 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class BundleJar
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    /**  class scanner {@link Property} key */
    public static final String SCANNER_KEY = "mangen.scanner.class";
    /** Default ClassScanner implementation class name */
    public static final String DLFT_SCANNER_CLASS = "org.apache.felix.tool.mangen.BCELScanner";
    
    /** Crude match pattern for L<classname>; signatures */ 
    public static Pattern   classnamePattern = Pattern.compile("L[^;]+?;");
    
    /** Buffer for jar copying is static. No need to sweat the GC. */
    public static byte[]    copyBuf = new byte[32767];
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////

    /**
    * Gets a {@link ClassScanner} instance usinng the configured scanner implementation
     * class.
     */
    public static ClassScanner getScanner()
            throws Exception
    {
        String name = PropertyManager.getProperty(SCANNER_KEY, DLFT_SCANNER_CLASS);
        Class scanClass = Class.forName(name);
        return (ClassScanner) scanClass.newInstance();
    }
    
    /**
     * Put the supplied key and value in the specified {@link Attributes}  
     * if the value is not an empty {@link String}, otherwise remove the key 
     * from the {@link Attributes}.
     */
    public static void putValueIfNotEmpty(Attributes atts, String key, String val)
    {
        if (!val.trim().equals(""))
        {
            atts.putValue(key, val);
        }
        else
        {
            // Note that Attributes entry keys are not Strings, so we have to remove
            // them using the correct object type.
            Attributes.Name nm = new Attributes.Name(key);
            atts.remove(nm);
        }
    }
        
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    /** bundle JAR file */
    public JarFile      jarFile;
    /** {@link Manifest} from existing bundle JAR */ 
    public Manifest     manifest;
    /** Main {@link Attributes} entry from existing bundle JAR */
    public Attributes   mainAttributes;
    /** mangen {@link Attributes} entry from existing bundle JAR */
    public Attributes   mangenAttributes;
    /** Set of inner JARs processed from the bundle JAR */
    public Set          currentInnerJars = new HashSet();
    
    public Set          possibleExports = OsgiPackage.getNewSet();
    public Set          possibleImports = OsgiPackage.getNewSet();
    /** Record of all inner classes, used for tracking awkward synthetic references to inner classes */
    public Set          innerClasses = new HashSet();
    /** Sun javac synthetic class references */
    public Set          syntheticClasses = new HashSet();

    public RuleHandler  bundleRuleHandler;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    /**
     * Create a new bundle JAR instance. Processing will only be performed if
     * a rule calls the {@see #process()} method.
     */
    public BundleJar(String filename) 
            throws Exception
    {
        jarFile = new JarFile(filename);
        processManifest();
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    /**
     * Gets the name of this jar.
     */
    public String getName()
    {
        return jarFile.getName();
    }        
    
    /**
     * Returns the set of possible export packages.
     */
    public Set getPossibleExports()
    {
        return possibleExports;
    }
    
    /**
     * Returns the set of possible import packages.
     */
    public Set getPossibleImports()
    {
        return possibleImports;
    }
    
    /**
     * Returns the set of current Manifest export packages.
     */
    public Set getCurrentExports()
    {
        return OsgiPackage.createFromHeaders(mainAttributes.getValue(Constants.EXPORT_PACKAGE));
    }
    
    /**
     * Returns the set of current Manifest import packages.
     */
    public Set getCurrentImports()
    {
        return OsgiPackage.createFromHeaders(mainAttributes.getValue(Constants.IMPORT_PACKAGE));
    }
    
    /**
     * Returns the set of "fixed" export packages. These use the same manifest key
     * but specified in the mangen attributes section.
     */
    public Set getFixedExports()
    {
        return OsgiPackage.createFromHeaders(mangenAttributes.getValue(Constants.EXPORT_PACKAGE));
    }
    
    /**
     * Returns the set of "fixed" imports packages. These use the same manifest key
     * but specified in the mangen attributes section.
     */
    public Set getFixedImports()
    {
        return OsgiPackage.createFromHeaders(mangenAttributes.getValue(Constants.IMPORT_PACKAGE));
    }
    
    /**
     * Returns a specified Manifest header value, optionally checking the mangen
     * attribute set first before the main attribute set.
     */
    public String getManifestHeader(String key, boolean checkMangenAtts)
    {
        String retval = null;
        
        if (checkMangenAtts)
        {
            retval = mangenAttributes.getValue(key);
        }
        
        if (retval == null)
        {
            retval = mainAttributes.getValue(key);
        }
        
        return retval != null ? retval : "";
    }
    
    
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Process the bundle JAR. Every class's package name will be added to the
     * list of possible exports. Class files will be parsed, and the packages
     * for all referenced classes contained within them will be added to the 
     * list of possible imports.
     */
    public void process() 
            throws Exception
    {
        processJarEntries();
        processSunJDKSyntheticClassRefs();        
        // final step is to execute our own local rules
        executeBundleRules();
    }
    
   /**
     * Update the bundle jar's manifest to contain the optimised set of imports
     * and exports. Note that because of limitations in the standard JDK classes,
     * this requires copying to a new jar at present and renaming over the current
     * jar.
     */
    public void update(boolean overwrite)
            throws IOException
    {
        Manifest newManifest = updateHeaders();
        String origName = getName();
        
        File newJar = new File(origName + ".new.jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(newJar), newManifest);
        
        Enumeration en = jarFile.entries();
        while (en.hasMoreElements())
        {
            ZipEntry ze = (ZipEntry) en.nextElement();
            if (ze.getName().compareToIgnoreCase("META-INF/MANIFEST.MF") != 0)
            {
                jos.putNextEntry(ze);
                copy(jarFile.getInputStream(ze), jos);
            }
        }
        
        jos.close();
        
        // replace existing file if needed
        if (overwrite)
        {
            jarFile.close();
            File origFile = new File(origName);
            
            if (!origFile.delete())
            {
                throw new IOException("delete of original JAR failed");
            }
            
            if (!newJar.renameTo(origFile))
            {
                throw new IOException("rename of new JAR failed");
            }
        }
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////

    /**
     * Process the Manifest for this Jar file. Need to retrieve any existing
     * imports and exports for this Jar file and also determine which inner
     * jars we should scan.
     */
    protected void processManifest()
        throws IOException
    {
        manifest = jarFile.getManifest();
        if (manifest == null)
        {
            manifest = new Manifest();
        }
        
        mainAttributes = manifest.getMainAttributes();        
        if (mainAttributes == null)
        {
            mainAttributes = new Attributes();
        }
        
        String val = mainAttributes.getValue(Constants.BUNDLE_CLASSPATH);
        if (val != null)
        {
            parseBundleClassPath(val, currentInnerJars);
        }

        // look for mangen rules in manifest 
        mangenAttributes = manifest.getAttributes("com/ascert/openosgi/mangen");
        if (mangenAttributes == null)
        {
            mangenAttributes = new Attributes();
        }
        
        bundleRuleHandler = new RuleHandler(mangenAttributes);
    }

    /** 
     * Parse the OSGi bundle classpath and add all jars to set of inner jars
     */
    public void parseBundleClassPath(String path, Set innerJars)
    {
        StringTokenizer tok = new StringTokenizer(path, ",");
        while (tok.hasMoreTokens())
        {
            String name = tok.nextToken();
            if (name.endsWith(".jar"))
            {
                innerJars.add(name.trim());
            }
        }
    }
    
    /**
     * Process the set of entries in the main Jar file. Every .class entry will
     * have it's package name added to the list of possible exports if needed, and
     * will be parsed to determine if it contains new imports.
     */
    protected void processJarEntries()
        throws Exception
    {
        Enumeration en = jarFile.entries();
        while (en.hasMoreElements())
        {
            ZipEntry ze = (ZipEntry) en.nextElement();
            String name = ze.getName();
            if (name.endsWith(".class"))
            {
                addPossibleExport(name);
                InputStream is = jarFile.getInputStream(ze);
                processClassEntry(is, name);
                is.close();
            }
            else if (name.endsWith(".jar"))
            {
                JarInputStream jis = new JarInputStream(jarFile.getInputStream(ze));
                processInnerJar(jis, name);
                jis.close();
            }
        }
    }
    
    /**
     * Parse and process an inner jar in the supplied InputStream. 
     *
     * At present we only process inner jars that are on the current bundle classpath. 
     * Since we're a manifest generator, we could also have rules to automatically
     * process matching inner jars we find and also generate an appropriate 
     * bundle classpath. 
     */
    public void processInnerJar(JarInputStream jis, String jarName)
        throws Exception
    {
        if (currentInnerJars.contains(jarName))
        {
            // Loop through JAR entries.
            for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry())
            {
                String name = je.getName();
                if (name.endsWith(".class"))
                {
                    addPossibleExport(name);
                    processClassEntry(jis, name);
                }
            }
        }
    }
    
    /**
     * Parse and process a class entry in the supplied InputStream.
     *
     */
    public void processClassEntry(InputStream is, String name)
        throws Exception    
    {
        // need to track inner classes for Sun synthetic class name handling
        if (name.indexOf('$') != -1)
        {
            addToInnerClasses(name);
        }
        
        ClassScanner scanner = getScanner();
        scanner.scan(is, name);
        scanConstantsClasses(scanner);
        scanFields(scanner);
        scanMethods(scanner);
    }
    
    /**
     * Map the supplied name into a package name and add to the target set if 
     * it is a new package name.
     */
    protected void addToPackageSet(String itemName, Set targetSet)
    {
        int lastPathSep = itemName.lastIndexOf('/');
        if (lastPathSep != -1)
        {
            String pkg = itemName.substring(0, lastPathSep);
            pkg = pkg.replace('/', '.');
            
            if (!targetSet.contains(pkg))
            {
                targetSet.add(OsgiPackage.createStringPackage(pkg));
            }
        }
    }
    
    
    /**
     * Add name possible exports if it contains a new package name.
     */
    protected void addPossibleExport(String name)
    {
        addToPackageSet(name, possibleExports);
    }
    
    /**
     * Add classname to list of inner classes
     */
    protected void addToInnerClasses(String name)
    {
        int suffix = name.lastIndexOf(".class");
        String justName = name.substring(0, suffix);
        
        if (!innerClasses.contains(justName))
        {
            innerClasses.add(justName);
        }
    }
    
    /**
     * Parse the supplied signature string, extract all L<class>; format
     * class references and adding them to the specified set.
     */
    protected boolean extractClassesFromSignature(String signature, Set set)
    {
        boolean matched = false;
        Matcher m = classnamePattern.matcher(signature);
        
        while (m.find())
        {
            matched = true;
            String classname = m.group();
            //System.out.println("match: " + classname);
            addToPackageSet(classname.substring(1, classname.length() - 1), set);
        }
        
        return matched;
    }
    
    
    /**
     * Scan the constant pool of the parsed java class for any ConstantClass references.
     * Add any found into the list of possible import packages.
     */
    protected void scanConstantsClasses(ClassScanner scanner) 
    {
        for(int ix=0; ix < scanner.getConstantClassCount(); ix++)
        {
            String classRef = scanner.getConstantClassSignature(ix);
            
            MangenMain.trace("ConstantClass : " + classRef);
            
            if (classRef.startsWith("["))
            {
              // array classname
              extractClassesFromSignature(classRef, possibleImports);
            }
            else
            {
              // simple classname
              addToPackageSet(classRef, possibleImports);
            }
        }
    }
    
    /**
     * Scan the fields of the parsed java class for all class references.
     * Add any found into the list of possible import packages.
     */
    protected void scanFields(ClassScanner scanner) 
    {
        for(int ix=0; ix < scanner.getFieldCount(); ix++) 
        {
            String name = scanner.getFieldName(ix);
            String sig = scanner.getFieldSignature(ix);
            
            MangenMain.trace("Field : name=" + name + ", sig=" + sig);

            if (scanner.isSyntheticField(ix))
            {
                handleSunJDKSyntheticClassRefs(name);
            }
            extractClassesFromSignature(sig, possibleImports);
        }
    }
    
    /**
     * Scan the methods of the parsed java class for all class references.
     * Add any found into the list of possible import packages.
     */
    protected void scanMethods(ClassScanner scanner) 
    {
        for(int ix=0; ix < scanner.getMethodCount(); ix++) 
        {
            String name = scanner.getMethodName(ix);
            String sig = scanner.getMethodSignature(ix);
            
            MangenMain.trace("Method : name=" + name + ", sig=" + sig);
            
            extractClassesFromSignature(sig, possibleImports);
        }
    }
    
    /**
     * The Sun JDK javac generates synthetic fields with a name of 
     *  class$packagename$classname for classes that are directly referenced 
     * in code as opposed to being used in methods and fields.
     *
     * First stage is to store all of these references ready for post-processing.
     */
    protected void handleSunJDKSyntheticClassRefs(String name)
    {
        if (name.startsWith("class$"))
        {
            syntheticClasses.add(name.substring(6));
        }
    }
    
    /**
     * Post-processing of Sun JDK javac generated synthetic fields.
     *
     * The general case is to handle these by unmangling the generated name and
     * create an import reference for it. A special case exists for inner class
     * references which need the last inner class reference removed.
     *
     * Not a perfect solution, but since this is a special case of dynamic
     * classloading without actually executing the bytecode or looking for code
     * patterns it's a reasonable compromise.
     */
    protected void processSunJDKSyntheticClassRefs()
    {
        for(Iterator i = syntheticClasses.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();
            
            // check for inner class case
            int lastSep = name.lastIndexOf('$');
            if (lastSep != -1)
            {
                String possInnerClass = name.substring(0, lastSep).replace('$','/') +
                                        name.substring(lastSep);
                                        
                if (innerClasses.contains(possInnerClass))
                {
                    // strip off last $ component, which is the inner class name
                    name = name.substring(0, lastSep);
                }
            }
            
            String classname = name.replace('$','/');
            addToPackageSet(classname, possibleImports);
        }
    }

    /**
     * Execute any local i.e. bundle specific rules.
     */
    protected void executeBundleRules()
    {
        if (bundleRuleHandler != null)
        {
            ArrayList dummyList = new ArrayList();
            dummyList.add(this);
            bundleRuleHandler.executeRules(dummyList);
        }
    }
    
    /**
     * Copy inputstream to output stream. Main use is Jar updating to create new
     * jar.
     */
    protected void copy(InputStream is, OutputStream os)
            throws IOException
    {
        int len = 0;
        
        while(len != -1)
        {
            len = is.read(copyBuf, 0, copyBuf.length);
            if (len > 0)
            {
                os.write(copyBuf, 0, len);
            }
        }
    }
    
    /**
     * Update the manifest headers based on the processed state of imports, 
     * exports etc.
     */
    protected Manifest updateHeaders()
    {
        Manifest newManifest = new Manifest(manifest);
        Attributes newAtts = newManifest.getMainAttributes();
        
        // First determine whether to mark for R3 or R4 usage
        String val = PropertyManager.getProperty("mangen.osgi.level", "3");
        if (val.equals("4"))
        {
            newAtts.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        }
        else
        {
            newAtts.putValue(Constants.BUNDLE_MANIFESTVERSION, "1");
        }

        putValueIfNotEmpty(newAtts, Constants.EXPORT_PACKAGE, getAsHeaderValue(possibleExports)); 
        putValueIfNotEmpty(newAtts, Constants.IMPORT_PACKAGE, getAsHeaderValue(possibleImports)); 
        //TODO: implement generation of bundle classpath if mangen.innerjar.auto set
        
        return newManifest;
    }
    
    /**
     * Get the specified set of packages as a String of values suitable for use in 
     * a manifest header. 
     */
    protected String getAsHeaderValue(Set set)
    {
        StringBuffer str = new StringBuffer();
        boolean first = true;
        
        for(Iterator i = set.iterator(); i.hasNext(); )
        {
            OsgiPackage pkg = (OsgiPackage) i.next();
            if (first)
            {
                str.append(pkg.toString());
                first = false;
            }
            else
            {
                str.append(", " + pkg.toString());
            }
        }
        
        return str.toString();
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
