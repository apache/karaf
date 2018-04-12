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
package javax.xml.soap;

public abstract class SAAJMetaFactory {

    private static final String META_FACTORY_DEPRECATED_CLASS_PROPERTY = "javax.xml.soap.MetaFactory";

    private static final String DEFAULT_META_FACTORY_CLASS = "com.sun.xml.internal.messaging.saaj.soap.SAAJMetaFactoryImpl";

    static SAAJMetaFactory getInstance() throws SOAPException {
        try {
            return $FactoryFinder.find(SAAJMetaFactory.class, DEFAULT_META_FACTORY_CLASS, true, META_FACTORY_DEPRECATED_CLASS_PROPERTY);
        } catch (Exception e) {
            throw new SOAPException("Unable to create SAAJ meta-factory: " + e.getMessage(), e);
        }
    }

    protected SAAJMetaFactory() { }

    protected abstract MessageFactory newMessageFactory(String protocol) throws SOAPException;

    protected abstract SOAPFactory newSOAPFactory(String protocol) throws SOAPException;
}
