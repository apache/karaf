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

import java.util.HashMap;
import java.util.Map;

public class ExprTokenizer {
    private String expr;
    private int currentPos;
    String[] operators = {"(", ")", "!", "&", "=", ">="};
    Map<Character, String> operatorMap;

    public ExprTokenizer(String expr) {
        this.expr = expr;
        this.currentPos = 0;
        operatorMap = new HashMap<>();
        for (String operator : operators) {
            operatorMap.put(operator.charAt(0), operator);
        }
    }
    
    public String peekNextToken() {
        int oldPos = currentPos;
        String token = nextToken();
        currentPos = oldPos;
        return token;
    }
    
    public String nextToken() {
        if (currentPos >= expr.length()) {
            return null;
        }
        while (isWhiteSpace()) {
            currentPos ++;
        }
        Character first = expr.charAt(currentPos);
        String operator = operatorMap.get(first);
        if (operator != null) {
            currentPos += operator.length();
            return operator;
        }
        int firstPos = currentPos;
        while (currentPos < expr.length() && !operatorMap.containsKey(expr.charAt(currentPos)) && !isWhiteSpace()) {
            currentPos ++;
        }
        return expr.substring(firstPos, currentPos); 
    }

    private boolean isWhiteSpace() {
        return expr.charAt(currentPos) == ' ';
    }
    
    public String toString() {
        return expr.substring(currentPos);
    }
}
