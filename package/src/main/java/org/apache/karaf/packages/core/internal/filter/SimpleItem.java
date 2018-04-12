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
package org.apache.karaf.packages.core.internal.filter;

public class SimpleItem implements Expression {
    enum FilterType { equal, gt, lt }

    public String attr;
    public FilterType filterType;
    public String value;

    public SimpleItem(String attr, String filterType, String value) {
        this.attr = attr;
        this.filterType = toFilterType(filterType);
        this.value = value;
    }

    private FilterType toFilterType(String typeSt) {
        if ("=".equals(typeSt)) {
            return FilterType.equal;
        }
        if (">=".equals(typeSt)) {
            return FilterType.gt;
        }
        if ("<=".equals(typeSt)) {
            return FilterType.lt;
        }
        throw new IllegalArgumentException("Invalid FilterType " + typeSt);
    }
}
