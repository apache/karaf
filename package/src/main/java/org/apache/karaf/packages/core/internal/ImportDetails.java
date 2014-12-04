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
package org.apache.karaf.packages.core.internal;

import org.apache.karaf.packages.core.internal.filter.AndExpression;
import org.apache.karaf.packages.core.internal.filter.Expression;
import org.apache.karaf.packages.core.internal.filter.FilterParser;
import org.apache.karaf.packages.core.internal.filter.NotExpression;
import org.apache.karaf.packages.core.internal.filter.SimpleItem;


/**
 * Helps to parse the expression
 * 
 * This class is internal to hide the FilterParser in the PackageService api
 */
class ImportDetails {
    String name;
    String minVersion;
    String maxVersion;

    public ImportDetails(String filter) {
        Expression filterExpr = new FilterParser().parse(filter);
        if (filterExpr instanceof AndExpression) {
            AndExpression andExpr = (AndExpression)filterExpr;
            for (Expression expr : andExpr.expressions) {
                parseSimpleItem(expr);
            }
        }
        parseSimpleItem(filterExpr);
    }

    private void parseSimpleItem(Expression expr) {
        if (expr instanceof SimpleItem) {
            SimpleItem simpleItem = (SimpleItem)expr;
            if ("osgi.wiring.package".equals(simpleItem.attr)) {
                this.name = simpleItem.value;
            }
            if ("version".equals(simpleItem.attr)) {
                this.minVersion = simpleItem.value;
            }
        }
        if (expr instanceof NotExpression) {
            SimpleItem simpleItem = (SimpleItem)((NotExpression)expr).expression;
            this.maxVersion = simpleItem.value;
        }
    }
}
