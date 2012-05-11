/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.util;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndentFormatter {

    public static void printFormatted(String prefix, String str, int termWidth, PrintStream out, boolean prefixFirstLine) {
        int pfxLen = prefix.length();
        int maxwidth = termWidth - pfxLen;
        Pattern wrap = Pattern.compile("(\\S\\S{" + maxwidth + ",}|.{1," + maxwidth + "})(\\s+|$)");
        int cur = 0;
        while (cur >= 0) {
            int lst = str.indexOf('\n', cur);
            String s = (lst >= 0) ? str.substring(cur, lst) : str.substring(cur);
            if (s.length() == 0) {
                out.println();
            } else {
                Matcher m = wrap.matcher(s);
                while (m.find()) {
                    if (cur > 0 || prefixFirstLine) {
                        out.print(prefix);
                    }
                    out.println(m.group());
                }
            }
            if (lst >= 0) {
                cur = lst + 1;
            } else {
                break;
            }
        }
    }

    public static void printFormatted(String prefix, String str, int termWidth, PrintStream out) {
        printFormatted(prefix, str, termWidth, out, true);
    }

}
