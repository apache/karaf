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

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

public abstract class SOAPFactory {

    private static final String DEFAULT_SOAP_FACTORY = "com.sun.xml.internal.messaging.saaj.soap.ver1_1.SOAPFactory1_1Impl";

    public SOAPElement createElement(Element domElement) throws SOAPException {
        throw new UnsupportedOperationException("createElement(org.w3c.dom.Element) must be overridden by all subclasses of SOAPFactory.");
    }

    public abstract SOAPElement createElement(Name name) throws SOAPException;

    public SOAPElement createElement(QName qname) throws SOAPException {
        throw new UnsupportedOperationException("createElement(QName) must be overridden by all subclasses of SOAPFactory.");
    }

    public abstract SOAPElement createElement(String localName) throws SOAPException;


    public abstract SOAPElement createElement(String localName, String prefix, String uri) throws SOAPException;

    public abstract Detail createDetail() throws SOAPException;

    public abstract SOAPFault createFault(String reasonText, QName faultCode) throws SOAPException;

    public abstract SOAPFault createFault() throws SOAPException;

    public abstract Name createName(String localName, String prefix, String uri) throws SOAPException;

    public abstract Name createName(String localName) throws SOAPException;

    public static SOAPFactory newInstance() throws SOAPException {
        try {
            SOAPFactory factory = $FactoryFinder.find(SOAPFactory.class, DEFAULT_SOAP_FACTORY, false);
            if (factory != null) {
                return factory;
            }
            return newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } catch (Exception ex) {
            throw new SOAPException("Unable to create SOAP Factory: " + ex.getMessage(), ex);
        }

    }

    public static SOAPFactory newInstance(String protocol) throws SOAPException {
        return SAAJMetaFactory.getInstance().newSOAPFactory(protocol);
    }
}
