/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.url;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;

import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.Connection;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import shaded.org.ops4j.util.property.PropertiesPropertyResolver;

public class CustomBundleURLStreamHandlerFactory implements URLStreamHandlerFactory {

	private static final String MVN_URI_PREFIX = "mvn";
	private static final String WRAP_URI_PREFIX = "wrap";
    private static final String FEATURE_URI_PREFIX = "feature";
    private static final String SPRING_URI_PREFIX = "spring";
    private static final String BLUEPRINT_URI_PREFIX = "blueprint";
    private static final String WAR_URI_PREFIX = "war";

	private final MavenResolver mavenResolver;

	public CustomBundleURLStreamHandlerFactory() {
		this(null);
	}

	public CustomBundleURLStreamHandlerFactory(MavenResolver mavenResolver) {
		this.mavenResolver = mavenResolver;
	}

	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equals(MVN_URI_PREFIX)) {
			return new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL u) throws IOException {
					MavenResolver resolver = mavenResolver;
					if (resolver == null) {
						PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(System.getProperties());
						final MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, ServiceConstants.PID);
						resolver = new AetherBasedResolver(config);
					}
					return new Connection(u, resolver);
				}
			};
		} else if (protocol.equals(WRAP_URI_PREFIX)){
			return new org.ops4j.pax.url.wrap.Handler();
		} else if (protocol.equals(FEATURE_URI_PREFIX)){
			return new FeatureURLHandler();
		} else if (protocol.equals(SPRING_URI_PREFIX)){
			return new SpringURLHandler();
		} else if (protocol.equals(BLUEPRINT_URI_PREFIX)){
			return new BlueprintURLHandler();
        } else if (protocol.equals(WAR_URI_PREFIX)) {
            return new WarURLHandler();
		} else {
			return null;
		}
	}

	public static void install() {
		install(null);
	}

	public static void install(MavenResolver mavenResolver) {
		uninstall();
		URL.setURLStreamHandlerFactory(new CustomBundleURLStreamHandlerFactory(mavenResolver));
	}

	public static void uninstall() {
		try {
			Field handlersField = URL.class.getDeclaredField("handlers");
			Field factoryField = URL.class.getDeclaredField("factory");
			factoryField.setAccessible(true);
			factoryField.set(null, null);
			handlersField.setAccessible(true);
			handlersField.set(null, new Hashtable());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
