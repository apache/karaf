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
package org.apache.felix.framework.util.ldap;

import java.util.Stack;
import java.util.Vector;

public class Evaluator {

    Object[] program = null;
    Stack operands = new Stack();
    Mapper mapper = null;

    public Evaluator()
    {
        reset();
    }

    public Evaluator(Object[] prog)
    {
        reset(prog);
    }

    public void reset()
    {
        program = null;
        mapper = null;
        operands.clear();
    }

    public void reset(Object[] prog)
    {
        reset();
        setProgram(prog);
    }

    public void setProgram(Object[] prog)
    {
        program = prog;
    }

    public void setMapper(Mapper mapper)
    {
        this.mapper = mapper;
    }

    public Stack getOperands()
    {
        return operands;
    }

    public boolean evaluate(Mapper mapper) throws EvaluationException
    {
        try
        {
            // The following code is a little complicated because it
            // is trying to deal with evaluating a given filter expression
            // when it contains an attribute that does not exist in the
            // supplied mapper. In such a situation the code below
            // catches the "attribute not found" exception and inserts
            // an instance of Unknown, which is used as a marker for
            // non-existent attributes. The Unknown instance forces the
            // operator to throw an "unsupported type" exception, which
            // the code below converts into a FALSE and this has the effect
            // of evaluating the subexpression that contained the
            // non-existent attribute to FALSE. The rest of the filter
            // expression evaluates normally. Any other exceptions are
            // rethrown.
            setMapper(mapper);
            for (int i = 0; i < program.length; i++)
            {
                try
                {
                    Operator op = (Operator) program[i];
                    op.execute(operands, mapper);
//                    printAction(op); // for debug output
                }
                catch (AttributeNotFoundException ex)
                {
                    operands.push(new Unknown());
                }
                catch (EvaluationException ex)
                {
                    // If the exception is for an unsupported type of
                    // type Unknown, then just push FALSE onto the
                    // operand stack because this type will only appear
                    // if an attribute was not found.
                    if (ex.isUnsupportedType() &&
                        (ex.getUnsupportedType() == Unknown.class))
                    {
                        operands.push(Boolean.FALSE);
                    }
                    // Otherwise, rethrow the exception.
                    else
                    {
                        throw ex;
                    }
                }
            }

            if (operands.empty())
            {
                throw new EvaluationException(
                    "Evaluation.evalute: final stack is empty");
            }

            Object result = operands.pop();

            if (!operands.empty())
            {
                throw new EvaluationException(
                    "Evaluation.evalute: final stack has more than one result");
            }

            if (!(result instanceof Boolean))
            {
                throw new EvaluationException(
                    "Evaluation.evalute: final result is not Boolean");
            }

            return ((Boolean) result).booleanValue();
        }
        finally
        {
            // Clear the operands just in case an exception was thrown,
            // otherwise stuff will be left in the stack.
            operands.clear();
        }
    }

    // For debugging; Dump the operator and stack
    void printAction(Operator op)
    {
        System.err.println("Operator:"+op.toString());
        System.err.print("Stack After:");
        // recast operands as Vector to make interior access easier
        Vector v = operands;
        int len = v.size();
        for (int i = 0; i < len; i++)
            System.err.print(" " + v.elementAt(i));
        System.err.println();
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < program.length; i++)
        {
            buf.append((i==0) ? "{" : ";");
                buf.append(((Operator) program[i]).toString());
        }
        buf.append("}");
        return buf.toString();
    }

    public String toStringInfix()
    {
        // First, we "evaluate" the program
        // but for the purpose of re-constructing
        // a parsetree.
        operands.clear();
        for (int i = 0; i < program.length; i++)
        {
            ((Operator) program[i]).buildTree(operands);
        }
        StringBuffer b = new StringBuffer();
        Object result = operands.pop();
        ((Operator)result).toStringInfix(b);
        operands.clear();
        return b.toString();
    }
}
