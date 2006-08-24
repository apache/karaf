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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import java.util.jar.Attributes;

/** 
 * The GenericHandler class provides a general purpose mechanism for providing
 * an extensible set of classname based handler items. 
 *
 * @version $Revision: 14 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */
public abstract class GenericHandler
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    /** List of usable handler items in order of their declaration */
    public ArrayList handlerList = new ArrayList();
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    /**
     * For every property which matches the name itemKey.<n> create a handler
     * item and check the handler item class matches the specified class. Handler items
     * should contain either fully qualified classnames or classnames relative to the 
     * supplied defaultPkg.
     */
    public GenericHandler(String itemKey, Class clazz, String defaultPkg)
    {
        int ix = 0;
        String itemString = PropertyManager.getProperty(itemKey + ix++);
        while (itemString != null)
        {
            handlerList.add(create(itemString, clazz, defaultPkg));
            itemString = PropertyManager.getProperty(itemKey + ix++);
        }
    }
    
    /**
     * Create a handler list based on a set of manifest attributes.
     */
    public GenericHandler(Attributes atts, String itemKey, Class clazz, String defaultPkg)
    {
        int ix = 0;
        String itemString = atts.getValue(itemKey + ix++);
        while (itemString != null)
        {
            GenericHandlerItem item = create(itemString, clazz, defaultPkg);
            if (item != null)
            {
                handlerList.add(item);
            }
            itemString = atts.getValue(itemKey + ix++);
        }
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Create the handler item object for the specified itemString. 
     * HandlerItems are in the following format
     * 
     *      <item-class> : <item-options>
     *
     *      <item-class>    is either a fully qualified classname, or a classname within 
     *                      defaultPkg. 
     *      <item-options>  is a free-format string of item specific options.
     *
     */
    protected GenericHandlerItem create(String itemString, Class clazz, String defaultPkg)
    {
        GenericHandlerItem item = null;
        String itemName = itemString;
        String itemOptions = "";

        int itemSepPos = itemString.indexOf(' ');
        if (itemSepPos != -1)
        {
            itemName    = itemString.substring(0, itemSepPos).trim();
            itemOptions = itemString.substring(itemSepPos+1).trim();
        }
        
        try
        {
            Class itemClass;
            
            if (itemName.indexOf('.') == -1)
            {
                itemClass = Class.forName(defaultPkg + "." + itemName);
            }
            else
            {
                itemClass = Class.forName(itemName);
            }

            if (! clazz.isAssignableFrom(itemClass))
            {
                throw new ClassCastException("mismatched class type");
            }
            
            item = (GenericHandlerItem) itemClass.newInstance();
            item.setOptions(itemOptions);
        }
        catch (ClassNotFoundException cnfe)
        {
            System.err.println("Unable to load class for handler item: " + itemName);
        }
        catch (Exception e)
        {
            System.err.println("Exception creating handler item object: " + itemName
                               + "(" + e + ")");
        }
        
        return item;
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
