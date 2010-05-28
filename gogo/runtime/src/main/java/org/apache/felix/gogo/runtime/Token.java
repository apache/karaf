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
package org.apache.felix.gogo.runtime;

import org.apache.felix.gogo.runtime.Tokenizer.Type;

public class Token implements CharSequence
{
    Type type; 
    CharSequence value;
    short line;
    short column;
    
    public Token(Type type, CharSequence value, short line, short column)
    {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString()
    {
        //return type + "<" + value + ">";
        return null == value ? type.toString() : value.toString();
    }
    
    public char charAt(int index)
    {
        return  value.charAt(index);
    }

    public int length()
    {
        return (null == value ? 0 : value.length());
    }

    public CharSequence subSequence(int start, int end)
    {
        return value.subSequence(start, end);
    }
    
    public String source()
    {
        switch (type)
        {
            case WORD:
                return value.toString();
                
            case CLOSURE:
                return "{" + value + "}";
                
            case EXECUTION:
                return "(" + value + ")";
                
            case ARRAY:
                return "[" + value + "]";
                
            default:
                return type.toString();
        }
    }
}
