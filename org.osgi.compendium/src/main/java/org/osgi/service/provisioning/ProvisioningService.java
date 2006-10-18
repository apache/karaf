/*
 * $Header: /cvshome/build/org.osgi.service.provisioning/src/org/osgi/service/provisioning/ProvisioningService.java,v 1.11 2006/07/12 21:21:31 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2002, 2006). All Rights Reserved.
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
package org.osgi.service.provisioning;

import java.io.IOException;
import java.util.Dictionary;
import java.util.zip.ZipInputStream;

/**
 * Service for managing the initial provisioning information.
 * <p>
 * Initial provisioning of an OSGi device is a multi step process that
 * culminates with the installation and execution of the initial management
 * agent. At each step of the process, information is collected for the next
 * step. Multiple bundles may be involved and this service provides a means for
 * these bundles to exchange information. It also provides a means for the
 * initial Management Bundle to get its initial configuration information.
 * <p>
 * The provisioning information is collected in a <code>Dictionary</code> object,
 * called the Provisioning Dictionary. Any bundle that can access the service
 * can get a reference to this object and read and update provisioning
 * information. The key of the dictionary is a <code>String</code> object and the
 * value is a <code>String</code> or <code>byte[]</code> object. The single
 * exception is the PROVISIONING_UPDATE_COUNT value which is an Integer. The
 * <code>provisioning</code> prefix is reserved for keys defined by OSGi, other
 * key names may be used for implementation dependent provisioning systems.
 * <p>
 * Any changes to the provisioning information will be reflected immediately in
 * all the dictionary objects obtained from the Provisioning Service.
 * <p>
 * Because of the specific application of the Provisioning Service, there should
 * be only one Provisioning Service registered. This restriction will not be
 * enforced by the Framework. Gateway operators or manufactures should ensure
 * that a Provisioning Service bundle is not installed on a device that already
 * has a bundle providing the Provisioning Service.
 * <p>
 * The provisioning information has the potential to contain sensitive
 * information. Also, the ability to modify provisioning information can have
 * drastic consequences. Thus, only trusted bundles should be allowed to
 * register and get the Provisioning Service. The <code>ServicePermission</code>
 * is used to limit the bundles that can gain access to the Provisioning
 * Service. There is no check of <code>Permission</code> objects to read or modify
 * the provisioning information, so care must be taken not to leak the
 * Provisioning Dictionary received from <code>getInformation</code> method.
 * 
 * @version $Revision: 1.11 $
 */
public interface ProvisioningService {
	/**
	 * The key to the provisioning information that uniquely identifies the
	 * Service Platform. The value must be of type <code>String</code>.
	 */
	public final static String	PROVISIONING_SPID			= "provisioning.spid";
	/**
	 * The key to the provisioning information that contains the location of the
	 * provision data provider. The value must be of type <code>String</code>.
	 */
	public final static String	PROVISIONING_REFERENCE		= "provisioning.reference";
	/**
	 * The key to the provisioning information that contains the initial
	 * configuration information of the initial Management Agent. The value will
	 * be of type <code>byte[]</code>.
	 */
	public final static String	PROVISIONING_AGENT_CONFIG	= "provisioning.agent.config";
	/**
	 * The key to the provisioning information that contains the update count of
	 * the info data. Each set of changes to the provisioning information must
	 * end with this value being incremented. The value must be of type
	 * <code>Integer</code>. This key/value pair is also reflected in the
	 * properties of the ProvisioningService in the service registry.
	 */
	public final static String	PROVISIONING_UPDATE_COUNT	= "provisioning.update.count";
	/**
	 * The key to the provisioning information that contains the location of the
	 * bundle to start with <code>AllPermission</code>. The bundle must have be
	 * previously installed for this entry to have any effect.
	 */
	public final static String	PROVISIONING_START_BUNDLE	= "provisioning.start.bundle";
	/**
	 * The key to the provisioning information that contains the root X509
	 * certificate used to esatblish trust with operator when using HTTPS.
	 */
	public final static String	PROVISIONING_ROOTX509		= "provisioning.rootx509";
	/**
	 * The key to the provisioning information that contains the shared secret
	 * used in conjunction with the RSH protocol.
	 */
	public final static String	PROVISIONING_RSH_SECRET		= "provisioning.rsh.secret";
	/**
	 * MIME type to be stored in the extra field of a <code>ZipEntry</code> object
	 * for String data.
	 */
	public final static String	MIME_STRING					= "text/plain;charset=utf-8";
	/**
	 * MIME type to be stored in the extra field of a <code>ZipEntry</code> object
	 * for <code>byte[]</code> data.
	 */
	public final static String	MIME_BYTE_ARRAY				= "application/octet-stream";
	/**
	 * MIME type to be stored in the extra field of a <code>ZipEntry</code> object
	 * for an installable bundle file. Zip entries of this type will be
	 * installed in the framework, but not started. The entry will also not be
	 * put into the information dictionary.
	 */
	public final static String	MIME_BUNDLE					= "application/x-osgi-bundle";
	/**
	 * MIME type to be stored in the extra field of a ZipEntry for a String that
	 * represents a URL for a bundle. Zip entries of this type will be used to
	 * install (but not start) a bundle from the URL. The entry will not be put
	 * into the information dictionary.
	 */
	public final static String	MIME_BUNDLE_URL				= "text/x-osgi-bundle-url";

	/**
	 * Returns a reference to the Provisioning Dictionary. Any change operations
	 * (put and remove) to the dictionary will cause an
	 * <code>UnsupportedOperationException</code> to be thrown. Changes must be
	 * done using the <code>setInformation</code> and <code>addInformation</code>
	 * methods of this service.
	 * @return A reference to the Provisioning Dictionary.
	 */
	public Dictionary getInformation();

	/**
	 * Replaces the Provisioning Information dictionary with the key/value pairs
	 * contained in <code>info</code>. Any key/value pairs not in <code>info</code>
	 * will be removed from the Provisioning Information dictionary. This method
	 * causes the <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
	 * 
	 * @param info the new set of Provisioning Information key/value pairs. Any
	 *        keys are values that are of an invalid type will be silently
	 *        ignored.
	 */
	public void setInformation(Dictionary info);

	/**
	 * Adds the key/value pairs contained in <code>info</code> to the Provisioning
	 * Information dictionary. This method causes the
	 * <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
	 * 
	 * @param info the set of Provisioning Information key/value pairs to add to
	 *        the Provisioning Information dictionary. Any keys are values that
	 *        are of an invalid type will be silently ignored.
	 */
	public void addInformation(Dictionary info);

	/**
	 * Processes the <code>ZipInputStream</code> and extracts information to add
	 * to the Provisioning Information dictionary, as well as, install/update
	 * and start bundles. This method causes the
	 * <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
	 * 
	 * @param zis the <code>ZipInputStream</code> that will be used to add
	 *        key/value pairs to the Provisioning Information dictionary and
	 *        install and start bundles. If a <code>ZipEntry</code> does not have
	 *        an <code>Extra</code> field that corresponds to one of the four
	 *        defined MIME types (<code>MIME_STRING</code>,
	 *        <code>MIME_BYTE_ARRAY</code>,<code>MIME_BUNDLE</code>, and
	 *        <code>MIME_BUNDLE_URL</code>) in will be silently ignored.
	 * @throws IOException if an error occurs while processing the
	 *            ZipInputStream. No additions will be made to the Provisioning
	 *            Information dictionary and no bundles must be started or
	 *            installed.
	 */
	public void addInformation(ZipInputStream zis) throws IOException;
}
