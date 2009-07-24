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

/**
 * The meta-information of a UPnP state variable as declared in the device's
 * service state table (SST).
 * <p>
 * Method calls to interact with a device (e.g.
 * <code>UPnPAction.invoke(...);</code>) use this class to encapsulate meta
 * information about the input and output arguments.
 * <p>
 * The actual values of the arguments are passed as Java objects. The mapping of
 * types from UPnP data types to Java data types is described with the field
 * definitions.
 * 
 * @version $Revision: 5673 $
 */
public interface UPnPStateVariable {
	/**
	 * Unsigned 1 <code>Byte</code> int.
	 * <p>
	 * Mapped to an <code>Integer</code> object.
	 */
	static final String	TYPE_UI1			= "ui1";
	/**
	 * Unsigned 2 Byte int.
	 * <p>
	 * Mapped to <code>Integer</code> object.
	 */
	static final String	TYPE_UI2			= "ui2";
	/**
	 * Unsigned 4 Byte int.
	 * <p>
	 * Mapped to <code>Long</code> object.
	 */
	static final String	TYPE_UI4			= "ui4";
	/**
	 * 1 Byte int.
	 * <p>
	 * Mapped to <code>Integer</code> object.
	 */
	static final String	TYPE_I1				= "i1";
	/**
	 * 2 Byte int.
	 * <p>
	 * Mapped to <code>Integer</code> object.
	 */
	static final String	TYPE_I2				= "i2";
	/**
	 * 4 Byte int.
	 * <p>
	 * Must be between -2147483648 and 2147483647
	 * <p>
	 * Mapped to <code>Integer</code> object.
	 */
	static final String	TYPE_I4				= "i4";
	/**
	 * Integer number.
	 * <p>
	 * Mapped to <code>Integer</code> object.
	 */
	static final String	TYPE_INT			= "int";
	/**
	 * 4 Byte float.
	 * <p>
	 * Same format as float. Must be between 3.40282347E+38 to 1.17549435E-38.
	 * <p>
	 * Mapped to <code>Float</code> object.
	 */
	static final String	TYPE_R4				= "r4";
	/**
	 * 8 Byte float.
	 * <p>
	 * Same format as float. Must be between -1.79769313486232E308 and
	 * -4.94065645841247E-324 for negative values, and between
	 * 4.94065645841247E-324 and 1.79769313486232E308 for positive values, i.e.,
	 * IEEE 64-bit (8-Byte) double.
	 * <p>
	 * Mapped to <code>Double</code> object.
	 */
	static final String	TYPE_R8				= "r8";
	/**
	 * Same as r8.
	 * <p>
	 * Mapped to <code>Double</code> object.
	 */
	static final String	TYPE_NUMBER			= "number";
	/**
	 * Same as r8 but no more than 14 digits to the left of the decimal point
	 * and no more than 4 to the right.
	 * <p>
	 * Mapped to <code>Double</code> object.
	 */
	static final String	TYPE_FIXED_14_4		= "fixed.14.4";
	/**
	 * Floating-point number.
	 * <p>
	 * Mantissa (left of the decimal) and/or exponent may have a leading sign.
	 * Mantissa and/or exponent may have leading zeros. Decimal character in
	 * mantissa is a period, i.e., whole digits in mantissa separated from
	 * fractional digits by period. Mantissa separated from exponent by E. (No
	 * currency symbol.) (No grouping of digits in the mantissa, e.g., no
	 * commas.)
	 * <p>
	 * Mapped to <code>Float</code> object.
	 */
	static final String	TYPE_FLOAT			= "float";
	/**
	 * Unicode string.
	 * <p>
	 * One character long.
	 * <p>
	 * Mapped to <code>Character</code> object.
	 */
	static final String	TYPE_CHAR			= "char";
	/**
	 * Unicode string.
	 * <p>
	 * No limit on length.
	 * <p>
	 * Mapped to <code>String</code> object.
	 */
	static final String	TYPE_STRING			= "string";
	/**
	 * A calendar date.
	 * <p>
	 * Date in a subset of ISO 8601 format without time data.
	 * <p>
	 * See <a
	 * href="http://www.w3.org/TR/xmlschema-2/#date">http://www.w3.org/TR/xmlschema-2/#date
	 * </a>.
	 * <p>
	 * Mapped to <code>java.util.Date</code> object. Always 00:00 hours.
	 */
	static final String	TYPE_DATE			= "date";
	/**
	 * A specific instant of time.
	 * <p>
	 * Date in ISO 8601 format with optional time but no time zone.
	 * <p>
	 * See <a
	 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">http://www.w3.org/TR/xmlschema-2/#dateTime
	 * </a>.
	 * <p>
	 * Mapped to <code>java.util.Date</code> object using default time zone.
	 */
	static final String	TYPE_DATETIME		= "dateTime";
	/**
	 * A specific instant of time.
	 * <p>
	 * Date in ISO 8601 format with optional time and optional time zone.
	 * <p>
	 * See <a
	 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">http://www.w3.org/TR/xmlschema-2/#dateTime
	 * </a>.
	 * <p>
	 * Mapped to <code>java.util.Date</code> object adjusted to default time zone.
	 */
	static final String	TYPE_DATETIME_TZ	= "dateTime.tz";
	/**
	 * An instant of time that recurs every day.
	 * <p>
	 * Time in a subset of ISO 8601 format with no date and no time zone.
	 * <p>
	 * See <a
	 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">http://www.w3.org/TR/xmlschema-2/#time
	 * </a>.
	 * <p>
	 * Mapped to <code>Long</code>. Converted to milliseconds since midnight.
	 */
	static final String	TYPE_TIME			= "time";
	/**
	 * An instant of time that recurs every day.
	 * <p>
	 * Time in a subset of ISO 8601 format with optional time zone but no date.
	 * <p>
	 * See <a
	 * href="http://www.w3.org/TR/xmlschema-2/#dateTime">http://www.w3.org/TR/xmlschema-2/#time
	 * </a>.
	 * <p>
	 * Mapped to <code>Long</code> object. Converted to milliseconds since
	 * midnight and adjusted to default time zone, wrapping at 0 and
	 * 24*60*60*1000.
	 */
	static final String	TYPE_TIME_TZ		= "time.tz";
	/**
	 * True or false.
	 * <p>
	 * Mapped to <code>Boolean</code> object.
	 */
	static final String	TYPE_BOOLEAN		= "boolean";
	/**
	 * MIME-style Base64 encoded binary BLOB.
	 * <p>
	 * Takes 3 Bytes, splits them into 4 parts, and maps each 6 bit piece to an
	 * octet. (3 octets are encoded as 4.) No limit on size.
	 * <p>
	 * Mapped to <code>byte[]</code> object. The Java byte array will hold the
	 * decoded content of the BLOB.
	 */
	static final String	TYPE_BIN_BASE64		= "bin.base64";
	/**
	 * Hexadecimal digits representing octets.
	 * <p>
	 * Treats each nibble as a hex digit and encodes as a separate Byte. (1
	 * octet is encoded as 2.) No limit on size.
	 * <p>
	 * Mapped to <code>byte[]</code> object. The Java byte array will hold the
	 * decoded content of the BLOB.
	 */
	static final String	TYPE_BIN_HEX		= "bin.hex";
	/**
	 * Universal Resource Identifier.
	 * <p>
	 * Mapped to <code>String</code> object.
	 */
	static final String	TYPE_URI			= "uri";
	/**
	 * Universally Unique ID.
	 * <p>
	 * Hexadecimal digits representing octets. Optional embedded hyphens are
	 * ignored.
	 * <p>
	 * Mapped to <code>String</code> object.
	 */
	static final String	TYPE_UUID			= "uuid";

	/**
	 * Returns the variable name.
	 * 
	 * <ul>
	 * <li>All standard variables defined by a UPnP Forum working committee
	 * must not begin with <code>X_</code> nor <code>A_</code>.</li>
	 * <li>All non-standard variables specified by a UPnP vendor and added to a
	 * standard service must begin with <code>X_</code>.</li>
	 * </ul>
	 * 
	 * @return Name of state variable. Must not contain a hyphen character nor a
	 *         hash character. Should be &lt; 32 characters.
	 */
	String getName();

	/**
	 * Returns the Java class associated with the UPnP data type of this state
	 * variable.
	 * <P>
	 * Mapping between the UPnP data types and Java classes is performed
	 * according to the schema mentioned above.
	 * 
	 * <pre>
	 * 
	 *  Integer              ui1, ui2, i1, i2, i4, int
	 *  Long                 ui4, time, time.tz
	 *  Float                r4, float
	 *  Double               r8, number, fixed.14.4
	 *  Character            char
	 *  String               string, uri, uuid
	 *  Date                 date, dateTime, dateTime.tz
	 *  Boolean              boolean
	 *  byte[]               bin.base64, bin.hex
	 *  
	 * </pre>
	 * 
	 * @return A class object corresponding to the Java type of this argument.
	 */
	Class getJavaDataType();

	/**
	 * Returns the UPnP type of this state variable. Valid types are defined as
	 * constants.
	 * 
	 * @return The UPnP data type of this state variable, as defined in above
	 *         constants.
	 */
	String getUPnPDataType();

	/**
	 * Returns the default value, if defined.
	 * 
	 * @return The default value or <code>null</code> if not defined. The type of
	 *         the returned object can be determined by <code>getJavaDataType</code>.
	 */
	Object getDefaultValue();

	/**
	 * Returns the allowed values, if defined. Allowed values can be defined
	 * only for String types.
	 * 
	 * @return The allowed values or <code>null</code> if not defined. Should be
	 *         less than 32 characters.
	 */
	String[] getAllowedValues();

	/**
	 * Returns the minimum value, if defined. Minimum values can only be defined
	 * for numeric types.
	 * 
	 * @return The minimum value or <code>null</code> if not defined.
	 */
	Number getMinimum();

	/**
	 * Returns the maximum value, if defined. Maximum values can only be defined
	 * for numeric types.
	 * 
	 * @return The maximum value or <code>null</code> if not defined.
	 */
	Number getMaximum();

	/**
	 * Returns the size of an increment operation, if defined. Step sizes can be
	 * defined only for numeric types.
	 * 
	 * @return The increment size or null if not defined.
	 */
	Number getStep();

	/**
	 * Tells if this StateVariable can be used as an event source.
	 * 
	 * If the StateVariable is eventable, an event listener service can be
	 * registered to be notified when changes to the variable appear.
	 * 
	 * @return <code>true</code> if the <code>StateVariable</code> generates events,
	 *         <code>false</code> otherwise.
	 */
	boolean sendsEvents();
}
