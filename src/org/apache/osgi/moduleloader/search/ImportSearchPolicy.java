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
package org.apache.osgi.moduleloader.search;

import java.net.URL;
import java.util.*;

import org.apache.osgi.moduleloader.*;

/**
 * <p>
 * This class implements a <tt>ModuleLoader</tt> search policy to support
 * modules that import and export classes and resources from/to one another.
 * Modules import from other modules by specifying a set of import identifiers
 * and associated version numbers. Modules export their classes and
 * resources by specifying a set of export identifiers and associated
 * versions. Exports for a given module are also treated as imports for that module,
 * meaning that it is possible for a module that exports classes to not use
 * the classes it exports, but to actually use classes that are exported from
 * another module. This search policy requires the following meta-data
 * attributes be attached to each module:
 * </p>
 * <ul>
 *   <li><tt>ImportSearchPolicy.EXPORTS_ATTR</tt> - the "<tt>exports</tt>"
 *       meta-data attribute is used to declare the module's exports,
 *   </li>
 *   <li><tt>ImportSearchPolicy.IMPORTS_ATTR</tt> - the "<tt>imports</tt>"
 *       meta-data attribute is used to declare the module's imports,
 *   </li>
 *   <li><tt>ImportSearchPolicy.PROPAGATES_ATTR</tt> - the "<tt>propagates</tt>"
 *       meta-data attribute is used to declare which imports are exposed or
 *       "propagated" to clients of the module's exports, and
 *   </li>
 *   <li><tt>ImportSearchPolicy.VALID_ATTR</tt> - the "<tt>valid</tt>"
 *       meta-data attribute signifies the current <i>validation</i> status
 *       of the module (this will be defined more fully below).
 *   </li>
 * </ul>
 * <p>
 * The value of the <tt>ImportSearchPolicy.EXPORTS_ATTR</tt> attribute is
 * an array of <tt>Object</tt> arrays, i.e., <tt>Object[][]</tt>. Each element
 * in the array signifies a particular export that is offered by this
 * associated module. Each element is an array triple of
 * <tt>Object</tt>, where the index into this triple is:
 * </p>
 * <ul>
 *   <li><tt>ImportSearchPolicy.IDENTIFIER_IDX</tt> - the first element
 *       is the export identifier object, used to identify the
 *       export target. The export identifier does not have any special
 *       meaning to the search policy and any value is allowed. A
 *       typical identifier might be the package name of the exported classes,
 *       such as <tt>javax.servlet</tt>.
 *   </li>
 *   <li><tt>ImportSearchPolicy.VERSION_IDX</tt> - the second element
 *       is the export version number. The version number does not have
 *       any special meaning to the search policy and any value is allowed.
 *       A typical version number might be major, minor, and release number.
 *   </li>
 *   <li><tt>ImportSearchPolicy.RESOLVING_MODULE_IDX</tt> - the third element
 *       is the resolving module for this export; since exports are treated like
 *       imports, it is possible that the resolving module will not be the
 *       exporting module itself. This value is filled in automatically by the
 *       search policy and is initially <tt>null</tt>.
 *   </li>
 * </ul>
 * </p>
 * <p>
 * The value of the <tt>ImportSearchPolicy.IMPORTS_ATTR</tt> attribute is
 * essentially the same as the <tt>ImportSearchPolicy.EXPORTS_ATTR</tt> defined
 * above; the only difference is that the array of versioned identifiers denote
 * import targets rather than exports.
 * </p>
 * <p>
 * The value of the <tt>ImportSearchPolicy.PROPAGATES_ATTR</tt> attribute is
 * an array of <tt>Object</tt>s, i.e., <tt>Object[]</tt>. Each element in the
 * array is an identifier of a propagated import target from the
 * <tt>ImportSearchPolicy.IMPORTS_ATTR</tt> attribute. Only identifiers for
 * import targets are candidates for inclusion and the version number is
 * unnecessary since it is assumed from the corresponding import target.
 * </p>
 * <p>
 * The value of the <tt>ImportSearchPolicy.VALID_ATTR</tt> attribute is a
 * <tt>Boolean</tt>. The value is initially set to <tt>Boolean.FALSE</tt>
 * and indicates that the module has not yet been validated. After the module
 * is validated, the value is set to <tt>Boolean.TRUE</tt>. The search policy
 * automatically adds this attribute to all modules and maintains its value.
 * </p>
 * <p>
 * These meta-data attributes help the search policy enforce consistency
 * using a process called <i>validation</i>; validation ensures that classes
 * and resources are only loaded from a module whose imports are satisfied.
 * Therefore, a <i>valid</i> module is a module whose imports are satisfied and
 * an <i>invalid</i> module is a module whose imports are not yet satisfied.
 * An invalid module may be invalid for two reasons:
 * </p>
 * <p>
 * <ol>
 *   <li>Its imports are not available or</li>
 *   <li>It has not yet been validated.</li>
 * </ol>
 * </p>
 * <p>
 * These two possibilities arise due to the fact that module validation
 * is not performed until it is necessary (i.e., lazy evaluation). A module
 * is automatically validated when an attempt is made to get classes or
 * resources from it, although it is possible to manually validate a module.
 * For a given module, called <tt>M</tt>, the validation process attempts to
 * find an exporting module for every import target of <tt>M</tt>. If an
 * exporter is not found for a specific import target, then the validation of
 * module <tt>M</tt> fails. If an exporting module is found, then this module
 * is also validated, if it is not already. As a result, the validation of
 * module <tt>M</tt> depends on the validation of the transitive closure of
 * all modules on which <tt>M</tt> depends. It is also possible for modules
 * to exhibit dependency cycles; circular dependencies are allowed.
 * Initially, a module's <tt>VALID_ATTR</tt> is set to <tt>Boolean.FALSE</tt>,
 * but after the module is successfully validated, this attribute is set to
 * <tt>Boolean.TRUE</tt>.
 * </p>
 * <p>
 * Besides ensuring that every import target is resolved to an appropriate
 * exporting module, the validation process also attempts to maintain
 * consistency along "propagation" chains. Propagation occurs when a module
 * imports classes that are also visible from its own exports; for example,
 * an HTTP server implementation may import classes from <tt>javax.servlet</tt>
 * and export classes that have methods that use the type <tt>javax.servlet.Servlet</tt>
 * in their signatures. Monitoring these types of occurences is important
 * to uncover import source and version conflicts when multiple sources or
 * versions of an import target are available within one virtual machine. When
 * a module <tt>M</tt> is validated, the propagation information of each
 * module that resolves the imports of <tt>M</tt> is checked to ensure
 * that they do not propagate conflicting sources of <tt>M</tt>'s
 * imports; specifically, it is verified that all propagators of a
 * particular import target have the same source module for that import
 * target.
 * </p>
 * <p>
 * To facilitate applicability in as many scenarios as possible, this search
 * policy delegates some decisions via additional policy interfaces. The following
 * two policy interfaces must be specified by the code that instantiates the
 * <tt>ImportSearchPolicy</tt> object:
 * </p>
 * <ul>
 *   <li><tt>CompatibilityPolicy</tt> - this policy is used to determine
 *       whether import/export version numbers are compatible.
 *   </li>
 *   <li><tt>SelectionPolicy</tt> - this policy is used to resolve a specific
 *       import target when multiple candidate exporting modules exist.
 *   </li>
 * </ul>
 * <p>
 * Once an instance is created with definitions of the above policy interfaces,
 * this search policy will operate largely self-contained. There are a few utility
 * methods for manually validating modules, adding validation listeners, and
 * access meta-data attributes, but for the most part these are not necessary
 * except for implementing more sophisticated infrastructure.
 * </p>
 * <p>
 * The follow snippet of code illustrates a typical usage scenario for
 * this search policy:
 * </p>
 * <pre>
 *     ...
 *     ImportSearchPolicy searchPolicy =
 *         new ImportSearchPolicy(
 *             new MyCompatibilityPolicy(), new MySelectionPolicy());
 *     ModuleManager mgr = new ModuleManager(searchPolicy);
 *     ...
 *     Object[][] exports = new Object[][] {
 *         { "org.apache.jasper", "2.1.0", null }
 *     };
 *     Object[][] imports = new Object[][] {
 *         { "javax.servlet", "2.3.1", null }
 *     };
 *     Object[][] attributes = new Object[][] {
 *         new Object[] { ImportSearchPolicy.EXPORTS_ATTR, exports },
 *         new Object[] { ImportSearchPolicy.IMPORTS_ATTR, imports },
 *         new Object[] { ImportSearchPolicy.PROPAGATES_ATTR, new Object[] { "javax.servlet" } }
 *      };
 *     ResourceSource[] resSources = new ResourceSource[] {
 *         new JarResourceSource(file1)
 *         new JarResourceSource(file2)
 *     };
 *     Module module = mgr.addModule(id, attributes, resSources, null);
 *     ClassLoader loader = module.getClassLoader();
 *     // Assuming that all imports are satisfied...
 *     Class clazz = loader.loadClass("org.foo.MyClass");
 *     ...
 * </pre>
 * <p>
 * The above code snippet illustrates creating a module with one export and one
 * import, where the import is also propagated via the module's export. The module
 * has multiple resource sources, but no library sources.
 * </p>
 * @see org.apache.osgi.moduleloader.SearchPolicy
 * @see org.apache.osgi.moduleloader.Module
 * @see org.apache.osgi.moduleloader.ModuleClassLoader
 * @see org.apache.osgi.moduleloader.ModuleManager
**/
public class ImportSearchPolicy implements SearchPolicy, ModuleListener
{
    /**
     * This is the name of the "exports" meta-data attribute that
     * should be attached to each module. The value of this attribute
     * is of type <tt>Object[][]</tt> and is described in the overview
     * documentation for this class.
    **/
    public static final String EXPORTS_ATTR = "exports";
    /**
     * This is the name of the "imports" meta-data attribute that
     * should be attached to each module. The value of this attribute
     * is of type <tt>Object[][]</tt> and is described in the overview
     * documentation for this class.
    **/
    public static final String IMPORTS_ATTR = "imports";
    /**
     * This is the name of the "propagates" meta-data attribute that
     * should be attached to each module. The value of this attribute
     * is of type <tt>Object[]</tt> and is described in the overview
     * documentation for this class.
    **/
    public static final String PROPAGATES_ATTR = "propagates";
    /**
     * This is the name of the "valid" meta-data attribute that is
     * automatically attached to each module. The value of this attribute
     * is of type <tt>Boolean</tt> and is described in the overview
     * documentation for this class.
    **/
    public static final String VALID_ATTR = "valid";

    /**
     * This is the index used to retrieve the import or export identifier
     * from a given element of the <tt>EXPORTS_ATTR</tt> or the <tt>IMPORTS_ATTR</tt>
     * attribute.
    **/
    public static final int IDENTIFIER_IDX = 0;
    /**
     * This is the index used to retrieve the import or export version number
     * from a given element of the <tt>EXPORTS_ATTR</tt> or the <tt>IMPORTS_ATTR</tt>
     * attribute.
    **/
    public static final int VERSION_IDX = 1;
    /**
     * This is the index used to retrieve the resolving module for an import
     * or export target from a given element of the <tt>EXPORTS_ATTR</tt> or
     * the <tt>IMPORTS_ATTR</tt> attribute.
    **/
    public static final int RESOLVING_MODULE_IDX = 2;

    private ModuleManager m_mgr = null;
    private CompatibilityPolicy m_compatPolicy = null;
    private SelectionPolicy m_selectPolicy = null;
    private ValidationListener[] m_listeners = null;
    private String[] m_searchAttrs = { IMPORTS_ATTR, EXPORTS_ATTR };
    private static final ValidationListener[] m_noListeners = new ValidationListener[0];

    /**
     * Constructs an import search policy instance with the supplied
     * compatibility and selection policies.
     * @param compatPolicy the compatibility policy implementation to be used
     *        by the search policy.
     * @param selectPolicy the selection policy implementation to be used
     *        by the search policy.
    **/
    public ImportSearchPolicy(
        CompatibilityPolicy compatPolicy,
        SelectionPolicy selectPolicy)
    {
        m_compatPolicy = compatPolicy;
        m_selectPolicy = selectPolicy;
        m_listeners = m_noListeners;
    }

    /**
     * Returns the compatibility policy used by this import search policy instance.
     * @return the compatibility policy of this import search policy instance.
    **/
    public CompatibilityPolicy getCompatibilityPolicy()
    {
        return m_compatPolicy;
    }

    /**
     * Returns the selection policy used by this import search policy instance.
     * @return the selection policy of this import search policy instance.
    **/
    public SelectionPolicy getSelectionPolicy()
    {
        return m_selectPolicy;
    }

    // JavaDoc comment copied from SearchPolicy.
    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException
    {
        if (m_mgr == null)
        {
            m_mgr = mgr;
            m_mgr.addModuleListener(this);
        }
        else
        {
            throw new IllegalStateException("Module manager is already initialized");
        }
    }

    public Object[] definePackage(Module module, String pkgName)
    {
        return null;
    }

    /**
     * This method is part of the <tt>SearchPolicy</tt> interface; it
     * should not be called directly. This method finds a class
     * based on the import/export meta-data attached to the module.
     * It first attempts to validate the target module, if it cannot
     * be validated, then a <tt>ClassNotFoundException</tt> is thrown.
     * Once the module is validated, the module's imports are searched
     * for the target class, then the module's exports are searched.
     * If the class is found in either place, then it is returned;
     * otherwise, <tt>null</tt> is returned.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return the class if found, <tt>null</tt> otherwise.
     * @throws java.lang.ClassNotFoundException if the target module
     *         could not be validated.
    **/
    public Class findClassBeforeModule(ClassLoader parent, Module module, String name)
        throws ClassNotFoundException
    {
        // First, try to validate the originating module.
        try {
            validate(module);
        } catch (ValidationException ex) {
            throw new ClassNotFoundException(name);
        }

        // Try to load from parent.
        if (parent != null)
        {
            try
            {
                Class c = parent.loadClass(name);
                if (c != null)
                {
                    return c;
                }
            }
            catch (ClassNotFoundException ex)
            {
                // Ignore and search imports/exports.
            }
        }

        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        // We delegate to the module's imports for finding the
        // desired class first, then we delegate to the module's
        // exports for finding the desired class. We do this because
        // implicitly a module imports everything that it exports.
        // To avoid code duplication, we use a simple array of
        // attribute names to loop through both of the imports
        // and exports meta-data searching for the desired class.
        for (int attrIdx = 0; attrIdx < m_searchAttrs.length; attrIdx++)
        {
            Object[][] imports = getImportsOrExports(module, m_searchAttrs[attrIdx]);

            // If the module doesn't import anything, then just
            // return null.
            if ((imports != null) && (imports.length > 0))
            {
                for (int i = 0; i < imports.length; i++)
                {
                    // Only check when the package of the class is
                    // the same as the import package.
                    if (imports[i][IDENTIFIER_IDX].equals(pkgName))
                    {
                        Module resolvingModule = (Module) imports[i][RESOLVING_MODULE_IDX];
                        try {
                            Class clazz =
                                resolvingModule.getClassLoader().loadClassFromModule(name);
                            if (clazz != null)
                            {
                                return clazz;
                            }
                        } catch (Throwable th) {
                            // Not much we can do.
                            System.err.println("ImportSearchPolicy: " + th.getMessage());
                        }
                    }
                }
            }
        }

        return null;
    }

    public Class findClassAfterModule(ClassLoader parent, Module module, String name)
    {
        return null;
    }

    /**
     * This method is part of the <tt>SearchPolicy</tt> interface; it
     * should not be called directly. This method finds a resource
     * based on the import/export meta-data attached to the module.
     * It first attempts to validate the target module, if it cannot
     * be validated, then it returns <tt>null</tt>. Once the module is
     * validated, the module's imports are searched for the target
     * resource, then the module's exports are searched. If the resource
     * is found in either place, then a <tt>URL</tt> to is is returned;
     * otherwise, <tt>null</tt> is returned.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the resource.
     * @param name the name of the resource being loaded.
     * @return a <tt>URL</tt> to the resource if found, <tt>null</tt> otherwise.
    **/
    public URL findResource(ClassLoader parent, Module module, String name)
    {
        // First, try to validate the originating module.
        try
        {
            validate(module);
        }
        catch (ValidationException ex)
        {
            return null;
        }

        // Try to load from parent.
        if (parent != null)
        {
            URL url = parent.getResource(name);
            if (url != null)
            {
                return url;
            }
        }

        // Get the package of the target resource.
        String pkgName = Util.getResourcePackage(name);

        // We delegate to the module's imports for finding the
        // desired class first, then we delegate to the module's
        // exports for finding the desired class. We do this because
        // implicitly a module imports everything that it exports.
        // To avoid code duplication, we use a simple array of
        // attribute names to loop through both of the imports
        // and exports meta-data searching for the desired class.
        for (int attrIdx = 0; attrIdx < m_searchAttrs.length; attrIdx++)
        {
            Object[][] imports = getImportsOrExports(module, m_searchAttrs[attrIdx]);

            // If the module doesn't import or export anything,
            // then just return null.
            if ((imports != null) && (imports.length > 0))
            {
                for (int i = 0; i < imports.length; i++)
                {
                    // Only check when the package of the resource is
                    // the same as the import package.
                    if (imports[i][IDENTIFIER_IDX].equals(pkgName))
                    {
                        Module resolvingModule = (Module) imports[i][RESOLVING_MODULE_IDX];
                        try {
                            URL url =
                                resolvingModule.getClassLoader().getResourceFromModule(name);
                            if (url != null)
                            {
                                return url;
                            }
                        } catch (Throwable th) {
                        }
                    }
                }
            }
        }

        return null;
    }

    private Map m_validateMap = new HashMap();
    private Module m_rootModule = null;

    /**
     * This method validates the specified target module. If the module
     * is already validated, then this method returns immediately. This
     * method synchronizes on the associated module manager to ensure that
     * modules are not added or removed while the validation is occuring.
     * Each import and export for the target module are resolved by first
     * using the compatibility policy to create a list of candidate export
     * modules, then using the selection policy to choose among the
     * candidates. Each selected candidate is also recursively validated;
     * this process validates a transitive closure of all dependent modules.
     * After the selected candidate is validated, its propagated imports
     * are checked to make sure that they do not conflict with any existing
     * propagated imports. If no validation errors occur, then all dependent
     * modules are marked as validated, if they are not already validated.
     * If an error occurs, the valid state of all modules remains unchanged.
     * @param module the module to validate.
     * @throws org.apache.osgi.moduleloader.search.ValidationException if
     *         the module or any dependent modules could not be validated.
    **/
    public void validate(Module module)
        throws ValidationException
    {
        if (getValidAttribute(module).booleanValue())
        {
            return;
        }

        // Flag to indicate whether the bundle is valid or not.
        boolean isValid = true;

        // This list will be used to remember which bundles
        // were validated so that the validation events can
        // be fired outside of the synchronized block.
        List fireValidatedList = null;

        // Will hold the exception to be thrown or rethrown.
        ValidationException invalidException = null;

        // Synchronize on the module manager, because we don't want
        // anything to change while we are in the middle of this
        // operation.
        synchronized (m_mgr)
        {
            // If we are already validating this module, then
            // just return; this is necessary for cycles.
            if (m_validateMap.get(module) != null)
            {
                return;
            }

            // Add the module to the validation map; this
            // is necessary for cycles.
            m_validateMap.put(module, module);

            // Keep track of the root module that started
            // the validation request; this is necessary
            // for cycles.
            if (m_rootModule == null)
            {
                m_rootModule = module;
            }

            // Now perform the validation algorithm.
            Map propagateMap = new HashMap();
  
            // Validation binds the module's imports to a specific exporting
            // module. A module also implicitly imports whatever it exports,
            // so exports are validated in the same fashion as imports. It
            // is possible, given the selection policy that a given export
            // may actually be satisfied by a different module (i.e., a
            // module is not guaranteed to be bound to what it exports). Since
            // the imports and exports meta-data are validated in the same
            // fashion, we will use the follow attribute array to loop and
            // validate both imports and exports using the same code.
            for (int attrIdx = 0; (isValid) && (attrIdx < m_searchAttrs.length); attrIdx++)
            {
                // Get the imports (exports are treated as imports to)
                // for the current module.
                Object[][] imports = getImportsOrExports(module, m_searchAttrs[attrIdx]);
                // See if each import has available exporters.
                for (int impIdx = 0; impIdx < imports.length; impIdx++)
                {
                    // Get all exporter candidates.
                    Module[] candidates =
                        getCompatibleModules(
                            imports[impIdx][IDENTIFIER_IDX], imports[impIdx][VERSION_IDX]);
                    // If there are no candidates, then prepare a
                    // validation exception.
                    if (candidates == null)
                    {
                        isValid = false;
                        invalidException =
                            new ValidationException(
                                "Unable to validate module",
                                module,
                                imports[impIdx][IDENTIFIER_IDX],
                                imports[impIdx][VERSION_IDX],
                                false);
                        break;
                    }

                    // Use selection policy to choose a single export candidate.
                    Module exportingModule = m_selectPolicy.select(
                        module, imports[impIdx][IDENTIFIER_IDX],
                        imports[impIdx][VERSION_IDX], candidates, m_compatPolicy);
                    // If there is no export module chosen, then prepare
                    // a validation exception.
                    if (exportingModule == null)
                    {
                        isValid = false;
                        invalidException =
                            new ValidationException(
                                "Unable to validate module",
                                module,
                                imports[impIdx][IDENTIFIER_IDX],
                                imports[impIdx][VERSION_IDX],
                                false);
                        break;
                    }

                    // Make sure that the export module is
                    // also validated.
                    try
                    {
                        validate(exportingModule);
                    }
                    catch (ValidationException ex)
                    {
                        // Prepare to rethrow the exception if
                        // the exporter could not be validated.
                        isValid = false;
                        invalidException = ex;
                        break;
                    }

                    // Keep track of all propagations from each module that this
                    // module imports from. Verify that any given import always
                    // comes form the same export module, otherwise there will be
                    // class cast exceptions.
                    Object[] propagates = getPropagatesAttribute(exportingModule);
                    for (int propIdx = 0; propIdx < propagates.length; propIdx++)
                    {
                        // If the module does not import the propagated target,
                        // then it can be safely ignored.
                        if (doesImport(module, propagates[propIdx]))
                        {
                            Module sourceModule =
                                (Module) propagateMap.get(propagates[propIdx]);

                            // If the propagation source module has not already been
                            // found, then remember the resolving module of the
                            // exporting module as the source of the propagated
                            // target.
                            if (sourceModule == null)
                            {
                                propagateMap.put(
                                    propagates[propIdx],
                                    getImportResolvingModule(
                                        exportingModule, propagates[propIdx]));
                            }
                            // If the propagation source module is found, then check to
                            // see if it is propagating the import target from the same
                            // module as previously determined for this module. If not,
                            // then this is a propagation conflict.
                            else if (sourceModule !=
                                getImportResolvingModule(
                                    exportingModule, propagates[propIdx]))
                            {
                                isValid = false;
                                invalidException =
                                    new ValidationException(
                                        "Unable to validate module",
                                        exportingModule,
                                        propagates[propIdx],
                                        null,
                                        true);
                                break;
                            }
                        }
                    }

                    // Set the chosen exporting module for the module
                    // being validated.
                    imports[impIdx][RESOLVING_MODULE_IDX] = exportingModule;
                }
            }

            // Since this method is recursive, check to see it we are
            // back at the root module that started the request, which
            // would indicate that the request is finished.
            if (m_rootModule == module)
            {
                // If the result is valid, then we have validated successfully.
                if (isValid)
                {
                    // Loop through all modules in validate map
                    // and mark them as valid.
                    Iterator iter = m_validateMap.keySet().iterator();
                    while (iter.hasNext())
                    {
                        Module m = (Module) iter.next();
                        if (!getValidAttribute(m).booleanValue())
                        {
                            m.setAttribute(VALID_ATTR, Boolean.TRUE);
                            if (fireValidatedList == null)
                            {
                                fireValidatedList = new ArrayList();
                            }
                            fireValidatedList.add(m);
                        }
                    }
                }
                // If we are here, then the validate failed, so we
                // need to reset any partially validated modules.
                else
                {
                    Iterator iter = m_validateMap.keySet().iterator();
                    while (iter.hasNext())
                    {
                        Module m = (Module) iter.next();
                        invalidate(
                            m,
                            m.getAttributes(),
                            m.getResourceSources(),
                            m.getLibrarySources());
                    }
                }

                // Clear the root module and validation map
                // before leaving the synchronized block.
                m_rootModule = null;
                m_validateMap.clear();
            }
        }

        // (Re)throw the exception if invalid, otherwise
        // fire validation events if the validated event
        // list is not null.
        if (!isValid)
        {
            throw invalidException;
        }
        else if (fireValidatedList != null)
        {
            for (int i = 0; i < fireValidatedList.size(); i++)
            {
                fireModuleValidated((Module) fireValidatedList.get(i));
            }
        }
    }

    /**
     * This method returns a list of modules that have an export
     * that is compatible with the given import identifier and version.
     * @param identifier the import identifier.
     * @param version the version of the import identifier.
     * @return an array of modules that have compatible exports or <tt>null</tt>
     *         if none are found.
    **/
    protected Module[] getCompatibleModules(Object identifier, Object version)
    {
        List list = null;
        Module[] modules = m_mgr.getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            Object[][] exports = getExportsAttribute(modules[modIdx]);
            for (int expIdx = 0; expIdx < exports.length; expIdx++)
            {
                // If the identifiers are comparable and compatible,
                // then add the export identifier to the list.
                if (m_compatPolicy.isCompatible(
                        exports[expIdx][IDENTIFIER_IDX], exports[expIdx][VERSION_IDX],
                        identifier, version))
                {
                    if (list == null)
                    {
                        list = new ArrayList();
                    }
                    list.add(modules[modIdx]);
                }
            }
        }

        if (list == null)
        {
            return null;
        }

        Module[] result = new Module[list.size()];
        return (Module[]) list.toArray(result);
    }

    /**
     * Invalidates a module by flushing its class loader and
     * re-initializing its meta-data values.
     * @param module the module to be invalidated.
     * @param attributes the attributes associated with the module, since they
     *        might have changed.
     * @param resSources the resource sources associated wih the module, since they
     *        might have changed.
     * @param libSources the library sources associated wih the module, since they
     *        might have changed.
    **/
    public void invalidate(
        Module module, Object[][] attributes,
        ResourceSource[] resSources, LibrarySource[] libSources)
    {
        // Synchronize on the module manager, because we don't want
        // anything to change while we are in the middle of this
        // operation.
        synchronized (m_mgr)
        {
            m_mgr.resetModule(module, attributes, resSources, libSources);
        }

        // Fire invalidation event if necessary.
        fireModuleInvalidated(m_mgr.getModule(module.getId()));
    }

    //
    // Event handling methods for validation events.
    //

    /**
     * Adds a validation listener to this import search policy. Validation
     * listeners are notified when a module is validated and/or invalidated
     * by the search policy.
     * @param l the validation listener to add.
    **/
    public void addValidationListener(ValidationListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_noListeners object as a lock.
        synchronized (m_noListeners)
        {
            // If we have no listeners, then just add the new listener.
            if (m_listeners == m_noListeners)
            {
                m_listeners = new ValidationListener[] { l };
            }
            // Otherwise, we need to do some array copying.
            // Notice, the old array is always valid, so if
            // the dispatch thread is in the middle of a dispatch,
            // then it has a reference to the old listener array
            // and is not affected by the new value.
            else
            {
                ValidationListener[] newList = new ValidationListener[m_listeners.length + 1];
                System.arraycopy(m_listeners, 0, newList, 0, m_listeners.length);
                newList[m_listeners.length] = l;
                m_listeners = newList;
            }
        }
    }

    /**
     * Removes a validation listener to this import search policy.
     * @param l the validation listener to remove.
    **/
    public void removeValidationListener(ValidationListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_noListeners object as a lock.
        synchronized (m_noListeners)
        {
            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < m_listeners.length; i++)
            {
                if (m_listeners[i].equals(l))
                {
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if (m_listeners.length == 1)
                {
                    m_listeners = m_noListeners;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    ValidationListener[] newList = new ValidationListener[m_listeners.length - 1];
                    System.arraycopy(m_listeners, 0, newList, 0, idx);
                    if (idx < newList.length)
                    {
                        System.arraycopy(m_listeners, idx + 1, newList, idx,
                            newList.length - idx);
                    }
                    m_listeners = newList;
                }
            }
        }
    }

    /**
     * Fires a validation event for the specified module.
     * @param module the module that was validated.
    **/
    protected void fireModuleValidated(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ValidationListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(m_mgr, module);
            }
            listeners[i].moduleValidated(event);
        }
    }

    /**
     * Fires an invalidation event for the specified module.
     * @param module the module that was invalidated.
    **/
    protected void fireModuleInvalidated(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ValidationListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(m_mgr, module);
            }
            listeners[i].moduleInvalidated(event);
        }
    }

    //
    // ModuleListener methods.
    //

    /**
     * Callback method for <tt>ModuleListener</tt>; this should not
     * be called directly. This callback is used to initialize module
     * meta-data attributes; it adds the <tt>VALID_ATTR</tt> attribute
     * and initializes the resolving module entries in <tt>EXPORTS_ATTR</tt>
     * and <tt>IMPORTS_ATTR</tt> to <tt>null</tt>.
    **/
    public void moduleAdded(ModuleEvent event)
    {
        synchronized (event.getModule())
        {
            // Add valid attribute to all modules.
            event.getModule().setAttribute(VALID_ATTR, Boolean.FALSE);

            for (int attrIdx = 0; attrIdx < m_searchAttrs.length; attrIdx++)
            {
                Object[][] imports =
                    getImportsOrExports(event.getModule(), m_searchAttrs[attrIdx]);
                for (int i = 0; i < imports.length; i++)
                {
                    imports[i][RESOLVING_MODULE_IDX] = null;
                }
            }
        }
    }

    /**
     * Callback method for <tt>ModuleListener</tt>; this should not
     * be called directly. This callback is used to re-initialize module
     * meta-data attributes; it adds the <tt>VALID_ATTR</tt> attribute
     * and initializes the resolving module entries in <tt>EXPORTS_ATTR</tt>
     * and <tt>IMPORTS_ATTR</tt> to <tt>null</tt>. It then invalidates
     * all modules that import from the reset module.
    **/
    public void moduleReset(ModuleEvent event)
    {
        // This will reset module meta-data.
        moduleAdded(event);

// TODO: Synchronization?
        ModuleManager m_mgr = (ModuleManager) event.getSource();
        List list = createImporterList(m_mgr, event.getModule());
        for (int i = 0; (list != null) && (i < list.size()); i++)
        {
            Module module = (Module) list.get(i);
            invalidate(
                module, module.getAttributes(),
                module.getResourceSources(), module.getLibrarySources());
        }
    }

    /**
     * Callback method for <tt>ModuleListener</tt>; this should not
     * be called directly. Used to listen for module removal events
     * in order to invalidate all the modules that import form the
     * removed moduled.
    **/
    public void moduleRemoved(ModuleEvent event)
    {
// TODO: Synchronization?
        ModuleManager m_mgr = (ModuleManager) event.getSource();
        List list = createImporterList(m_mgr, event.getModule());
        for (int i = 0; (list != null) && (i < list.size()); i++)
        {
            Module module = (Module) list.get(i);
            invalidate(
                module, module.getAttributes(),
                module.getResourceSources(), module.getLibrarySources());
        }
    }

    //
    // Instance utility methods.
    //

    /**
     * This utility method returns the module that exports the
     * specified import identifier and version. This method uses the
     * <tt>validate()</tt> method to find the exporting module and,
     * as a result, relies on the compatibility and selection
     * policies associated with this <tt>ImportSearchPolicy</tt>
     * instance. If successful, the returned module is guaranteed
     * to be validated. This method only needs to be used for more
     * advanced purposes (i.e., check import availability dynamically,
     * etc.) and need not be used under normal circumstances.
     * @param identifier the identifier of the import to resolve.
     * @param version the version of the import to resolve.
     * @return the exporting module selected to resolve the specified
     *         import target.
    **/
    public Module resolveImportTarget(Object identifier, Object version)
    {
        // Create a fake module that imports the specified target
        // and then try to validate it so we can get the exporting
        // module that is used to satisfy the import.
        Object[] targetImport = { identifier, version, null };
        Object[][] attrs = new Object[][] {
            new Object[] { EXPORTS_ATTR, new Object[0][0] },
            new Object[] { IMPORTS_ATTR, new Object[][] { targetImport } },
            new Object[] { PROPAGATES_ATTR, new Object[0] },
            new Object[] { VALID_ATTR, Boolean.FALSE}
        };
        Module fake = new Module(m_mgr, "resolve import", attrs, null, null, false);
        try {
            validate(fake);
        } catch (ValidationException ex) {
            // Ignore this.
        }
        return (Module) targetImport[RESOLVING_MODULE_IDX];
    }

    //
    // Static utility methods.
    //

    private static final Object[][] m_emptyImports = new Object[0][0];
    private static final Object[] m_emptyProp = new Object[0];

    /**
     * Utility method that returns the <tt>VALID_ATTR</tt> attribute for
     * the specified module.
     * @param module the module whose <tt>VALID_ATTR</tt> attribute is to
     *        be retrieved.
     * @return an instance of <tt>Boolean</tt>.
    **/
    public static Boolean getValidAttribute(Module module)
    {
        Object value = module.getAttribute(VALID_ATTR);
        if (value != null)
        {
            return (Boolean) value;
        }
        return Boolean.FALSE;
    }

    /**
     * Utility method that returns the <tt>IMPORTS_ATTR</tt> attribute for
     * the specified module.
     * @param module the module whose <tt>IMPORTS_ATTR</tt> attribute is to
     *        be retrieved.
     * @return an <tt>Object[][]</tt> value or <tt>null</tt>.
    **/
    public static Object[][] getImportsAttribute(Module module)
    {
        Object value = module.getAttribute(IMPORTS_ATTR);
        if (value != null)
        {
            return (Object[][]) value;
        }
        return m_emptyImports;
    }

    /**
     * Utility method that returns the <tt>EXPORTS_ATTR</tt> attribute for
     * the specified module.
     * @param module the module whose <tt>EXPORTS_ATTR</tt> attribute is to
     *        be retrieved.
     * @return an <tt>Object[][]</tt> value or <tt>null</tt>.
    **/
    public static Object[][] getExportsAttribute(Module module)
    {
        Object value = module.getAttribute(EXPORTS_ATTR);
        if (value != null)
        {
            return (Object[][]) value;
        }
        return m_emptyImports;
    }

    /**
     * Utility method that returns the <tt>IMPORTS_ATTR</tt> or the
     * <tt>EXPORTS_ATTR</tt> attribute for the specified module.
     * @param module the module whose <tt>IMPORTS_ATTR</tt> or
     *        <tt>EXPORTS_ATTR</tt> attribute is to be retrieved.
     * @param name either <tt>IMPORTS_ATTR</tt> or <tt>EXPORTS_ATTR</tt>
     *        depending on which attribute should be retrieved.
     * @return an <tt>Object[][]</tt> value or <tt>null</tt>.
    **/
    public static Object[][] getImportsOrExports(Module module, String name)
    {
        Object value = module.getAttribute(name);
        if (value != null)
        {
            return (Object[][]) value;
        }
        return m_emptyImports;
    }

    /**
     * Utility method that returns the <tt>PROPAGATES_ATTR</tt> attribute for
     * the specified module.
     * @param module the module whose <tt>PROPAGATES_ATTR</tt> attribute is to
     *        be retrieved.
     * @return an <tt>Object[]</tt> value or <tt>null</tt>.
    **/
    public static Object[] getPropagatesAttribute(Module module)
    {
        Object value = module.getAttribute(PROPAGATES_ATTR);
        if (value != null)
        {
            return (Object[]) value;
        }
        return m_emptyProp;
    }

    /**
     * Utility method to determine if the specified module imports a given
     * import identifier, regardless of version. This method checks both
     * imports and exports, since a module is assumed to import what it exports.
     * @param module the module to check.
     * @param identifier the import identifier to check.
     * @return <tt>true</tt> if the module imports the specified
     *         import identifier or <tt>false</tt> if it does not.
    **/
    public static boolean doesImport(Module module, Object identifier)
    {
        Object[][] imports = getImportsAttribute(module);
        for (int i = 0; i < imports.length; i++)
        {
            if (imports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return true;
            }
        }
        imports = getExportsAttribute(module);
        for (int i = 0; i < imports.length; i++)
        {
            if (imports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method to create a list of modules that import from
     * the specified module.
     * @param mgr the module manager that contains the module.
     * @param module the module for which to create an importer list.
     * @return a list of modules that import from the specified module
     *         or <tt>null</tt>.
    **/
    public static List createImporterList(ModuleManager mgr, Module module)
    {
        List list = null;
        Module[] modules = mgr.getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            Object[][] imports = getImportsAttribute(modules[modIdx]);
            for (int impIdx = 0; impIdx < imports.length; impIdx++)
            {
                if (imports[impIdx][RESOLVING_MODULE_IDX] == module)
                {
                    if (list == null)
                    {
                        list = new ArrayList();
                    }
                    list.add(modules[modIdx]);
                    break;
                }
            }
        }

        return list;
    }

    /**
     * Utility method to get the import version number associated with a specific
     * import identifier of the specified module.
     * @param module the module to investigate.
     * @param identifier the import identifier for which the version should
     *        be retrieved.
     * @return the version number object or <tt>null</tt>.
    **/
    public static Object getImportVersion(Module module, Object identifier)
    {
        Object[][] imports = getImportsAttribute(module);
        for (int i = 0; i < imports.length; i++)
        {
            if (imports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return imports[i][VERSION_IDX];
            }
        }
        return null;
    }

    /**
     * Utility method to get the export version number associated with a specific
     * export identifier of the specified module.
     * @param module the module to investigate.
     * @param identifier the export identifier for which the version should
     *        be retrieved.
     * @return the version number object or <tt>null</tt>.
    **/
    public static Object getExportVersion(Module module, Object identifier)
    {
        Object[][] exports = getExportsAttribute(module);
        for (int i = 0; i < exports.length; i++)
        {
            if (exports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return exports[i][VERSION_IDX];
            }
        }
        return null;
    }

    /**
     * Utility method to get the resolving module of a specific import
     * identifier for the specified module.
     * @param module the module to investigate.
     * @param identifier the import identifier for which the resolving
     *        module should be retrieved.
     * @return the resolving module or <tt>null</tt>.
    **/
    public static Module getImportResolvingModule(Module module, Object identifier)
    {
        Object[][] imports = getImportsAttribute(module);

        for (int i = 0; i < imports.length; i++)
        {
            if (imports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return (Module) imports[i][RESOLVING_MODULE_IDX];
            }
        }

        return null;
    }

    /**
     * Utility method to get the resolving module of a specific export
     * identifier for the specified module.
     * @param module the module to investigate.
     * @param identifier the export identifier for which the resolving
     *        module should be retrieved.
     * @return the resolving module or <tt>null</tt>.
    **/
    public static Module getExportResolvingModule(Module module, Object identifier)
    {
        Object[][] exports = getExportsAttribute(module);

        for (int i = 0; i < exports.length; i++)
        {
            if (exports[i][IDENTIFIER_IDX].equals(identifier))
            {
                return (Module) exports[i][RESOLVING_MODULE_IDX];
            }
        }

        return null;
    }
}