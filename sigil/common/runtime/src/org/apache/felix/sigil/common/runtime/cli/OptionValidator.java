/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.felix.sigil.common.runtime.cli;

/**
 * Validates an Option string.
 *
 * @author John Keyes ( john at integralsource.com )
 * @version $Revision: 680644 $, $Date: 2008-07-29 01:13:48 -0700 (Tue, 29 Jul 2008) $
 * @since 1.1
 */
class OptionValidator
{
    /**
     * Validates whether <code>opt</code> is a permissable Option
     * shortOpt.  The rules that specify if the <code>opt</code>
     * is valid are:
     *
     * <ul>
     *  <li><code>opt</code> is not NULL</li>
     *  <li>a single character <code>opt</code> that is either
     *  ' '(special case), '?', '@' or a letter</li>
     *  <li>a multi character <code>opt</code> that only contains
     *  letters.</li>
     * </ul>
     *
     * @param opt The option string to validate
     * @throws IllegalArgumentException if the Option is not valid.
     */
    static void validateOption(String opt) throws IllegalArgumentException
    {
        // check that opt is not NULL
        if (opt == null)
        {
            return;
        }

        // handle the single character opt
        else if (opt.length() == 1)
        {
            char ch = opt.charAt(0);

            if (!isValidOpt(ch))
            {
                throw new IllegalArgumentException("illegal option value '" + ch + "'");
            }
        }

        // handle the multi character opt
        else
        {
            char[] chars = opt.toCharArray();

            for (int i = 0; i < chars.length; i++)
            {
                if (!isValidChar(chars[i]))
                {
                    throw new IllegalArgumentException("opt contains illegal character value '" + chars[i] + "'");
                }
            }
        }
    }

    /**
     * Returns whether the specified character is a valid Option.
     *
     * @param c the option to validate
     * @return true if <code>c</code> is a letter, ' ', '?' or '@',
     *         otherwise false.
     */
    private static boolean isValidOpt(char c)
    {
        return isValidChar(c) || c == ' ' || c == '?' || c == '@';
    }

    /**
     * Returns whether the specified character is a valid character.
     *
     * @param c the character to validate
     * @return true if <code>c</code> is a letter.
     */
    private static boolean isValidChar(char c)
    {
        return Character.isJavaIdentifierPart(c);
    }
}
