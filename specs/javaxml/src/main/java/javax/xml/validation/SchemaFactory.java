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
package javax.xml.validation;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.net.URL;

public abstract class SchemaFactory {

    private static final String DEFAULT_IMPL = "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory";

    protected SchemaFactory() {
    }

    public static SchemaFactory newDefaultInstance() {
        return new $SchemaFactoryFinder(null).createInstance(DEFAULT_IMPL, true);
    }

    public static SchemaFactory newInstance(String schemaLanguage) {
        ClassLoader cl = $SchemaFactoryFinder.getContextClassLoader();
        if (cl == null) {
            cl = SchemaFactory.class.getClassLoader();
        }
        SchemaFactory f = new $SchemaFactoryFinder(cl).newFactory(schemaLanguage);
        if (f == null) {
            throw new IllegalArgumentException(
                    "No SchemaFactory"
                            + " that implements the schema language specified by: " + schemaLanguage
                            + " could be loaded");
        }
        return f;
    }

    public static SchemaFactory newInstance(String schemaLanguage, String factoryClassName, ClassLoader classLoader) {
        ClassLoader cl = classLoader;
        if (cl == null) {
            cl = $SchemaFactoryFinder.getContextClassLoader();
        }
        SchemaFactory f = new $SchemaFactoryFinder(cl).createInstance(factoryClassName);
        if (f == null) {
            throw new IllegalArgumentException(
                    "Factory " + factoryClassName
                            + " could not be loaded to implement the schema language specified by: " + schemaLanguage);
        }
        if (f.isSchemaLanguageSupported(schemaLanguage)) {
            return f;
        } else {
            throw new IllegalArgumentException(
                    "Factory " + f.getClass().getName()
                            + " does not implement the schema language specified by: " + schemaLanguage);
        }

    }

    public abstract boolean isSchemaLanguageSupported(String schemaLanguage);

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    public void setProperty(String name, Object object)
            throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    public abstract void setErrorHandler(ErrorHandler errorHandler);

    public abstract ErrorHandler getErrorHandler();

    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    public abstract LSResourceResolver getResourceResolver();

    public Schema newSchema(Source schema) throws SAXException {
        return newSchema(new Source[]{schema});
    }

    public Schema newSchema(File schema) throws SAXException {
        return newSchema(new StreamSource(schema));
    }

    public Schema newSchema(URL schema) throws SAXException {
        return newSchema(new StreamSource(schema.toExternalForm()));
    }

    public abstract Schema newSchema(Source[] schemas) throws SAXException;

    public abstract Schema newSchema() throws SAXException;
}
