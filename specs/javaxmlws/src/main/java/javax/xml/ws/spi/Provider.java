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
package javax.xml.ws.spi;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.*;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class Provider {

    private static final String DEFAULT_JAXWSPROVIDER = "com.sun.xml.internal.ws.spi.ProviderImpl";

    protected Provider() {
    }

    public static Provider provider() {
        try {
            return $FactoryFinder.find(Provider.class, DEFAULT_JAXWSPROVIDER);
        } catch (WebServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebServiceException("Unable to createEndpointReference Provider", ex);
        }
    }

    public abstract ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName,
                                                          Class<? extends Service> serviceClass);

    public ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName,
                                                 Class<? extends Service> serviceClass, WebServiceFeature... features) {
        throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
    }

    public abstract Endpoint createEndpoint(String bindingId, Object implementor);

    public abstract Endpoint createAndPublishEndpoint(String address, Object implementor);

    public abstract EndpointReference readEndpointReference(Source eprInfoset);

    public abstract <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
                                  WebServiceFeature... features);

    public abstract W3CEndpointReference createW3CEndpointReference(String address, QName serviceName,
                                                                    QName portName, List<Element> metadata,
                                                                    String wsdlDocumentLocation,
                                                                    List<Element> referenceParameters);

    public W3CEndpointReference createW3CEndpointReference(String address,
                                                           QName interfaceName, QName serviceName, QName portName,
                                                           List<Element> metadata, String wsdlDocumentLocation,
                                                           List<Element> referenceParameters,
                                                           List<Element> elements, Map<QName, String> attributes) {
        throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
    }

    public Endpoint createAndPublishEndpoint(String address, Object implementor, WebServiceFeature... features) {
        throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
    }

    public Endpoint createEndpoint(String bindingId, Object implementor, WebServiceFeature... features) {
        throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
    }

    public Endpoint createEndpoint(String bindingId, Class<?> implementorClass,
                                   Invoker invoker, WebServiceFeature... features) {
        throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
    }

}
