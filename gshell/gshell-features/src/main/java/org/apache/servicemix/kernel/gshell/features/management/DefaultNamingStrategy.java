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
package org.apache.servicemix.kernel.gshell.features.management;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.apache.servicemix.kernel.gshell.features.FeaturesRegistry;

/**
 * Naming strategy for JMX MBeans.
 */
public class DefaultNamingStrategy implements NamingStrategy {

    private String jmxDomainName;

    public String getJmxDomainName() {
        return jmxDomainName;
    }

    public void setJmxDomainName(String jmxDomainName) {
        this.jmxDomainName = jmxDomainName;
    }

    public ObjectName getObjectName(ManagedFeature feature) throws MalformedObjectNameException {
        return getObjectName(feature, false);
    }

    public ObjectName getObjectName(ManagedFeature feature, boolean installed) throws MalformedObjectNameException {
        StringBuffer sb = new StringBuffer();
        sb.append(jmxDomainName).append(":Service=Features,");

        if (installed) {
            sb.append("Type=Installed,");
        } else {
            sb.append("Type=Available,");
        }

        sb.append("Name=").append(sanitize(feature.getName())).append(",")
          .append("FeatureVersion=").append(sanitize(feature.getVersion()));

        return new ObjectName(sb.toString());
    }

    public ObjectName getObjectName(ManagedRepository repository) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":" +
                                    "Service=Features," +
                                    "Type=Repositories," +
                                    "Name=" + sanitize(repository.getUri().toString())); // + "," +
    }

    public ObjectName getObjectName(FeaturesRegistry featuresRegistry) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":" +
                                    "Service=Features," +
                                    "Name=FeaturesService");
    }

    private String sanitize(String in) {
        String result = null;
        if (in != null) {
            result = in.replace(':', '_');
            result = result.replace('/', '_');
            result = result.replace('\\', '_');
            result = result.replace('?', '_');
            result = result.replace('=', '_');
            result = result.replace(',', '_');
        }
        return result;
    }
}