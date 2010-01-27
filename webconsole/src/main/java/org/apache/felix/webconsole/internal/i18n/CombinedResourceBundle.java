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
package org.apache.felix.webconsole.internal.i18n;


import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


class CombinedResourceBundle extends ResourceBundle
{

    private final ResourceBundle resourceBundle;
    private final ResourceBundle defaultResourceBundle;
    private final Locale locale;


    CombinedResourceBundle( final ResourceBundle resourceBundle, final ResourceBundle defaultResourceBundle,
        final Locale locale )
    {
        this.resourceBundle = resourceBundle;
        this.defaultResourceBundle = defaultResourceBundle;
        this.locale = locale;
    }


    public Locale getLocale()
    {
        return locale;
    }


    public Enumeration getKeys()
    {
        return new CombinedEnumeration( resourceBundle.getKeys(), defaultResourceBundle.getKeys() );
    }


    protected Object handleGetObject( String key )
    {
        // check primary resource bundle first
        try
        {
            return resourceBundle.getObject( key );
        }
        catch ( MissingResourceException mre )
        {
            // ignore
        }

        // now check the default resource bundle
        try
        {
            return defaultResourceBundle.getObject( key );
        }
        catch ( MissingResourceException mre )
        {
            // ignore
        }

        // finally fall back to using the key
        return key;
    }

}
