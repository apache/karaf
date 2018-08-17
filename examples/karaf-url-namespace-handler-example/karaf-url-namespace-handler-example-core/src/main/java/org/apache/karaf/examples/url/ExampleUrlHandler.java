/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.url;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@Component(
        property = { "url.handler.protocol=example" }
)
public class ExampleUrlHandler extends AbstractURLStreamHandlerService implements URLStreamHandlerService {

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        System.out.println("Thanks for using the Example URL !");
        return new ExampleConnection(url);
    }

    class ExampleConnection extends URLConnection {

        private InputStream inputStream;

        public ExampleConnection(URL url) throws MalformedURLException {
            super(new URL(url.toString().substring("example:".length())));
        }

        @Override
        public void connect() throws IOException {
            // fake, just using the regular URL
            inputStream = url.openStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (inputStream == null) {
                connect();
            }
            return inputStream;
        }
    }


}
