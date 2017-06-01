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

import java.util.ArrayList;
import java.util.List;

public class FilterParser {
    public Expression parse(String filter) {
        return parse(new ExprTokenizer(filter));
    }

    private Expression parse(ExprTokenizer tokenizer) {
        if (!"(".equals(tokenizer.nextToken())) {
            throw new IllegalArgumentException("Invalid Syntax");
        }
        return parseFilterComp(tokenizer);
    }

    private Expression parseFilterComp(ExprTokenizer tokenizer) {
        String token = tokenizer.peekNextToken();
        if ("!".equals(token)) {
            tokenizer.nextToken();
            return new NotExpression(parse(tokenizer));
        }
        if ("&".equals(token)) {
            tokenizer.nextToken();
            return new AndExpression(parseFilterList(tokenizer));
        }
        return parseItem(tokenizer);
    }
    
    private Expression[] parseFilterList(ExprTokenizer tokenizer) {
        List<Expression> exprList = new ArrayList<>();
        while ("(".equals(tokenizer.peekNextToken())) {
            tokenizer.nextToken();
            exprList.add(parseFilterComp(tokenizer));
            tokenizer.nextToken();
        }
        return exprList.toArray(new Expression[]{});
    }

    private Expression parseItem(ExprTokenizer tokenizer) {
        String attr = tokenizer.nextToken();
        String filterType = tokenizer.nextToken();
        String value = tokenizer.nextToken();
        return new SimpleItem(attr, filterType, value);
    }

}
