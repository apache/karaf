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
package javax.xml.transform;

public abstract class TransformerFactory {

    private static final String DEFAULT_IMPL = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";

    protected TransformerFactory() {
    }

    public static TransformerFactory newDefaultInstance() {
        return $FactoryFinder.newInstance(TransformerFactory.class, DEFAULT_IMPL, null, false, true);
    }

    public static TransformerFactory newInstance() throws TransformerFactoryConfigurationError {
        return $FactoryFinder.find(TransformerFactory.class, DEFAULT_IMPL);
    }

    public static TransformerFactory newInstance(String factoryClassName, ClassLoader classLoader) throws TransformerFactoryConfigurationError {
        return $FactoryFinder.newInstance(TransformerFactory.class, factoryClassName, classLoader, false, false);
    }

    public abstract Transformer newTransformer(Source source) throws TransformerConfigurationException;

    public abstract Transformer newTransformer() throws TransformerConfigurationException;

    public abstract Templates newTemplates(Source source) throws TransformerConfigurationException;

    public abstract Source getAssociatedStylesheet(Source source, String media, String title, String charset) throws TransformerConfigurationException;

    public abstract void setURIResolver(URIResolver resolver);

    public abstract URIResolver getURIResolver();

    public abstract void setFeature(String name, boolean value) throws TransformerConfigurationException;

    public abstract boolean getFeature(String name);

    public abstract void setAttribute(String name, Object value);

    public abstract Object getAttribute(String name);

    public abstract void setErrorListener(ErrorListener listener);

    public abstract ErrorListener getErrorListener();

}
