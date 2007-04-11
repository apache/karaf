/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.metatype.internal;


import org.apache.felix.metatype.internal.l10n.Resources;
import org.osgi.service.log.LogService;


/**
 * The <code>LocalizedBase</code> class provides methods to localize strings
 * and string arrays on demand using a <code>ResourceBundle</code> specified
 * at construction time.
 *
 * @author fmeschbe
 */
abstract class LocalizedBase
{

    /**
     * The {@link Resources} used to localize strings.
     * 
     * @see #localize(String)
     * @see #localize(String[])
     */
    private final Resources resources;


    /**
     * Sets up this class using the given <code>ResourceBundle</code>.
     * 
     * @param resources The {@link Resources} used to localize return values of
     * localizable methods.
     *      
     * @throws NullPointerException If <code>resources</code> is
     *      <code>null</code>.
     */
    protected LocalizedBase( Resources resources )
    {
        if ( resources == null )
        {
            throw new NullPointerException( "resources" );
        }
        this.resources = resources;
    }


    /**
     * Returns the {@link Resources} assigned to this instance.
     */
    protected Resources getResources()
    {
        return resources;
    }


    /**
     * Calls {@link #localize(String)} for each string in the array and returns
     * an array of the resulting localized strings. If <code>strings</code> is
     * <code>null</code> <code>null</code> is returned.
     * 
     * @param strings An array of non-<code>null</code> strings to localize.
     * 
     * @return <code>null</code> if <code>strings</code> is <code>null</code> or
     *      an array of the same size as the <code>strings</code> array
     *      containing localized strings.
     */
    protected String[] localize( String[] strings )
    {
        if ( strings == null )
        {
            return null;
        }

        String[] localized = new String[strings.length];
        for ( int i = 0; i < strings.length; i++ )
        {
            localized[i] = localize( strings[i] );
        }
        return localized;
    }


    /**
     * Localizes the string using the
     * {@link #getResourceBundle() ResourceBundle} set on this instance if
     * string starts with the percent character (<code>%</code>). If the
     * string is <code>null</code>, does not start with a percent character
     * or the resource whose key is the string without the leading the percent
     * character is not found the string is returned without the leading percent
     * character.
     * <p>
     * Examples of different localizations:
     * <p>
     * <table border="0" cellspacing="0" cellpadding="3">
     *  <tr bgcolor="#ccccff">
     *   <th><code>string</code></th>
     *   <th>Key</th>
     *   <th>Resource</th>
     *   <th>Result</th>
     *  </tr>
     *  <tr>
     *   <td><code>null</code></td>
     *   <td>-</td>
     *   <td>-</td>
     *  <td><code>null</code></td>
     *  </tr>
     *  <tr bgcolor="#eeeeff">
     *   <td>sample</td>
     *   <td>-</td>
     *   <td>-</td>
     *   <td>sample</td>
     *  </tr>
     *  <tr>
     *   <td><b>%</b>sample</td>
     *   <td>sample</td>
     *   <td>-</td>
     *   <td>sample</td>
     *  </tr>
     *  <tr bgcolor="#eeeeff">
     *   <td><b>%</b>sample</td>
     *   <td>sample</td>
     *   <td>resource</td>
     *   <td>resource</td>
     *  </tr>
     * </table>
     * 
     * @param string The string to localize
     * @return The localized string
     */
    protected String localize( String string )
    {
        if ( string != null && string.startsWith( "%" ) )
        {
            string = string.substring( 1 );
            try
            {
                return getResources().getResource( string );
            }
            catch ( Exception e )
            {
                // ClassCastException, MissingResourceException
                Activator.log( LogService.LOG_DEBUG, "localize: Failed getting resource '" + string + "'", e );
            }
        }

        // just return the string unmodified
        return string;
    }
}
