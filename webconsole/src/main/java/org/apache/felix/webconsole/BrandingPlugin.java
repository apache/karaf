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


/**
 * The <code>BrandingPlugin</code> is the service interface for the most
 * elaborate way of branding the web console.
 *
 * @see DefaultBrandingPlugin
 */
public interface BrandingPlugin
{
    /**
     * Returns an indicative name of the branding plugin. This value is used
     * as the Window/Page title together with the title of the respective
     * plugin.
     * 
     * @return the name of the branding plugin
     */
    String getBrandName();


    /**
     * Returns the name of the product in which the web console is contained
     * and to which the web console is branded.
     *
     * @return the product name
     */
    String getProductName();


    /**
     * Returns an (absolute) URL to a web site representing the product to
     * which the web console is branded.
     *
     * @return the product URL
     */
    String getProductURL();


    /**
     * Returns an absolute path to an image to be rendered as the logo of the
     * branding product.
     *
     * @return a path to an image - usually the product logo
     */
    String getProductImage();


    /**
     * Returns the name of the branding product vendor.
     *
     * @return the product vendor
     */
    String getVendorName();


    /**
     * Returns an (absolute) URL to the web site of the branding product
     * vendor.
     *
     * @return the URL of the product vendor
     */
    String getVendorURL();


    /**
     * Returns an absolute path to an image to be rendered as the logo of the
     * branding product vendor.
     *
     * @return the company logo
     */
    String getVendorImage();


    /**
     * Returns the absolute path to an icon to be used as the web console
     * "favicon".
     *
     * @return path to an image, that is shown as favorite icon in the web browser
     */
    String getFavIcon();


    /**
     * Returns the absolute path to a CSS file to be used as the main CSS for
     * the basic admin site.
     *
     * @return a path to a custom CSS. Used to override the default web console styling
     */
    String getMainStyleSheet();
}
