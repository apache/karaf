/*
 * Copyright (c) OSGi Alliance (2010, 2011). All Rights Reserved.
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

package org.osgi.framework.wiring;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Bundle Revision. When a bundle is installed and each time a bundle is
 * updated, a new bundle revision of the bundle is created. Since a bundle
 * update can change the entries in a bundle, different bundle wirings for the
 * same bundle can be associated with different bundle revisions.
 * 
 * <p>
 * For a bundle that has not been uninstalled, the most recent bundle revision
 * is defined to be the current bundle revision. A bundle in the UNINSTALLED
 * state does not have a current revision. The current bundle revision for a
 * bundle can be obtained by calling {@link Bundle#adapt(Class) bundle.adapt}
 * (BundleRevision.class). Since a bundle in the UNINSTALLED state does not have
 * a current revision, adapting such a bundle returns {@code null}.
 * 
 * <p>
 * The framework defines name spaces for {@link #PACKAGE_NAMESPACE package},
 * {@link #BUNDLE_NAMESPACE bundle} and {@link #HOST_NAMESPACE host}
 * capabilities and requirements. These name spaces are defined only to express
 * wiring information by the framework. They must not be used in
 * {@link Constants#PROVIDE_CAPABILITY Provide-Capability} and
 * {@link Constants#REQUIRE_CAPABILITY Require-Capability} manifest headers.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 139b3046ebd46c48b03dda8d36f2f9d79e2e616d $
 */
public interface BundleRevision extends BundleReference {
	/**
	 * Returns the symbolic name for this bundle revision.
	 * 
	 * @return The symbolic name for this bundle revision.
	 * @see Bundle#getSymbolicName()
	 */
	String getSymbolicName();

	/**
	 * Returns the version for this bundle revision.
	 * 
	 * @return The version for this bundle revision, or
	 *         {@link Version#emptyVersion} if this bundle revision has no
	 *         version information.
	 * @see Bundle#getVersion()
	 */
	Version getVersion();

	/**
	 * Returns the capabilities declared by this bundle revision.
	 * 
	 * @param namespace The name space of the declared capabilities to return or
	 *        {@code null} to return the declared capabilities from all name
	 *        spaces.
	 * @return A list containing a snapshot of the declared
	 *         {@link BundleCapability}s, or an empty list if this bundle
	 *         revision declares no capabilities in the specified name space.
	 *         The list contains the declared capabilities in the order they are
	 *         specified in the manifest.
	 */
	List<BundleCapability> getDeclaredCapabilities(String namespace);

	/**
	 * Returns the requirements declared by this bundle revision.
	 * 
	 * @param namespace The name space of the declared requirements to return or
	 *        {@code null} to return the declared requirements from all name
	 *        spaces.
	 * @return A list containing a snapshot of the declared
	 *         {@link BundleRequirement}s, or an empty list if this bundle
	 *         revision declares no requirements in the specified name space.
	 *         The list contains the declared requirements in the order they are
	 *         specified in the manifest.
	 */
	List<BundleRequirement> getDeclaredRequirements(String namespace);

	/**
	 * Name space for package capabilities and requirements.
	 * 
	 * <p>
	 * The name of the package is stored in the capability attribute of the same
	 * name as this name space (osgi.wiring.package). The other
	 * directives and attributes of the package, from the
	 * {@link Constants#EXPORT_PACKAGE Export-Package} manifest header, can be
	 * found in the cabability's {@link BundleCapability#getDirectives()
	 * directives} and {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#VERSION_ATTRIBUTE version} capability attribute must
	 * contain the {@link Version} of the package if one is specified or
	 * {@link Version#emptyVersion} if not specified. The
	 * {@link Constants#BUNDLE_SYMBOLICNAME_ATTRIBUTE bundle-symbolic-name}
	 * capability attribute must contain the
	 * {@link BundleRevision#getSymbolicName() symbolic name} of the provider if
	 * one is specified. The {@link Constants#BUNDLE_VERSION_ATTRIBUTE
	 * bundle-version} capability attribute must contain the
	 * {@link BundleRevision#getVersion() version} of the provider if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * The package capabilities provided by the system bundle, that is the
	 * bundle with id zero, must include the package specified by the
	 * {@link Constants#FRAMEWORK_SYSTEMPACKAGES} and
	 * {@link Constants#FRAMEWORK_SYSTEMPACKAGES_EXTRA} framework properties as
	 * well as any other package exported by the framework implementation.
	 * 
	 * <p>
	 * A bundle revision {@link BundleRevision#getDeclaredCapabilities(String)
	 * declares} zero or more package capabilities (this is, exported packages)
	 * and {@link BundleRevision#getDeclaredRequirements(String) declares} zero
	 * or more package requirements.
	 * <p>
	 * A bundle wiring {@link BundleWiring#getCapabilities(String) provides}
	 * zero or more resolved package capabilities (that is, exported packages)
	 * and {@link BundleWiring#getRequiredWires(String) requires} zero or more
	 * resolved package requirements (that is, imported packages). The number of
	 * package wires required by a bundle wiring may change as the bundle wiring
	 * may dynamically import additional packages.
	 */
	String	PACKAGE_NAMESPACE	= "osgi.wiring.package";

	/**
	 * Name space for bundle capabilities and requirements.
	 * 
	 * <p>
	 * The bundle symbolic name of the bundle is stored in the capability
	 * attribute of the same name as this name space (osgi.wiring.bundle).
	 * The other directives and attributes of the bundle, from the
	 * {@link Constants#BUNDLE_SYMBOLICNAME Bundle-SymbolicName} manifest
	 * header, can be found in the cabability's
	 * {@link BundleCapability#getDirectives() directives} and
	 * {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} capability
	 * attribute must contain the {@link Version} of the bundle from the
	 * {@link Constants#BUNDLE_VERSION Bundle-Version} manifest header if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * A non-fragment revision
	 * {@link BundleRevision#getDeclaredCapabilities(String) declares} exactly
	 * one<sup>&#8224;</sup> bundle capability (that is, the bundle can be
	 * required by another bundle). A fragment revision must not declare a
	 * bundle capability.
	 * 
	 * <p>
	 * A bundle wiring for a non-fragment revision
	 * {@link BundleWiring#getCapabilities(String) provides} exactly
	 * one<sup>&#8224;</sup> bundle capability (that is, the bundle can be
	 * required by another bundle) and
	 * {@link BundleWiring#getRequiredWires(String) requires} zero or more
	 * bundle capabilities (that is, requires other bundles).
	 * 
	 * <p>
	 * &#8224; A bundle with no bundle symbolic name (that is, a bundle with
	 * {@link Constants#BUNDLE_MANIFESTVERSION Bundle-ManifestVersion}
	 * {@literal <} 2) must not provide a bundle capability.
	 */
	String	BUNDLE_NAMESPACE	= "osgi.wiring.bundle";

	/**
	 * Name space for host capabilities and requirements.
	 * 
	 * <p>
	 * The bundle symbolic name of the bundle is stored in the capability
	 * attribute of the same name as this name space (osgi.wiring.host).
	 * The other directives and attributes of the bundle, from the
	 * {@link Constants#BUNDLE_SYMBOLICNAME Bundle-SymbolicName} manifest
	 * header, can be found in the cabability's
	 * {@link BundleCapability#getDirectives() directives} and
	 * {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} capability
	 * attribute must contain the {@link Version} of the bundle from the
	 * {@link Constants#BUNDLE_VERSION Bundle-Version} manifest header if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * A non-fragment revision
	 * {@link BundleRevision#getDeclaredCapabilities(String) declares} zero or
	 * one<sup>&#8224;</sup> host capability if the bundle
	 * {@link Constants#FRAGMENT_ATTACHMENT_DIRECTIVE allows fragments to be
	 * attached}. A fragment revision must
	 * {@link BundleRevision#getDeclaredRequirements(String) declare} exactly
	 * one host requirement.
	 * 
	 * <p>
	 * A bundle wiring for a non-fragment revision
	 * {@link BundleWiring#getCapabilities(String) provides} zero or
	 * one<sup>&#8224;</sup> host capability if the bundle
	 * {@link Constants#FRAGMENT_ATTACHMENT_DIRECTIVE allows fragments to be
	 * attached}. A bundle wiring for a fragment revision
	 * {@link BundleWiring#getRequiredWires(String) requires} a host capability
	 * for each host to which it is attached.
	 * 
	 * <p>
	 * &#8224; A bundle with no bundle symbolic name (that is, a bundle with
	 * {@link Constants#BUNDLE_MANIFESTVERSION Bundle-ManifestVersion}
	 * {@literal <} 2) must not provide a host capability.
	 */
	String	HOST_NAMESPACE		= "osgi.wiring.host";

	/**
	 * Returns the special types of this bundle revision. The bundle revision
	 * type values are:
	 * <ul>
	 * <li>{@link #TYPE_FRAGMENT}
	 * </ul>
	 * 
	 * A bundle revision may be more than one type at a time. A type code is
	 * used to identify the bundle revision type for future extendability.
	 * 
	 * <p>
	 * If this bundle revision is not one or more of the defined types then 0 is
	 * returned.
	 * 
	 * @return The special types of this bundle revision. The type values are
	 *         ORed together.
	 */
	int getTypes();

	/**
	 * Bundle revision type indicating the bundle revision is a fragment.
	 * 
	 * @see #getTypes()
	 */
	int	TYPE_FRAGMENT	= 0x00000001;

	/**
	 * Returns the bundle wiring which is using this bundle revision.
	 * 
	 * @return The bundle wiring which is using this bundle revision or
	 *         {@code null} if no bundle wiring is using this bundle revision.
	 * @see BundleWiring#getRevision()
	 */
	BundleWiring getWiring();
}
