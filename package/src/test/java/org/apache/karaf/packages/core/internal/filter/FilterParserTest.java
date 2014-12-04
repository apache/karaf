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


import org.apache.karaf.packages.core.internal.filter.AndExpression;
import org.apache.karaf.packages.core.internal.filter.Expression;
import org.apache.karaf.packages.core.internal.filter.FilterParser;
import org.apache.karaf.packages.core.internal.filter.NotExpression;
import org.apache.karaf.packages.core.internal.filter.SimpleItem;
import org.apache.karaf.packages.core.internal.filter.SimpleItem.FilterType;
import org.junit.Assert;
import org.junit.Test;

public class FilterParserTest {
    @Test
    public void testSimpleItem() {
        Expression expr = new FilterParser().parse(" (a= b)");
        SimpleItem item = (SimpleItem) expr;
        checkItem(item, "a", FilterType.equal, "b");
    }

    private void checkItem(Expression expr, String expectedAttr, FilterType expectedFilterType, String expectedValue) {
        if (!(expr instanceof SimpleItem)) {
            Assert.fail("SimpleItem expected");
        }
        SimpleItem item = (SimpleItem)expr;
        Assert.assertEquals(expectedAttr, item.attr);
        Assert.assertEquals(expectedFilterType, item.filterType);
        Assert.assertEquals(expectedValue, item.value);
    }
    
    @Test
    public void testNotSimpleItem() {
        NotExpression not = (NotExpression) new FilterParser().parse("(!(a=b))");
        checkItem(not.expression, "a", FilterType.equal, "b");
    }
    
    @Test
    public void testPackageImport() {
        AndExpression expr = (AndExpression) new FilterParser().parse("(&(osgi.wiring.package=org.mypackage)(version>=1.9.0)(!(version>=2.0.0)))");
        Assert.assertEquals(3, expr.expressions.length);
        checkItem(expr.expressions[0], "osgi.wiring.package", FilterType.equal, "org.mypackage");
        checkItem(expr.expressions[1], "version", FilterType.gt, "1.9.0");
        NotExpression notVersion = (NotExpression)expr.expressions[2];
        checkItem(notVersion.expression, "version", FilterType.gt, "2.0.0");
    }
    
    @Test
    public void testPackageImportNoVersions() {
        Expression expr = new FilterParser().parse("(osgi.wiring.package=org.mypackage)");
        checkItem(expr, "osgi.wiring.package", FilterType.equal, "org.mypackage");
    }
}
