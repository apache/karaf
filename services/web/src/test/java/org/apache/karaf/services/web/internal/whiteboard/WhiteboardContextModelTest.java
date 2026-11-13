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
package org.apache.karaf.services.web.internal.whiteboard;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.karaf.services.web.WebElement;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

public class WhiteboardContextModelTest {

    private final TestBundles bundles = new TestBundles();
    private final Bundle bundle = bundles.bundle(10);

    @Test
    public void singletonServletIsDeployedWhereItIsNotShadowed() {
        WhiteboardContextModel first = context("first", "/first");
        WhiteboardContextModel second = context("second", "/second");
        WhiteboardService other = servlet("/api", 10, Constants.SCOPE_SINGLETON);
        WhiteboardService singleton = servlet("/api", 0, Constants.SCOPE_SINGLETON);
        first.add(other);
        first.add(singleton);
        second.add(singleton);

        List<WebElement> failures = resolve(first, second);

        assertEquals(List.of(other), services(first));
        assertEquals(List.of(singleton), services(second));
        assertEquals(1, failures.size());
        assertEquals(WebElement.State.SHADOWED, failures.get(0).getState());
    }

    @Test
    public void singletonServletIsDeployedInOneContext() {
        WhiteboardContextModel first = context("first", "/first");
        WhiteboardContextModel second = context("second", "/second");
        WhiteboardService singleton = servlet("/api", 0, Constants.SCOPE_SINGLETON);
        WhiteboardService prototype = servlet("/other", 0, Constants.SCOPE_PROTOTYPE);
        for (WhiteboardContextModel context : List.of(first, second)) {
            context.add(singleton);
            context.add(prototype);
        }

        List<WebElement> failures = resolve(first, second);

        assertEquals(List.of(singleton, prototype), services(first));
        assertEquals(List.of(prototype), services(second));
        assertEquals(1, failures.size());
        assertEquals(WebElement.State.FAILED, failures.get(0).getState());
        assertEquals("/second", failures.get(0).getContextPath());
    }

    private WhiteboardService servlet(String pattern, int ranking, String scope) {
        return bundles.register(WhiteboardService.Kind.SERVLET, bundle, using -> null,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, pattern,
                Constants.SERVICE_RANKING, ranking, Constants.SERVICE_SCOPE, scope);
    }

    private static WhiteboardContextModel context(String name, String path) {
        return new WhiteboardContextModel(name, path, null);
    }

    /**
     * Resolves the contexts in their ranking order.
     */
    private static List<WebElement> resolve(WhiteboardContextModel... contexts) {
        List<WebElement> failures = new ArrayList<>();
        Map<WhiteboardService, WhiteboardContextModel> owners = new HashMap<>();
        for (WhiteboardContextModel context : contexts) {
            context.resolve(failures, owners);
        }
        return failures;
    }

    private static List<WhiteboardService> services(WhiteboardContextModel context) {
        return context.getServlets().stream().map(WhiteboardContextModel.ServletModel::getService)
                .collect(Collectors.toList());
    }

}
