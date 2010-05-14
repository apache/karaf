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
// DWB14: parser loops if // comment at start of program
// DWB15: allow program to have trailing ';'
package org.apache.felix.gogo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.runtime.Tokenizer.Type;

public class Parser
{
    private final Tokenizer tz;

    public Parser(CharSequence program)
    {
        tz = new Tokenizer(program);
    }

    public List<List<List<Token>>> program()
    {
        List<List<List<Token>>> program = new ArrayList<List<List<Token>>>();
        
        while (tz.next() != Type.EOT)
        {
            program.add(pipeline());
            
            switch (tz.type())
            {
                case SEMICOLON:
                case NEWLINE:
                    continue;
            }
            
            break;
        }

        if (tz.next() != Type.EOT)
            throw new RuntimeException("Program has trailing text: " + tz.value());

        return program;
    }

    private List<List<Token>> pipeline()
    {
        List<List<Token>> pipeline = new ArrayList<List<Token>>();
        
        while (true)
        {
            pipeline.add(command());
            switch (tz.type())
            {
                case PIPE:
                    if (tz.next() == Type.EOT)
                    {
                        Token t = tz.token();
                        throw new EOFError(t.line, t.column, "unexpected EOT after pipe '|'");
                    }
                    break;

                default:
                    return pipeline;
            }
        }
    }

    private List<Token> command()
    {
        List<Token> command = new ArrayList<Token>();

        while (true)
        {
            Token t = tz.token();
            
            switch (t.type)
            {
                case WORD:
                case CLOSURE:
                case EXECUTION:
                case ARRAY:
                case ASSIGN:
                    break;
                    
                default:
                    throw new SyntaxError(t.line, t.column, "unexpected token: " + t.type);
            }
            
            command.add(t);
            
            switch (tz.next())
            {
                case PIPE:
                case SEMICOLON:
                case NEWLINE:
                case EOT:
                    return command;
            }
        }
    }
    
    public void array(List<Token> list, Map<Token, Token> map) throws Exception
    {
        Token lt = null;
        boolean isMap = false;

        while (tz.next() != Type.EOT)
        {
            if (isMap)
            {
                Token key = lt;
                lt = null;
                if (null == key)
                {
                    key = tz.token();

                    if (tz.next() != Type.ASSIGN)
                    {
                        Token t = tz.token();
                        throw new SyntaxError(t.line, t.column,
                            "map expected '=', found: " + t);
                    }

                    tz.next();
                }

                Token k = (list.isEmpty() ? key : list.remove(0));
                Token v = tz.token();
                map.put(k, v);
            }
            else
            {
                switch (tz.type())
                {
                    case WORD:
                    case CLOSURE:
                    case EXECUTION:
                    case ARRAY:
                        lt = tz.token();
                        list.add(lt);
                        break;

                    case ASSIGN:
                        if (list.size() == 1)
                        {
                            isMap = true;
                            break;
                        }
                        // fall through
                    default:
                        lt = tz.token();
                        throw new SyntaxError(lt.line, lt.column,
                            "unexpected token in list: " + lt);
                }
            }
        }
    }

}
