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
package org.apache.karaf.services.web.internal.command;

import org.apache.karaf.services.web.WebElement;

/**
 * Formats the columns of the web commands.
 */
final class Elements {

    private Elements() {
    }

    static String path(WebElement element) {
        return element.getContextPath() == null ? "-" : element.getContextPath();
    }

    static String text(String value) {
        return value == null ? "" : value;
    }

    static String bundle(WebElement element) {
        return element.getBundleId() < 0 ? "" : Long.toString(element.getBundleId());
    }

    static String service(WebElement element) {
        return element.getServiceId() < 0 ? "" : Long.toString(element.getServiceId());
    }

}
