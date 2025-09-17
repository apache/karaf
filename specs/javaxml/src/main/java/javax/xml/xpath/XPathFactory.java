/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.xml.xpath;

public abstract class XPathFactory {

    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";

    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";

    private static final String DEFAULT_IMPL = "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl";

    protected XPathFactory() {
    }

    public static XPathFactory newDefaultInstance() {
        try {
            return new $XPathFactoryFinder(null).createInstance(DEFAULT_IMPL, true);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(
                    "XPathFactory#newInstance() failed to create an XPathFactory for the default object model: "
                            + DEFAULT_OBJECT_MODEL_URI
                            + " with the XPathFactoryConfigurationException: "
                            + e.getMessage(), e
            );
        }
    }

    public static XPathFactory newInstance() {
        try {
            return newInstance(DEFAULT_OBJECT_MODEL_URI);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(
                    "XPathFactory#newInstance() failed to create an XPathFactory for the default object model: "
                            + DEFAULT_OBJECT_MODEL_URI
                            + " with the XPathFactoryConfigurationException: "
                            + e.getMessage(), e
            );
        }
    }

    public static XPathFactory newInstance(final String uri) throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        ClassLoader classLoader = $XPathFactoryFinder.getContextClassLoader();
        if (classLoader == null) {
            classLoader = XPathFactory.class.getClassLoader();
        }
        XPathFactory xpathFactory = new $XPathFactoryFinder(classLoader).newFactory(uri);
        if (xpathFactory == null) {
            throw new XPathFactoryConfigurationException(
                    "No XPathFactory implementation found for the object model: "
                            + uri);
        }
        return xpathFactory;
    }

    public static XPathFactory newInstance(String uri, String factoryClassName, ClassLoader classLoader) throws XPathFactoryConfigurationException {
        ClassLoader cl = classLoader;
        if (uri == null) {
            throw new NullPointerException("XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException("XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        if (cl == null) {
            cl = $XPathFactoryFinder.getContextClassLoader();
        }
        XPathFactory f = new $XPathFactoryFinder(cl).createInstance(factoryClassName);

        if (f == null) {
            throw new XPathFactoryConfigurationException(
                    "No XPathFactory implementation found for the object model: "
                            + uri);
        }
        if (f.isObjectModelSupported(uri)) {
            return f;
        } else {
            throw new XPathFactoryConfigurationException("Factory "
                    + factoryClassName + " doesn't support given " + uri
                    + " object model");
        }

    }

    public abstract boolean isObjectModelSupported(String objectModel);

    public abstract void setFeature(String name, boolean value)
            throws XPathFactoryConfigurationException;

    public abstract boolean getFeature(String name)
            throws XPathFactoryConfigurationException;

    public abstract void setXPathVariableResolver(XPathVariableResolver resolver);

    public abstract void setXPathFunctionResolver(XPathFunctionResolver resolver);

    public abstract XPath newXPath();

}
