/*
 * $Header: /cvsroot/bnd/aQute.bnd/src/aQute/lib/osgi/Instruction.java,v 1.1 2009/01/19 14:08:30 pkriens Exp $
 * 
 * Copyright (c) OSGi Alliance (2006). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.lib.osgi;

import java.util.regex.*;

public class Instruction {
    Pattern pattern;
    String  instruction;
    boolean negated;
    boolean optional;
    
    public Instruction(String instruction, boolean negated) {
        this.instruction = instruction;
        this.negated = negated;
    }

    public boolean matches(String value) {
        return getMatcher(value).matches();
    }

    public boolean isNegated() {
        return negated;
    }

    public String getPattern() {
        return instruction;
    }

    /**
     * Convert a string based pattern to a regular expression based pattern.
     * This is called an instruction, this object makes it easier to handle the
     * different cases
     * 
     * @param string
     * @return
     */
    public static Instruction getPattern(String string) {
        boolean negated = false;
        if (string.startsWith("!")) {
            negated = true;
            string = string.substring(1);
        }
        StringBuffer sb = new StringBuffer();
        for (int c = 0; c < string.length(); c++) {
            switch (string.charAt(c)) {
            case '.':
                sb.append("\\.");
                break;
            case '*':
                sb.append(".*");
                break;
            case '?':
                sb.append(".?");
                break;
            default:
                sb.append(string.charAt(c));
                break;
            }
        }
        string = sb.toString();
        if (string.endsWith("\\..*")) {
            sb.append("|");
            sb.append(string.substring(0, string.length() - 4));
        }
        return new Instruction(sb.toString(), negated);
    }

    public String toString() {
        return getPattern();
    }

    public Matcher getMatcher(String value) {
        if (pattern == null) {
            pattern = Pattern.compile(instruction);
        }
        return pattern.matcher(value);
    }

    public int hashCode() {
        return instruction.hashCode();
    }

    public boolean equals(Object other) {
        return other != null && (other instanceof Instruction)
                && instruction.equals(((Instruction) other).instruction);
    }

    public void setOptional() {
        optional = true;
    }
    
    public boolean isOptional() {
        return optional;
    }

}
