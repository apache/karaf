/*
 * Copyright (c) OSGi Alliance (2002, 2008). All Rights Reserved.
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
package org.osgi.service.upnp;

import java.util.Dictionary;

/**
 * Represents a UPnP device.
 * 
 * For each UPnP root and embedded device, an object is registered with the
 * framework under the <code>UPnPDevice</code> interface.
 * <p>
 * The relationship between a root device and its embedded devices can be
 * deduced using the <code>UPnPDevice.CHILDREN_UDN</code> and
 * <code>UPnPDevice.PARENT_UDN</code> service registration properties.
 * <p>
 * The values of the UPnP property names are defined by the UPnP Forum.
 * <p>
 * All values of the UPnP properties are obtained from the device using the
 * device's default locale.
 * <p>
 * If an application wants to query for a set of localized property values, it
 * has to use the method <code>UPnPDevice.getDescriptions(String locale)</code>.
 * 
 * @version $Revision: 5673 $
 */
public interface UPnPDevice {
	/*
	 * Constants for the UPnP device match scale.
	 */
	/**
	 * Constant for the UPnP device match scale, indicating a generic match for
	 * the device. Value is 1.
	 */
	int		MATCH_GENERIC								= 1;
	/**
	 * Constant for the UPnP device match scale, indicating a match with the
	 * device type. Value is 3.
	 */
	int		MATCH_TYPE									= 3;
	/**
	 * Constant for the UPnP device match scale, indicating a match with the
	 * device model. Value is 7.
	 */
	int		MATCH_MANUFACTURER_MODEL					= 7;
	/**
	 * Constant for the UPnP device match scale, indicating a match with the
	 * device revision. Value is 15.
	 */
	int		MATCH_MANUFACTURER_MODEL_REVISION			= 15;
	/**
	 * Constant for the UPnP device match scale, indicating a match with the
	 * device revision and the serial number. Value is 31.
	 */
	int		MATCH_MANUFACTURER_MODEL_REVISION_SERIAL	= 31;
	/**
	 * Constant for the value of the service property <code>DEVICE_CATEGORY</code>
	 * used for all UPnP devices. Value is "UPnP".
	 * 
	 * @see "<code>org.osgi.service.device.Constants.DEVICE_CATEGORY</code>"
	 */
	String	DEVICE_CATEGORY								= "UPnP";
	/**
	 * The <code>UPnP.export</code> service property is a hint that marks a device
	 * to be picked up and exported by the UPnP Service. Imported devices do not
	 * have this property set. The registered property requires no value.
	 * <p>
	 * The UPNP_EXPORT string is "UPnP.export".
	 */
	String	UPNP_EXPORT									= "UPnP.export";
	/**
	 * Property key for the Unique Device Name (UDN) property. It is the unique
	 * identifier of an instance of a <code>UPnPDevice</code>. The value of the
	 * property is a <code>String</code> object of the Device UDN. Value of the
	 * key is "UPnP.device.UDN". This property must be set.
	 */
	String	UDN											= "UPnP.device.UDN";
	/**
	 * Property key for the Unique Device ID property. This property is an alias
	 * to <code>UPnPDevice.UDN</code>. It is merely provided for reasons of
	 * symmetry with the <code>UPnPService.ID</code> property. The value of the
	 * property is a <code>String</code> object of the Device UDN. The value of
	 * the key is "UPnP.device.UDN".
	 */
	String	ID											= UDN;
	/**
	 * Property key for the UPnP Device Type property. Some standard property
	 * values are defined by the Universal Plug and Play Forum. The type string
	 * also includes a version number as defined in the UPnP specification. This
	 * property must be set.
	 * <p>
	 * For standard devices defined by a UPnP Forum working committee, this must
	 * consist of the following components in the given order separated by
	 * colons:
	 * <ul>
	 * <li><code>urn</code></li>
	 * <li>schemas-upnp-org</li>
	 * <li><code>device</code></li>
	 * <li>a device type suffix</li>
	 * <li>an integer device version</li>
	 * </ul>
	 * For non-standard devices specified by UPnP vendors following components
	 * must be specified in the given order separated by colons:
	 * <ul>
	 * <li><code>urn</code></li>
	 * <li>an ICANN domain name owned by the vendor</li>
	 * <li><code>device</code></li>
	 * <li>a device type suffix</li>
	 * <li>an integer device version</li>
	 * </ul>
	 * <p>
	 * To allow for backward compatibility the UPnP driver must automatically
	 * generate additional Device Type property entries for smaller versions
	 * than the current one. If for example a device announces its type as
	 * version 3, then properties for versions 2 and 1 must be automatically
	 * generated.
	 * <p>
	 * In the case of exporting a UPnPDevice, the highest available version must
	 * be announced on the network.
	 * <p>
	 * Syntax Example: <code>urn:schemas-upnp-org:device:deviceType:v</code>
	 * <p>
	 * The value is "UPnP.device.type".
	 */
	String	TYPE										= "UPnP.device.type";
	/**
	 * Mandatory property key for the device manufacturer's property. The
	 * property value holds a String representation of the device manufacturer's
	 * name. Value is "UPnP.device.manufacturer".
	 */
	String	MANUFACTURER								= "UPnP.device.manufacturer";
	/**
	 * Mandatory property key for the device model name. The property value
	 * holds a <code>String</code> object giving more information about the device
	 * model. Value is "UPnP.device.modelName".
	 */
	String	MODEL_NAME									= "UPnP.device.modelName";
	/**
	 * Mandatory property key for a short user friendly version of the device
	 * name. The property value holds a <code>String</code> object with the user
	 * friendly name of the device. Value is "UPnP.device.friendlyName".
	 */
	String	FRIENDLY_NAME								= "UPnP.device.friendlyName";
	/**
	 * Optional property key for a URL to the device manufacturers Web site. The
	 * value of the property is a <code>String</code> object representing the URL.
	 * Value is "UPnP.device.manufacturerURL".
	 */
	String	MANUFACTURER_URL							= "UPnP.device.manufacturerURL";
	/**
	 * Optional (but recommended) property key for a <code>String</code> object
	 * with a long description of the device for the end user. The value is
	 * "UPnP.device.modelDescription".
	 */
	String	MODEL_DESCRIPTION							= "UPnP.device.modelDescription";
	/**
	 * Optional (but recommended) property key for a <code>String</code> class
	 * typed property holding the model number of the device. Value is
	 * "UPnP.device.modelNumber".
	 */
	String	MODEL_NUMBER								= "UPnP.device.modelNumber";
	/**
	 * Optional property key for a <code>String</code> typed property holding a
	 * string representing the URL to the Web site for this model. Value is
	 * "UPnP.device.modelURL".
	 */
	String	MODEL_URL									= "UPnP.device.modelURL";
	/**
	 * Optional (but recommended) property key for a <code>String</code> typed
	 * property holding the serial number of the device. Value is
	 * "UPnP.device.serialNumber".
	 */
	String	SERIAL_NUMBER								= "UPnP.device.serialNumber";
	/**
	 * Optional property key for a <code>String</code> typed property holding the
	 * Universal Product Code (UPC) of the device. Value is "UPnP.device.UPC".
	 */
	String	UPC											= "UPnP.device.UPC";
	/**
	 * Optional (but recommended) property key for a <code>String</code> typed
	 * property holding a string representing the URL to a device representation
	 * Web page. Value is "UPnP.presentationURL".
	 */
	String	PRESENTATION_URL							= "UPnP.presentationURL";
	/**
	 * The property key that must be set for all embedded devices. It contains
	 * the UDN of the parent device. The property is not set for root devices.
	 * The value is "UPnP.device.parentUDN".
	 */
	String	PARENT_UDN									= "UPnP.device.parentUDN";
	/**
	 * The property key that must be set for all devices containing other
	 * embedded devices.
	 * <p>
	 * The value is an array of UDNs for each of the device's children (
	 * <code>String[]</code>). The array contains UDNs for the immediate
	 * descendants only.
	 * </p>
	 * <p>
	 * If an embedded device in turn contains embedded devices, the latter are
	 * not included in the array.
	 * </p>
	 * The UPnP Specification does not encourage more than two levels of
	 * nesting.
	 * <p>
	 * The property is not set if the device does not contain embedded devices.
	 * <p>
	 * The property is of type <code>String[]</code>. Value is
	 * "UPnP.device.childrenUDN"
	 */
	String	CHILDREN_UDN								= "UPnP.device.childrenUDN";

	/**
	 * Locates a specific service by its service id.
	 * 
	 * @param serviceId The service id
	 * @return The requested service or null if not found.
	 */
	UPnPService getService(String serviceId);

	/**
	 * Lists all services provided by this device.
	 * 
	 * @return Array of services or <code>null</code> if no services are
	 *         available.
	 */
	UPnPService[] getServices();

	/**
	 * Lists all icons for this device in a given locale.
	 * 
	 * The UPnP specification allows a device to present different icons based
	 * on the client's locale.
	 * 
	 * @param locale A language tag as defined by RFC 1766 and maintained by ISO
	 *        639. Examples include "<code>de</code>", "<code>en</code>" or "
	 *        <code>en-US</code>". The default locale of the device is specified
	 *        by passing a <code>null</code> argument.
	 * 
	 * @return Array of icons or null if no icons are available.
	 */
	UPnPIcon[] getIcons(String locale);

	/**
	 * Get a set of localized UPnP properties.
	 * 
	 * The UPnP specification allows a device to present different device
	 * properties based on the client's locale. The properties used to register
	 * the UPnPDevice service in the OSGi registry are based on the device's
	 * default locale. To obtain a localized set of the properties, an
	 * application can use this method.
	 * <p>
	 * Not all properties might be available in all locales. This method does
	 * <b>not </b> substitute missing properties with their default locale
	 * versions.
	 * <p>
	 * 
	 * @param locale A language tag as defined by RFC 1766 and maintained by ISO
	 *        639. Examples include "<code>de</code>", "<code>en</code>" or "
	 *        <code>en-US</code>". The default locale of the device is specified
	 *        by passing a <code>null</code> argument.
	 * @return Dictionary mapping property name Strings to property value
	 *         Strings
	 *  
	 */
	Dictionary getDescriptions(String locale);
}
