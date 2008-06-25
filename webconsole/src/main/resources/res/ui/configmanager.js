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


function configure() {
    var span = document.getElementById('configField');
    if (!span) {
        return;
    }
    var select = document.getElementById('configSelection_pid');
    var pid = select.options[select.selectedIndex].value;
    var parm = pluginRoot + '/' + pid;
    sendRequest('POST', parm, displayConfigForm);
}


function create() {
    var span = document.getElementById('configField');
    if (!span) {
        return;
    }
    var select = document.getElementById('configSelection_factory');
    var pid = select.options[select.selectedIndex].value;
    var parm = pluginRoot + '/' + pid + '?create=true';
    sendRequest('POST', parm, displayConfigForm);
}

function displayConfigForm(obj) {
    var span1 = document.getElementById('configField');
    var span2 = document.getElementById('factoryField');
    if (!span1 && !span2) {
        return;
    }
    
    var parent = span1 ? span1.parentNode : span2.parentNode;
    
    clearChildren( parent );
    
    if (span1) {
        parent.appendChild( span1 );
    }
    if (span2) {
        parent.appendChild( span2 );
    }
    
    var trEl = tr( "content" );
    var tdEl = createElement( "th", "content", { colspan: 2 } );
    addText( tdEl, obj.title );
    trEl.appendChild( tdEl );
    parent.appendChild( trEl );

    // innerHtml += '<tr class="content">';
    // innerHtml += '<td class="content">&nbsp;</td>';
    trEl = tr( "content" );
    parent.appendChild( trEl );
    
    tdEl = td( "content" );
    addText( tdEl, "\u00a0" );
    trEl.appendChild( tdEl );
    
    // innerHtml += '<td class="content">';
    tdEl = td( "content" );
    trEl.appendChild( tdEl );
    
    // innerHtml += '<form method="post" action="' + pluginRoot + '/' + obj.pid + '">';
    // innerHtml += '<input type="hidden" name="apply" value="true" />';
    var formEl = createElement( "form", null, {
            method: "post",
            action: pluginRoot + "/" + obj.pid
        });
    tdEl.appendChild( formEl );
    
    var inputEl = createElement( "input", null, {
            type: "hidden",
            name: "apply",
            value: "true"
        });
    formEl.appendChild( inputEl );
    
    // add the factory PID as a hidden form field if present
    if (obj.factoryPid)
    {
        // innerHtml += '<input type="hidden" name="factoryPid" value="' + obj.factoryPid + '" />';
        inputEl = createElement( "input", null, {
                type: "hidden",
                name: "factoryPid",
                value: obj.factoryPid
            });
        formEl.appendChild( inputEl );
    }
    
    // innerHtml += '<input type="hidden" name="action" value="ajaxConfigManager" />';
    inputEl = createElement( "input", null, {
            type: "hidden",
            name: "action",
            value: "ajaxConfigManager"
        });
    formEl.appendChild( inputEl );
    
    // innerHtml += '<table border="0" width="100%">';
    var tableEl = createElement( "table", null, {
            border: 0,
            width: "100%"
        });
    formEl.appendChild( tableEl );
    
    var bodyEl = createElement( "tbody" );
    tableEl.appendChild( bodyEl );
    
    if (obj.description)
    {
        // innerHtml += '<tr class="content">';
        // innerHtml += '<td class="content" colspan="2">' + obj.description + '</td></tr>';
        trEl = tr( "content" );
        tdEl = td( "content", { colspan: 2 } );
        addText( tdEl, obj.description );
        trEl.appendChild( tdEl );
        bodyEl.appendChild( trEl );
    }
    
    if (obj.propertylist == 'properties')
    {
        printTextArea(bodyEl, obj.properties);
    }
    else
    {
        printForm(bodyEl, obj);
    }
    
    // innerHtml += '<tr class="content">';
    trEl = tr( "content" );
    bodyEl.appendChild( trEl );
    
    // innerHtml += '<td class="content">&nbsp;</td>';
    tdEl = td( "content" );
    addText( tdEl, "\u00a0" );
    trEl.appendChild( tdEl );
    
    // innerHtml += '<td class="content">';
    tdEl = td( "content" );
    trEl.appendChild( tdEl );
    
    // innerHtml += '<input type="submit" class="submit" name="submit" value="Save" />';
    tdEl.appendChild( createElement( "input", "submit", {
            type: "submit",
            name: "submit",
            value: "Save"
        })
    );
    
    // innerHtml += '&nbsp;&nbsp;&nbsp;';
    addText( tdEl, "\u00a0\u00a0\u00a0" );
    
    // innerHtml += '<input type="reset" class="submit" name="reset" value="Reset" />';
    tdEl.appendChild( createElement( "input", "submit", {
            type: "reset",
            name: "reset",
            value: "Reset"
        })
    );
    
    // innerHtml += '&nbsp;&nbsp;&nbsp;';
    // addText( tdEl, "\u00a0\u00a0\u00a0" );

    // innerHtml += '<input type="submit" class="submit" name="delete" value="Delete" onClick="return confirmDelete();"/>';
    tdEl.appendChild( createElement( "input", "submit", {
            type: "submit",
            name: "delete",
            value: "Delete",
            onClick: "return confirmDelete();"
        })
    );
    
    // innerHtml += '</td></tr>';

    // innerHtml += '</table>';
    // innerHtml += '</form>';
    // innerHtml += '</td></tr>';
    
    printConfigurationInfo(parent, obj);
}

function printTextArea(/* Element */ parent, props ) {
    
    var propsValue = "";
    for (var key in props) {
        propsValue += key + ' =  ' + props[key] + '\r\n';
    }

    return tr( "content", null, [
        td( "content aligntop", null, [
            text( "Properties" )
        ]),
        td( "content", { style: "width: 99%" }, [
            createElement( "textarea", null, {
                    name: "properties",
                    style: "height: 50%; width: 99%"
                }, [ text( propsValue ) ] ),
            text( "Enter Name-Value pairs of configuration properties" )
        ])
    ]);        

/*
    var innerHtml = '<tr class="content">';
    innerHtml += '<td class="content" style="vertical-align: top">Properties</td>';
    innerHtml += '<td class="content" style="width: 99%">';
    innerHtml += '<textarea name="properties" style="height: 50%; width: 99%">';
    innerHtml += '</textarea>';
    innerHtml += 'Enter Name-Value pairs of configuration properties.</td>';
    return innerHtml;
*/
}

function printForm( /* Element */ parent, obj ) {
    var propList;
    for (var idx in obj.propertylist)
    {
        var prop = obj.propertylist[idx];
        var attr = obj[prop];
  
        // innerHtml += '<tr class="content">';
        // innerHtml += '<td class="content" style="vertical-align: top">' + attr.name + '</td>';
        var trEl = tr( "content", null, [
                td( "content aligntop", null, [ text( attr.name ) ] )
            ]);
        parent.appendChild( trEl );

        // innerHtml += '<td class="content" style="width: 99%">';
        var tdEl = td( "content", { style: "width: 99%" } );
        trEl.appendChild( tdEl );
  
        if (attr.value != undefined)
        {
            // check is required to also handle empty strings, 0 and false
            tdEl.appendChild( createInput( prop, attr.value, attr.type, '99%' ) );
            tdEl.appendChild( createElement( "br" ) );
        }
        else if (typeof(attr.type) == 'object')
        {
        	// assume attr.values and multiselect
        	createMultiSelect( tdEl, prop, attr.values, attr.type, '99%' );
            tdEl.appendChild( createElement( "br" ) );
        }
        else if (attr.values.length == 0)
        {
            tdEl.appendChild( createSpan( prop, "", attr.type ) );
        }
        else
        {
            for (var vidx in attr.values)
            {
                tdEl.appendChild( createSpan( prop, attr.values[vidx], attr.type ) );
            }
        }
        
        if (attr.description)
        {
            addText( tdEl, attr.description );
        }
        
        if (propList) {
            propList += ',' + prop;
        } else {
            propList = prop;
        }
    }
    
    // innerHtml += '<input type="hidden" name="propertylist" value="' + propList + '"/>';
    parent.appendChild( createElement( "input", null, {
            type: "hidden",
            name: "propertylist",
            value: propList
        })
    );
}

function printConfigurationInfo( /* Element */ parent, obj )
{
    // var innerHtml = '<tr class="content">';
    // innerHtml += '<th colspan="2" class="content" >Configuration Information</th></tr>';
    parent.appendChild( tr( "content", null, [
            createElement( "th", "content", { colspan: 2 }, [
                text( "Configuration Information" )
            ])
        ])
    );
    
    // innerHtml += '<tr class="content">';
    // innerHtml += '<td class="content">Persistent Identity (PID)</td>';
    // innerHtml += '<td class="content">' + obj.pid + '</td></tr>';
    parent.appendChild( tr( "content", null, [
            td( "content", null, [
                text( "Persistent Identity (PID)" )
            ]),
            td( "content", null, [
                text( obj.pid )
            ])
        ])
    );

    if (obj.factoryPID)
    {
        // innerHtml += '<tr class="content">';
        // innerHtml += '<td class="content">Factory Peristent Identifier (Factory PID)</td>';
        // innerHtml += '<td class="content">' + obj.factoryPID + '</td></tr>';
        parent.appendChild( tr( "content", null, [
                td( "content", null, [
                    text( "Factory Peristent Identifier (Factory PID)" )
                ]),
                td( "content", null, [
                    text( obj.factoryPID )
                ])
            ])
        );
    }
    
    var binding = obj.bundleLocation;
    if (!binding)
    {
        binding = "Unbound or new configuration";
    }
    
    // innerHtml += '<tr class="content">';
    // innerHtml += '<td class="content">Configuration Binding</td>';
    // innerHtml += '<td class="content">' + binding + '</td></tr>';
    parent.appendChild( tr( "content", null, [
            td( "content", null, [
                text( "Configuration Binding" )
            ]),
            td( "content", null, [
                text( binding )
            ])
        ])
    );
}


var spanCounter = 0;
/* Element */ function createSpan(prop, value, type) {
    spanCounter++;
    var newId = prop + spanCounter;
    
    return createElement( "span", null, { id: newId }, [
        createInput( prop, value, type, '89%' ),
        createElement( "input", "input", {
                type: "button",
                value: "+",
                onClick: "addValue('" + prop + "', '" + newId + "');",
                style: "width: 5%"
            }),
        createElement( "input", "input", {
                type: "button",
                value: "-",
                onClick: "removeValue('" + newId + "');",
                style: "width: 5%"
            }),
        createElement( "br" )
    ]);
}

/* Element */ function createInput(prop, value, type, width) {
    if (type == 11) { // AttributeDefinition.BOOLEAN

        var inputEl = createElement( "input", "input", {
                type: "checkbox",
                name: prop,
                value: true
            });
            
        if (value && typeof(value) != "boolean")
        {
            value = value.toString().toLowerCase() == "true";
        }
        if (value)
        {
            inputEl.setAttribute( "checked", true );
        }
        
        return inputEl;
        
    } else if (typeof(type) == "object") { // predefined values
    
        var selectEl = createElement( "select", "select", {
                name: prop,
                style: "width: " + width
            });

    	var labels = type.labels;
    	var values = type.values;
        for (var idx in labels) {
            var optionEl = createElement( "option", null, {
                    value: values[idx]
                }, [ text( labels[idx] ) ]);
                
            if (value == values[idx])
            {
                optionEl.setAttribute( "selected", true );
            }
            selectEl.appendChild( optionEl );
    	}
        
    	return selectEl;
        
    } else { // Simple 
    
        return createElement( "input", "input", {
                type: "text",
                name: prop,
                value: value,
                style: "width: " + width
            });
    }
}

function createMultiSelect(/* Element */ parent, prop, values, options, width) {
    // convert value list into 'set'
    var valueSet = new Object();
    for (var idx in values) {
    	valueSet[ values[idx] ] = true;
    }
    
   	var labels = options.labels;
   	var values = options.values;
   	for (var idx in labels) {
    
        var inputEl = createElement( "input", null, {
                type: "checkbox",
                name: prop,
                value: values[idx] 
            });
    
        if (valueSet[ values[idx] ])
        {
            inputEl.setAttribute( "checked", true );
        }
        
        var labelEl = createElement( "label" );
        labelEl.appendChild( inputEl );
        addText( labelEl, labels[idx] );
        
        parent.appendChild( labelEl );
   	}
}


function addValue(prop, vidx)
{
    var span = document.getElementById(vidx);
    if (!span)
    {
        return;
    }
    var newSpan = createSpan(prop, '');
    span.parentNode.insertBefore(newSpan, span.nextSibling);
}

function removeValue(vidx)
{
    var span = document.getElementById(vidx);
    if (!span)
    {
        return;
    }
    span.parentNode.removeChild(span);
}

function confirmDelete()
{
    return confirm("Are you sure to delete this configuration ?");
}
