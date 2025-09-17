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
package javax.xml.parsers;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.validation.Schema;

public abstract class SAXParserFactory {

    private static final String DEFAULT_IMPL = "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl";

    private boolean validating = false;

    private boolean namespaceAware = false;

    protected SAXParserFactory() {

    }

    public static SAXParserFactory newDefaultInstance() {
        return $FactoryFinder.newInstance(SAXParserFactory.class, DEFAULT_IMPL, null, false, true);
    }


    public static SAXParserFactory newInstance() {
        return $FactoryFinder.find(SAXParserFactory.class, DEFAULT_IMPL);
    }

    public static SAXParserFactory newInstance(String factoryClassName, ClassLoader classLoader) {
        return $FactoryFinder.newInstance(SAXParserFactory.class, factoryClassName, classLoader, false);
    }


    public abstract SAXParser newSAXParser() throws ParserConfigurationException, SAXException;

    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    public boolean isValidating() {
        return validating;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public abstract void setFeature(String name, boolean value)
            throws ParserConfigurationException, SAXNotRecognizedException,
            SAXNotSupportedException;

    public abstract boolean getFeature(String name)
            throws ParserConfigurationException, SAXNotRecognizedException,
            SAXNotSupportedException;


    public Schema getSchema() {
        throw new UnsupportedOperationException(
                "This parser does not support specification \""
                        + this.getClass().getPackage().getSpecificationTitle()
                        + "\" version \""
                        + this.getClass().getPackage().getSpecificationVersion()
                        + "\""
        );
    }

    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException(
                "This parser does not support specification \""
                        + this.getClass().getPackage().getSpecificationTitle()
                        + "\" version \""
                        + this.getClass().getPackage().getSpecificationVersion()
                        + "\""
        );
    }

    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
                "This parser does not support specification \""
                        + this.getClass().getPackage().getSpecificationTitle()
                        + "\" version \""
                        + this.getClass().getPackage().getSpecificationVersion()
                        + "\""
        );
    }

    public void setXIncludeAware(final boolean state) {
        if (state) {
            throw new UnsupportedOperationException(" setXIncludeAware " +
                    "is not supported on this JAXP" +
                    " implementation or earlier: " + this.getClass());
        }
    }
}
