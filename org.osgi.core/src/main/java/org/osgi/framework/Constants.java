/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/Constants.java,v 1.32 2007/02/20 00:07:22 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2000, 2007). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework;

/**
 * Defines standard names for the OSGi environment system properties, service
 * properties, and Manifest header attribute keys.
 * 
 * <p>
 * The values associated with these keys are of type
 * <code>java.lang.String</code>, unless otherwise indicated.
 * 
 * @since 1.1
 * @version $Revision: 1.32 $
 */

public interface Constants {
	/**
	 * Location identifier of the OSGi <i>system bundle </i>, which is defined
	 * to be &quot;System Bundle&quot;.
	 */
	public static final String	SYSTEM_BUNDLE_LOCATION					= "System Bundle";

	/**
	 * Alias for the symbolic name of the OSGi <i>system bundle </i>. It is
	 * defined to be &quot;system.bundle&quot;.
	 * 
	 * @since 1.3
	 */
	public static final String	SYSTEM_BUNDLE_SYMBOLICNAME				= "system.bundle";

	/**
	 * Manifest header (named &quot;Bundle-Category&quot;) identifying the
	 * bundle's category.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_CATEGORY							= "Bundle-Category";

	/**
	 * Manifest header (named &quot;Bundle-ClassPath&quot;) identifying a list
	 * of directories and embedded JAR files, which are bundle resources used to
	 * extend the bundle's classpath.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_CLASSPATH						= "Bundle-ClassPath";

	/**
	 * Manifest header (named &quot;Bundle-Copyright&quot;) identifying the
	 * bundle's copyright information.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_COPYRIGHT						= "Bundle-Copyright";

	/**
	 * Manifest header (named &quot;Bundle-Description&quot;) containing a brief
	 * description of the bundle's functionality.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_DESCRIPTION						= "Bundle-Description";

	/**
	 * Manifest header (named &quot;Bundle-Name&quot;) identifying the bundle's
	 * name.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_NAME								= "Bundle-Name";

	/**
	 * Manifest header (named &quot;Bundle-NativeCode&quot;) identifying a
	 * number of hardware environments and the native language code libraries
	 * that the bundle is carrying for each of these environments.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_NATIVECODE						= "Bundle-NativeCode";

	/**
	 * Manifest header (named &quot;Export-Package&quot;) identifying the
	 * packages that the bundle offers to the Framework for export.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	EXPORT_PACKAGE							= "Export-Package";

	/**
	 * Manifest header (named &quot;Export-Service&quot;) identifying the fully
	 * qualified class names of the services that the bundle may register (used
	 * for informational purposes only).
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @deprecated As of 1.2.
	 */
	public static final String	EXPORT_SERVICE							= "Export-Service";

	/**
	 * Manifest header (named &quot;Import-Package&quot;) identifying the
	 * packages on which the bundle depends.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	IMPORT_PACKAGE							= "Import-Package";

	/**
	 * Manifest header (named &quot;DynamicImport-Package&quot;) identifying the
	 * packages that the bundle may dynamically import during execution.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.2
	 */
	public static final String	DYNAMICIMPORT_PACKAGE					= "DynamicImport-Package";

	/**
	 * Manifest header (named &quot;Import-Service&quot;) identifying the fully
	 * qualified class names of the services that the bundle requires (used for
	 * informational purposes only).
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @deprecated As of 1.2.
	 */
	public static final String	IMPORT_SERVICE							= "Import-Service";

	/**
	 * Manifest header (named &quot;Bundle-Vendor&quot;) identifying the
	 * bundle's vendor.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_VENDOR							= "Bundle-Vendor";

	/**
	 * Manifest header (named &quot;Bundle-Version&quot;) identifying the
	 * bundle's version.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_VERSION							= "Bundle-Version";

	/**
	 * Manifest header (named &quot;Bundle-DocURL&quot;) identifying the
	 * bundle's documentation URL, from which further information about the
	 * bundle may be obtained.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_DOCURL							= "Bundle-DocURL";

	/**
	 * Manifest header (named &quot;Bundle-ContactAddress&quot;) identifying the
	 * contact address where problems with the bundle may be reported; for
	 * example, an email address.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_CONTACTADDRESS					= "Bundle-ContactAddress";

	/**
	 * Manifest header attribute (named &quot;Bundle-Activator&quot;)
	 * identifying the bundle's activator class.
	 * 
	 * <p>
	 * If present, this header specifies the name of the bundle resource class
	 * that implements the <code>BundleActivator</code> interface and whose
	 * <code>start</code> and <code>stop</code> methods are called by the
	 * Framework when the bundle is started and stopped, respectively.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_ACTIVATOR						= "Bundle-Activator";

	/**
	 * Manifest header (named &quot;Bundle-UpdateLocation&quot;) identifying the
	 * location from which a new bundle version is obtained during a bundle
	 * update operation.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	BUNDLE_UPDATELOCATION					= "Bundle-UpdateLocation";

	/**
	 * Manifest header attribute (named &quot;specification-version&quot;)
	 * identifying the version of a package specified in the Export-Package or
	 * Import-Package manifest header.
	 * 
	 * @deprecated As of 1.3. This has been replaced by
	 *             {@link #VERSION_ATTRIBUTE}.
	 */
	public static final String	PACKAGE_SPECIFICATION_VERSION			= "specification-version";

	/**
	 * Manifest header attribute (named &quot;processor&quot;) identifying the
	 * processor required to run native bundle code specified in the
	 * Bundle-NativeCode manifest header).
	 * 
	 * <p>
	 * The attribute value is encoded in the Bundle-NativeCode manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-NativeCode: http.so ; processor=x86 ...
	 * </pre>
	 */
	public static final String	BUNDLE_NATIVECODE_PROCESSOR				= "processor";

	/**
	 * Manifest header attribute (named &quot;osname&quot;) identifying the
	 * operating system required to run native bundle code specified in the
	 * Bundle-NativeCode manifest header).
	 * <p>
	 * The attribute value is encoded in the Bundle-NativeCode manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-NativeCode: http.so ; osname=Linux ...
	 * </pre>
	 */
	public static final String	BUNDLE_NATIVECODE_OSNAME				= "osname";

	/**
	 * Manifest header attribute (named &quot;osversion&quot;) identifying the
	 * operating system version required to run native bundle code specified in
	 * the Bundle-NativeCode manifest header).
	 * <p>
	 * The attribute value is encoded in the Bundle-NativeCode manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-NativeCode: http.so ; osversion=&quot;2.34&quot; ...
	 * </pre>
	 */
	public static final String	BUNDLE_NATIVECODE_OSVERSION				= "osversion";

	/**
	 * Manifest header attribute (named &quot;language&quot;) identifying the
	 * language in which the native bundle code is written specified in the
	 * Bundle-NativeCode manifest header. See ISO 639 for possible values.
	 * <p>
	 * The attribute value is encoded in the Bundle-NativeCode manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-NativeCode: http.so ; language=nl_be ...
	 * </pre>
	 */
	public static final String	BUNDLE_NATIVECODE_LANGUAGE				= "language";

	/**
	 * Manifest header (named &quot;Bundle-RequiredExecutionEnvironment&quot;)
	 * identifying the required execution environment for the bundle. The
	 * service platform may run this bundle if any of the execution environments
	 * named in this header matches one of the execution environments it
	 * implements.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.2
	 */
	public static final String	BUNDLE_REQUIREDEXECUTIONENVIRONMENT		= "Bundle-RequiredExecutionEnvironment";

	/*
	 * Framework environment properties.
	 */

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.version&quot;) identifying the Framework
	 * version.
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_VERSION						= "org.osgi.framework.version";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.vendor&quot;) identifying the Framework
	 * implementation vendor.
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_VENDOR						= "org.osgi.framework.vendor";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.language&quot;) identifying the Framework
	 * implementation language (see ISO 639 for possible values).
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_LANGUAGE						= "org.osgi.framework.language";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.os.name&quot;) identifying the Framework
	 * host-computer's operating system.
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_OS_NAME						= "org.osgi.framework.os.name";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.os.version&quot;) identifying the Framework
	 * host-computer's operating system version number.
	 * 
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_OS_VERSION					= "org.osgi.framework.os.version";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.processor&quot;) identifying the Framework
	 * host-computer's processor name.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 */
	public static final String	FRAMEWORK_PROCESSOR						= "org.osgi.framework.processor";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.executionenvironment&quot;) identifying
	 * execution environments provided by the Framework.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.2
	 */
	public static final String	FRAMEWORK_EXECUTIONENVIRONMENT			= "org.osgi.framework.executionenvironment";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.bootdelegation&quot;) identifying packages for
	 * which the Framework must delegate class loading to the boot class path.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	FRAMEWORK_BOOTDELEGATION				= "org.osgi.framework.bootdelegation";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.framework.system.packages&quot;) identifying package which
	 * the system bundle must export.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	FRAMEWORK_SYSTEMPACKAGES				= "org.osgi.framework.system.packages";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.supports.framework.extension&quot;) identifying whether
	 * the Framework supports framework extension bundles. As of version 1.4,
	 * the value of this property must be <code>true</code>. The Framework
	 * must support framework extension bundles.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	SUPPORTS_FRAMEWORK_EXTENSION			= "org.osgi.supports.framework.extension";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.supports.bootclasspath.extension&quot;) identifying
	 * whether the Framework supports bootclasspath extension bundles. If the
	 * value of this property is <code>true</code>, then the Framework
	 * supports bootclasspath extension bundles. The default value is
	 * <code>false</code>.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	SUPPORTS_BOOTCLASSPATH_EXTENSION		= "org.osgi.supports.bootclasspath.extension";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.supports.framework.fragment&quot;) identifying whether the
	 * Framework supports fragment bundles. As of version 1.4, the value of this
	 * property must be <code>true</code>. The Framework must support
	 * fragment bundles.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	SUPPORTS_FRAMEWORK_FRAGMENT				= "org.osgi.supports.framework.fragment";

	/**
	 * Framework environment property (named
	 * &quot;org.osgi.supports.framework.requirebundle&quot;) identifying
	 * whether the Framework supports the <code>Require-Bundle</code> manifest
	 * header. As of version 1.4, the value of this property must be
	 * <code>true</code>. The Framework must support the
	 * <code>Require-Bundle</code> manifest header.
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * <code>BundleContext.getProperty</code> method.
	 * 
	 * @since 1.3
	 */
	public static final String	SUPPORTS_FRAMEWORK_REQUIREBUNDLE		= "org.osgi.supports.framework.requirebundle";

	/*
	 * Service properties.
	 */

	/**
	 * Service property (named &quot;objectClass&quot;) identifying all of the
	 * class names under which a service was registered in the Framework (of
	 * type <code>java.lang.String[]</code>).
	 * 
	 * <p>
	 * This property is set by the Framework when a service is registered.
	 */
	public static final String	OBJECTCLASS								= "objectClass";

	/**
	 * Service property (named &quot;service.id&quot;) identifying a service's
	 * registration number (of type <code>java.lang.Long</code>).
	 * 
	 * <p>
	 * The value of this property is assigned by the Framework when a service is
	 * registered. The Framework assigns a unique value that is larger than all
	 * previously assigned values since the Framework was started. These values
	 * are NOT persistent across restarts of the Framework.
	 */
	public static final String	SERVICE_ID								= "service.id";

	/**
	 * Service property (named &quot;service.pid&quot;) identifying a service's
	 * persistent identifier.
	 * 
	 * <p>
	 * This property may be supplied in the <code>properties</code>
	 * <code>Dictionary</code>
	 * object passed to the <code>BundleContext.registerService</code> method.
	 * 
	 * <p>
	 * A service's persistent identifier uniquely identifies the service and
	 * persists across multiple Framework invocations.
	 * 
	 * <p>
	 * By convention, every bundle has its own unique namespace, starting with
	 * the bundle's identifier (see {@link Bundle#getBundleId}) and followed by
	 * a dot (.). A bundle may use this as the prefix of the persistent
	 * identifiers for the services it registers.
	 */
	public static final String	SERVICE_PID								= "service.pid";

	/**
	 * Service property (named &quot;service.ranking&quot;) identifying a
	 * service's ranking number (of type <code>java.lang.Integer</code>).
	 * 
	 * <p>
	 * This property may be supplied in the <code>properties
	 * Dictionary</code>
	 * object passed to the <code>BundleContext.registerService</code> method.
	 * 
	 * <p>
	 * The service ranking is used by the Framework to determine the <i>default
	 * </i> service to be returned from a call to the
	 * {@link BundleContext#getServiceReference} method: If more than one
	 * service implements the specified class, the <code>ServiceReference</code>
	 * object with the highest ranking is returned.
	 * 
	 * <p>
	 * The default ranking is zero (0). A service with a ranking of
	 * <code>Integer.MAX_VALUE</code> is very likely to be returned as the
	 * default service, whereas a service with a ranking of
	 * <code>Integer.MIN_VALUE</code> is very unlikely to be returned.
	 * 
	 * <p>
	 * If the supplied property value is not of type
	 * <code>java.lang.Integer</code>, it is deemed to have a ranking value
	 * of zero.
	 */
	public static final String	SERVICE_RANKING							= "service.ranking";

	/**
	 * Service property (named &quot;service.vendor&quot;) identifying a
	 * service's vendor.
	 * 
	 * <p>
	 * This property may be supplied in the properties <code>Dictionary</code>
	 * object passed to the <code>BundleContext.registerService</code> method.
	 */
	public static final String	SERVICE_VENDOR							= "service.vendor";

	/**
	 * Service property (named &quot;service.description&quot;) identifying a
	 * service's description.
	 * 
	 * <p>
	 * This property may be supplied in the properties <code>Dictionary</code>
	 * object passed to the <code>BundleContext.registerService</code> method.
	 */
	public static final String	SERVICE_DESCRIPTION						= "service.description";

	/**
	 * Manifest header (named &quot;Bundle-SymbolicName&quot;) identifying the
	 * bundle's symbolic name.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.3
	 */
	public final static String	BUNDLE_SYMBOLICNAME						= "Bundle-SymbolicName";

	/**
	 * Manifest header directive (named &quot;singleton&quot;) identifying
	 * whether a bundle is a singleton. The default value is <code>false</code>.
	 * 
	 * <p>
	 * The directive value is encoded in the Bundle-SymbolicName manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-SymbolicName: com.acme.module.test; singleton:=true
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	SINGLETON_DIRECTIVE						= "singleton";

	/**
	 * Manifest header directive (named &quot;fragment-attachment&quot;)
	 * identifying if and when a fragment may attach to a host bundle. The
	 * default value is <code>&quot;always&quot;</code>.
	 * 
	 * <p>
	 * The directive value is encoded in the Bundle-SymbolicName manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-SymbolicName: com.acme.module.test; fragment-attachment:=&quot;never&quot;
	 * </pre>
	 * 
	 * @see Constants#FRAGMENT_ATTACHMENT_ALWAYS
	 * @see Constants#FRAGMENT_ATTACHMENT_RESOLVETIME
	 * @see Constants#FRAGMENT_ATTACHMENT_NEVER
	 * @since 1.3
	 */
	public final static String	FRAGMENT_ATTACHMENT_DIRECTIVE			= "fragment-attachment";

	/**
	 * Manifest header directive value (named &quot;always&quot;) identifying a
	 * fragment attachment type of always. A fragment attachment type of always
	 * indicates that fragments are allowed to attach to the host bundle at any
	 * time (while the host is resolved or during the process of resolving the
	 * host bundle).
	 * 
	 * <p>
	 * The directive value is encoded in the Bundle-SymbolicName manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-SymbolicName: com.acme.module.test; fragment-attachment:=&quot;always&quot;
	 * </pre>
	 * 
	 * @see Constants#FRAGMENT_ATTACHMENT_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	FRAGMENT_ATTACHMENT_ALWAYS				= "always";

	/**
	 * Manifest header directive value (named &quot;resolve-time&quot;)
	 * identifying a fragment attachment type of resolve-time. A fragment
	 * attachment type of resolve-time indicates that fragments are allowed to
	 * attach to the host bundle only during the process of resolving the host
	 * bundle.
	 * 
	 * <p>
	 * The directive value is encoded in the Bundle-SymbolicName manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-SymbolicName: com.acme.module.test; fragment-attachment:=&quot;resolve-time&quot;
	 * </pre>
	 * 
	 * @see Constants#FRAGMENT_ATTACHMENT_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	FRAGMENT_ATTACHMENT_RESOLVETIME			= "resolve-time";

	/**
	 * Manifest header directive value (named &quot;never&quot;) identifying a
	 * fragment attachment type of never. A fragment attachment type of never
	 * indicates that no fragments are allowed to attach to the host bundle at
	 * any time.
	 * 
	 * <p>
	 * The directive value is encoded in the Bundle-SymbolicName manifest header
	 * like:
	 * 
	 * <pre>
	 *     Bundle-SymbolicName: com.acme.module.test; fragment-attachment:=&quot;never&quot;
	 * </pre>
	 * 
	 * @see Constants#FRAGMENT_ATTACHMENT_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	FRAGMENT_ATTACHMENT_NEVER				= "never";

	/**
	 * Manifest header (named &quot;Bundle-Localization&quot;) identifying the
	 * base name of the bundle's localization entries.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @see #BUNDLE_LOCALIZATION_DEFAULT_BASENAME
	 * @since 1.3
	 */
	public final static String	BUNDLE_LOCALIZATION						= "Bundle-Localization";

	/**
	 * Default value for the <code>Bundle-Localization</code> manifest header.
	 * 
	 * @see #BUNDLE_LOCALIZATION
	 * @since 1.3
	 */
	public final static String	BUNDLE_LOCALIZATION_DEFAULT_BASENAME	= "OSGI-INF/l10n/bundle";

	/**
	 * Manifest header (named &quot;Require-Bundle&quot;) identifying the
	 * symbolic names of other bundles required by the bundle.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.3
	 */
	public final static String	REQUIRE_BUNDLE							= "Require-Bundle";

	/**
	 * Manifest header attribute (named &quot;bundle-version&quot;) identifying
	 * a range of versions for a bundle specified in the Require-Bundle or
	 * Fragment-Host manifest headers. The default value is <code>0.0.0</code>.
	 * 
	 * <p>
	 * The attribute value is encoded in the Require-Bundle manifest header
	 * like:
	 * 
	 * <pre>
	 *     Require-Bundle: com.acme.module.test; bundle-version=&quot;1.1&quot;
	 *     Require-Bundle: com.acme.module.test; bundle-version=&quot;[1.0,2.0)&quot;
	 * </pre>
	 * 
	 * <p>
	 * The bundle-version attribute value uses a mathematical interval notation
	 * to specify a range of bundle versions. A bundle-version attribute value
	 * specified as a single version means a version range that includes any
	 * bundle version greater than or equal to the specified version.
	 * 
	 * @since 1.3
	 */
	public static final String	BUNDLE_VERSION_ATTRIBUTE				= "bundle-version";

	/**
	 * Manifest header (named &quot;Fragment-Host&quot;) identifying the
	 * symbolic name of another bundle for which that the bundle is a fragment.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.3
	 */
	public final static String	FRAGMENT_HOST							= "Fragment-Host";

	/**
	 * Manifest header attribute (named &quot;selection-filter&quot;) is used
	 * for selection by filtering based upon system properties.
	 * 
	 * <p>
	 * The attribute value is encoded in manifest headers like:
	 * 
	 * <pre>
	 *     Bundle-NativeCode: libgtk.so; selection-filter=&quot;(ws=gtk)&quot;; ...
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	SELECTION_FILTER_ATTRIBUTE				= "selection-filter";

	/**
	 * Manifest header (named &quot;Bundle-ManifestVersion&quot;) identifying
	 * the bundle manifest version. A bundle manifest may express the version of
	 * the syntax in which it is written by specifying a bundle manifest
	 * version. Bundles exploiting OSGi R4, or later, syntax must specify a
	 * bundle manifest version.
	 * <p>
	 * The bundle manifest version defined by OSGi R4 or, more specifically, by
	 * V1.3 of the OSGi Framework Specification is "2".
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.3
	 */
	public final static String	BUNDLE_MANIFESTVERSION					= "Bundle-ManifestVersion";

	/**
	 * Manifest header attribute (named &quot;version&quot;) identifying the
	 * version of a package specified in the Export-Package or Import-Package
	 * manifest header.
	 * 
	 * <p>
	 * The attribute value is encoded in the Export-Package or Import-Package
	 * manifest header like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; version=&quot;1.1&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	VERSION_ATTRIBUTE						= "version";

	/**
	 * Manifest header attribute (named &quot;bundle-symbolic-name&quot;)
	 * identifying the symbolic name of a bundle that exports a package
	 * specified in the Import-Package manifest header.
	 * 
	 * <p>
	 * The attribute value is encoded in the Import-Package manifest header
	 * like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; bundle-symbolic-name=&quot;com.acme.module.test&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	BUNDLE_SYMBOLICNAME_ATTRIBUTE			= "bundle-symbolic-name";

	/**
	 * Manifest header directive (named &quot;resolution&quot;) identifying the
	 * resolution type in the Import-Package or Require-Bundle manifest header.
	 * 
	 * <p>
	 * The directive value is encoded in the Import-Package or Require-Bundle
	 * manifest header like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; resolution:=&quot;optional&quot;
	 *     Require-Bundle: com.acme.module.test; resolution:=&quot;optional&quot;
	 * </pre>
	 * 
	 * @see Constants#RESOLUTION_MANDATORY
	 * @see Constants#RESOLUTION_OPTIONAL
	 * @since 1.3
	 */
	public final static String	RESOLUTION_DIRECTIVE					= "resolution";

	/**
	 * Manifest header directive value (named &quot;mandatory&quot;) identifying
	 * a mandatory resolution type. A mandatory resolution type indicates that
	 * the import package or require bundle must be resolved when the bundle is
	 * resolved. If such an import or require bundle cannot be resolved, the
	 * module fails to resolve.
	 * 
	 * <p>
	 * The directive value is encoded in the Import-Package or Require-Bundle
	 * manifest header like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; resolution:=&quot;manditory&quot;
	 *     Require-Bundle: com.acme.module.test; resolution:=&quot;manditory&quot;
	 * </pre>
	 * 
	 * @see Constants#RESOLUTION_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	RESOLUTION_MANDATORY					= "mandatory";

	/**
	 * Manifest header directive value (named &quot;optional&quot;) identifying
	 * an optional resolution type. An optional resolution type indicates that
	 * the import or require bundle is optional and the bundle may be resolved
	 * without the import or require bundle being resolved. If the import or
	 * require bundle is not resolved when the bundle is resolved, the import or
	 * require bundle may not be resolved before the bundle is refreshed.
	 * 
	 * <p>
	 * The directive value is encoded in the Import-Package or Require-Bundle
	 * manifest header like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; resolution:=&quot;optional&quot;
	 *     Require-Bundle: com.acme.module.test; resolution:=&quot;optional&quot;
	 * </pre>
	 * 
	 * @see Constants#RESOLUTION_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	RESOLUTION_OPTIONAL						= "optional";

	/**
	 * Manifest header directive (named &quot;uses&quot;) identifying a list of
	 * packages that an exported package uses.
	 * 
	 * <p>
	 * The directive value is encoded in the Export-Package manifest header
	 * like:
	 * 
	 * <pre>
	 *     Export-Package: org.osgi.util.tracker; uses:=&quot;org.osgi.framework&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	USES_DIRECTIVE							= "uses";

	/**
	 * Manifest header directive (named &quot;include&quot;).
	 * <p>
	 * This directive is used by the Import-Package manifest header to identify
	 * a list of classes of the specified package which must be allowed to be
	 * exported. The directive value is encoded in the Import-Package manifest
	 * header like:
	 * 
	 * <pre>
	 *     Import-Package: org.osgi.framework; include:=&quot;MyClass*&quot;
	 * </pre>
	 * 
	 * <p>
	 * This directive is also used by the Bundle-ActivationPolicy manifest
	 * header to identify the packages from which class loads will trigger lazy
	 * activation. The directive value is encoded in the Bundle-ActivationPolicy
	 * manifest header like:
	 * 
	 * <pre>
	 *     Bundle-ActivationPolicy: lazy; include:=&quot;org.osgi.framework&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	INCLUDE_DIRECTIVE						= "include";

	/**
	 * Manifest header directive (named &quot;exclude&quot;).
	 * <p>
	 * This directive is used by the Export-Package manifest header to identify
	 * a list of classes of the specified package which must not be allowed to
	 * be exported. The directive value is encoded in the Export-Package
	 * manifest header like:
	 * 
	 * <pre>
	 *     Export-Package: org.osgi.framework; exclude:=&quot;*Impl&quot;
	 * </pre>
	 * 
	 * <p>
	 * This directive is also used by the Bundle-ActivationPolicy manifest
	 * header to identify the packages from which class loads will not trigger
	 * lazy activation. The directive value is encoded in the
	 * Bundle-ActivationPolicy manifest header like:
	 * 
	 * <pre>
	 *     Bundle-ActivationPolicy: lazy; exclude:=&quot;org.osgi.framework&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	EXCLUDE_DIRECTIVE						= "exclude";

	/**
	 * Manifest header directive (named &quot;mandatory&quot;) identifying names
	 * of matching attributes which must be specified by matching Import-Package
	 * statements in the Export-Package manifest header.
	 * 
	 * <p>
	 * The directive value is encoded in the Export-Package manifest header
	 * like:
	 * 
	 * <pre>
	 *     Export-Package: org.osgi.framework; mandatory:=&quot;bundle-symbolic-name&quot;
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public final static String	MANDATORY_DIRECTIVE						= "mandatory";

	/**
	 * Manifest header directive (named &quot;visibility&quot;) identifying the
	 * visibility of a reqiured bundle in the Require-Bundle manifest header.
	 * 
	 * <p>
	 * The directive value is encoded in the Require-Bundle manifest header
	 * like:
	 * 
	 * <pre>
	 *     Require-Bundle: com.acme.module.test; visibility:=&quot;reexport&quot;
	 * </pre>
	 * 
	 * @see Constants#VISIBILITY_PRIVATE
	 * @see Constants#VISIBILITY_REEXPORT
	 * @since 1.3
	 */
	public final static String	VISIBILITY_DIRECTIVE					= "visibility";

	/**
	 * Manifest header directive value (named &quot;private&quot;) identifying a
	 * private visibility type. A private visibility type indicates that any
	 * packages that are exported by the required bundle are not made visible on
	 * the export signature of the requiring bundle.
	 * 
	 * <p>
	 * The directive value is encoded in the Require-Bundle manifest header
	 * like:
	 * 
	 * <pre>
	 *     Require-Bundle: com.acme.module.test; visibility:=&quot;private&quot;
	 * </pre>
	 * 
	 * @see Constants#VISIBILITY_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	VISIBILITY_PRIVATE						= "private";

	/**
	 * Manifest header directive value (named &quot;reexport&quot;) identifying
	 * a reexport visibility type. A reexport visibility type indicates any
	 * packages that are exported by the required bundle are re-exported by the
	 * requiring bundle. Any arbitrary arbitrary matching attributes with which
	 * they were exported by the required bundle are deleted.
	 * 
	 * <p>
	 * The directive value is encoded in the Require-Bundle manifest header
	 * like:
	 * 
	 * <pre>
	 *     Require-Bundle: com.acme.module.test; visibility:=&quot;reexport&quot;
	 * </pre>
	 * 
	 * @see Constants#VISIBILITY_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	VISIBILITY_REEXPORT						= "reexport";
	/**
	 * Manifest header directive (named &quot;extension&quot;) identifying the
	 * type of the extension fragment.
	 * 
	 * <p>
	 * The directive value is encoded in the Fragment-Host manifest header like:
	 * 
	 * <pre>
	 *     Fragment-Host: system.bundle; extension:=&quot;framework&quot;
	 * </pre>
	 * 
	 * @see Constants#EXTENSION_FRAMEWORK
	 * @see Constants#EXTENSION_BOOTCLASSPATH
	 * @since 1.3
	 */
	public final static String	EXTENSION_DIRECTIVE						= "extension";

	/**
	 * Manifest header directive value (named &quot;framework&quot;) identifying
	 * the type of extension fragment. An extension fragment type of framework
	 * indicates that the extension fragment is to be loaded by the framework's
	 * class loader.
	 * 
	 * <p>
	 * The directive value is encoded in the Fragment-Host manifest header like:
	 * 
	 * <pre>
	 *     Fragment-Host: system.bundle; extension:=&quot;framework&quot;
	 * </pre>
	 * 
	 * @see Constants#EXTENSION_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	EXTENSION_FRAMEWORK						= "framework";

	/**
	 * Manifest header directive value (named &quot;bootclasspath&quot;)
	 * identifying the type of extension fragment. An extension fragment type of
	 * bootclasspath indicates that the extension fragment is to be loaded by
	 * the boot class loader.
	 * 
	 * <p>
	 * The directive value is encoded in the Fragment-Host manifest header like:
	 * 
	 * <pre>
	 *     Fragment-Host: system.bundle; extension:=&quot;bootclasspath&quot;
	 * </pre>
	 * 
	 * @see Constants#EXTENSION_DIRECTIVE
	 * @since 1.3
	 */
	public final static String	EXTENSION_BOOTCLASSPATH					= "bootclasspath";

	/**
	 * Manifest header (named &quot;Bundle-ActivationPolicy&quot;) identifying
	 * the bundle's activation policy.
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 * 
	 * @since 1.4
	 * @see Constants#ACTIVATION_LAZY
	 * @see Constants#INCLUDE_DIRECTIVE
	 * @see Constants#EXCLUDE_DIRECTIVE
	 */
	public final static String	BUNDLE_ACTIVATIONPOLICY					= "Bundle-ActivationPolicy";

	/**
	 * Bundle activation policy (named &quot;lazy&quot;) declaring the bundle
	 * must be activated when the first class load is made from the bundle.
	 * <p>
	 * A bundle with the lazy activation policy that is started with the
	 * {@link Bundle#START_ACTIVATION_POLICY START_ACTIVATION_POLICY} option
	 * will wait in the {@link Bundle#STARTING STARTING} state until the first
	 * class load from the bundle occurs. The bundle will then be activated
	 * before the class is returned to the requestor.
	 * <p>
	 * The activation policy value is specified as in the
	 * Bundle-ActivationPolicy manifest header like:
	 * 
	 * <pre>
	 *       Bundle-ActivationPolicy: lazy
	 * </pre>
	 * 
	 * @see Constants#BUNDLE_ACTIVATIONPOLICY
	 * @see Bundle#start(int)
	 * @see Bundle#START_ACTIVATION_POLICY
	 * @since 1.4
	 */
	public final static String	ACTIVATION_LAZY							= "lazy";

}