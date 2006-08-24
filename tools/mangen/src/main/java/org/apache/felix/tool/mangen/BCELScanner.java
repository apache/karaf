/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tool.mangen;

import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;

import org.apache.bcel.classfile.*;

/**
 *
 * @version $Revision: 14 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public class BCELScanner
        implements ClassScanner
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    public BCELScanner()
    {
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    public JavaClass        jc;
    public Constant[]       constants = new Constant[0];
    public Method[]         methods = new Method[0];
    public Field[]          fields = new Field[0];
    public ConstantClass[]  constantClasses = new ConstantClass[0]; 
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - ClassScanner
    //////////////////////////////////////////////////

    public void scan(InputStream is, String name)
            throws IOException
    {
        ClassParser parser = new ClassParser(is, name);
        jc = parser.parse();
        
        constants = jc.getConstantPool().getConstantPool();
        methods = jc.getMethods();
        fields = jc.getFields();
        
        // extract out constant classes for later
        ArrayList cls = new ArrayList();
        for(int ix=0; ix < constants.length; ix++) 
        {
            if (constants[ix] instanceof ConstantClass)
            {
                cls.add(constants[ix]); 
            }
        }
        constantClasses = (ConstantClass[]) cls.toArray(constantClasses);
    }

    
    public int getMethodCount()
    {
        return methods.length;
    }
    
    
    public String getMethodName(int index)
    {
        return methods[index].getName();
    }

    
    public String getMethodSignature(int index)
    {
        return methods[index].getSignature();
    }

    
    public boolean isSyntheticMethod(int index)
    {
        return isSynthetic(methods[index]);
    }
    
    
    public int getFieldCount()
    {
        return fields.length;        
    }

    
    public String getFieldName(int index)
    {
        return fields[index].getName();
    }

    
    public String getFieldSignature(int index)
    {
        return fields[index].getSignature();
    }
    
    public boolean isSyntheticField(int index)
    {
        return isSynthetic(fields[index]);
    }

    
    public int getConstantClassCount()
    {
        return constantClasses.length;        
    }
    
    
    public String getConstantClassSignature(int index)
    {
        int nameIndex = constantClasses[index].getNameIndex();
        return ((ConstantUtf8) constants[nameIndex]).getBytes();
    }

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Scan any attributes present and return true if the Synthetic attribute
     * is found.
     */
    protected boolean isSynthetic(FieldOrMethod fOrM)
    {
        boolean found = false;
        
        Attribute[] att = fOrM.getAttributes();
        for (int ix = 0; ix < att.length && !found; ix++)
        {
            if (att[ix] instanceof Synthetic)
            {
                found = true;
            }
        }
        
        return found;
    }
    
    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    
}
