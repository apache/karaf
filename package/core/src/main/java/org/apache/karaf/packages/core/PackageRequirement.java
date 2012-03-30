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
package org.apache.karaf.packages.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;

public class PackageRequirement {
    private String filter;
    private boolean optional;
    private Bundle bundle;
    private boolean resolveable;
    private static Pattern packagePattern  = Pattern.compile(".*" + Pattern.quote("(osgi.wiring.package=") + "(.*?)\\).*");
    
    public PackageRequirement(String filter, boolean optional, Bundle bundle, boolean resolveable) {
        super();
        this.filter = filter;
        this.optional = optional;
        this.bundle = bundle;
        this.resolveable = resolveable;
    }
    
    public Bundle getBundle() {
        return bundle;
    }

    public String getFilter() {
        return filter;
    }
    public boolean isOptional() {
        return optional;
    }

    public boolean isResolveable() {
        return resolveable;
    }

    public String getPackageName() {
        Matcher matcher = packagePattern.matcher(filter);
        matcher.matches();
        return matcher.group(1);
    }

}
