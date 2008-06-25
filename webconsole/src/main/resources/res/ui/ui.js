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


/* Element */ function clearChildren( /* Element */ element )
{
    while (element.firstChild)
    {
        element.removeChild(element.firstChild);
    }
    
    return element;
}

/* String */ function serialize( /* Element */ element )
{
    var result = "";
    
    if (element)
    {
        if (element.nodeValue)
        {
            result = element.nodeValue;
        }
        else {
            result += "<" + element.tagName;
            
            var attrs = element.attributes;
            for (var i=0; i < attrs.length; i++)
            {
                if (attrs[i].nodeValue)
                {
                    result += " " + attrs[i].nodeName + "='" + attrs[i].nodeValue + "'";
                }
            }
            
            var children = element.childNodes;
            if (children && children.length)
            {
                result += ">";
            
                for (var i=0; i < children.length; i++)
                {
                    result += serialize( children[i] );
                }
                result += "</" + element.tagName + ">";
            }
            else
            {
                result += "/>";
            }
        }
    }
    
    return result;
}

/* Element */ function tr( /* String */ cssClass, /* Map */ attrs, /* Element[] */ children )
{
    return createElement( "tr", cssClass, attrs, children );
}


/* Element */ function td( /* String */ cssClass, /* Map */ attrs, /* Element[] */ children )
{
    return createElement( "td", cssClass, attrs, children );
}


/* Element */ function text( /* String */ textValue )
{
    return document.createTextNode( textValue );
}


/* Element */ function createElement( /* String */ name, /* String */ cssClass, /* Map */ attrs, /* Element[] */ children  )
{
    var element = document.createElement( name );
    
    if (cssClass)
    {
        element.setAttribute( "class", cssClass ); // non-IE
        element.setAttribute( "className", cssClass ); // IE
    }
    
    if (attrs)
    {
        for (var lab in attrs)
        {
            if ("style" == lab)
            {
                var styles = attrs[lab];
                for (var styleName in styles)
                {
                    element.style[styleName] = styles[styleName];
                }
            }
            else
            {
                element.setAttribute( lab, attrs[lab] );
            }
        }
    }
    
    if (children && children.length)
    {
        for (var i=0; i < children.length; i++)
        {
            element.appendChild( children[i] );
        }
    }
    
    return element;
}


/* Element */ function addText( /* Element */ element, /* String */ textValue )
{
    if (element && textValue)
    {
        element.appendChild( text( textValue ) );
    }
    
    return element;
}