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
package org.apache.felix.webconsole;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;


/**
 * The <code>DefaultBrandingPlugin</code> class is the default implementation
 * of the {@link BrandingPlugin} interface. The singleton instance of this
 * class is used as branding plugin if no BrandingPlugin service is registered
 * in the system.
 * <p>
 * This default implementation provides Apache Felix based default branding
 * as follows:
 * <table>
 * <tr><th>Name</th><th>Property Name</th><th>Default Value</th></tr>
 * <tr>
 *  <td>Brand Name</td>
 *  <td>webconsole.brand.name</td>
 *  <td>Apache Felix Web Console</td>
 * </tr>
 * <tr>
 *  <td>Product Name</td>
 *  <td>webconsole.product.name</td>
 *  <td>Apache Felix</td>
 * </tr>
 * <tr>
 *  <td>Product URL</td>
 *  <td>webconsole.product.url</td>
 *  <td>http://felix.apache.org</td>
 * </tr>
 * <tr>
 *  <td>Product Image</td>
 *  <td>webconsole.product.image</td>
 *  <td>/res/imgs/logo.png</td>
 * </tr>
 * <tr>
 *  <td>Vendor Name</td>
 *  <td>webconsole.vendor.name</td>
 *  <td>The Apache Software Foundation</td>
 * </tr>
 * <tr>
 *  <td>Vendor URL</td>
 *  <td>webconsole.vendor.url</td>
 *  <td>http://www.apache.org</td>
 * </tr>
 * <tr>
 *  <td>Vendor Image</td>
 *  <td>webconsole.vendor.image</td>
 *  <td>/res/imgs/logo.png</td>
 * </tr>
 * <tr>
 *  <td>Favourite Icon</td>
 *  <td>webconsole.favicon</td>
 *  <td>/res/imgs/favicon.ico</td>
 * </tr>
 * <tr>
 *  <td>Main Stylesheet</td>
 *  <td>webconsole.stylesheet</td>
 *  <td>/res/ui/admin.css</td>
 * </tr>
 * </table>
 * <p>
 * If a properties file <code>META-INF/webconsole.properties</code> is available
 * through the class loader of this class, the properties overwrite the default
 * settings according to the property names listed above. The easiest way to
 * add such a properties file is to provide a fragment bundle with the file.
 */
public class DefaultBrandingPlugin implements BrandingPlugin
{

    /**
     * The name of the bundle entry providing branding properties for this
     * default branding plugin (value is "/META-INF/webconsole.properties").
     */
    private static final String BRANDING_PROPERTIES = "/META-INF/webconsole.properties";

    private static DefaultBrandingPlugin instance;

    private final String brandName;

    private final String productName;

    private final String productURL;

    private final String productImage;

    private final String vendorName;

    private final String vendorURL;

    private final String vendorImage;

    private final String favIcon;

    private final String mainStyleSheet;


    private DefaultBrandingPlugin()
    {
        Properties props = new Properties();

        // try to load the branding properties
        InputStream ins = getClass().getResourceAsStream( BRANDING_PROPERTIES );
        if ( ins != null )
        {
            try
            {
                props.load( ins );
            }
            catch ( IOException ignore )
            { /* ignore - will use defaults */
            }
            finally
            {
                IOUtils.closeQuietly( ins );
            }
        }

        // set the fields from the properties now
        brandName = props.getProperty( "webconsole.brand.name", "Apache Felix Web Console" );
        productName = props.getProperty( "webconsole.product.name", "Apache Felix" );
        productURL = props.getProperty( "webconsole.product.url", "http://felix.apache.org" );
        productImage = props.getProperty( "webconsole.product.image", "/res/imgs/logo.png" );
        vendorName = props.getProperty( "webconsole.vendor.name", "The Apache Software Foundation" );
        vendorURL = props.getProperty( "webconsole.vendor.url", "http://www.apache.org" );
        vendorImage = props.getProperty( "webconsole.vendor.image", "/res/imgs/logo.png" );
        favIcon = props.getProperty( "webconsole.favicon", "/res/imgs/favicon.ico" );
        mainStyleSheet = props.getProperty( "webconsole.stylesheet", "/res/ui/webconsole.css" );
    }


    /**
     * Retrieves the shared instance
     * 
     * @return the singleton instance of the object
     */
    public static DefaultBrandingPlugin getInstance()
    {
        if ( instance == null )
        {
            instance = new DefaultBrandingPlugin();
        }
        return instance;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getBrandName()
     */
    public String getBrandName()
    {
        return brandName;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getProductName()
     */
    public String getProductName()
    {
        return productName;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getProductURL()
     */
    public String getProductURL()
    {
        return productURL;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getProductImage()
     */
    public String getProductImage()
    {
        return productImage;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getVendorName()
     */
    public String getVendorName()
    {
        return vendorName;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getVendorURL()
     */
    public String getVendorURL()
    {
        return vendorURL;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getVendorImage()
     */
    public String getVendorImage()
    {
        return vendorImage;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getFavIcon()
     */
    public String getFavIcon()
    {
        return favIcon;
    }


    /**
     * @see org.apache.felix.webconsole.BrandingPlugin#getMainStyleSheet()
     */
    public String getMainStyleSheet()
    {
        return mainStyleSheet;
    }
}
