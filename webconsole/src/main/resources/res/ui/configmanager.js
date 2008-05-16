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
    var select = document.configSelection.pid;
    var pid = select.options[select.selectedIndex].value;
    var parm = '?action=ajaxConfigManager&' + pid;
    sendRequest('GET', parm, displayConfigForm);
}

function displayConfigForm(obj) {
    var span = document.getElementById('configField');
    if (!span) {
        return;
    }
    var innerHtml = '<tr class="content" id="configField">' + span.innerHTML + '</tr>';
    innerHtml += '<tr class="content">';
    innerHtml += '<th colspan="2" class="content" >' + obj.title + '</th></tr>';
    innerHtml += '<tr class="content">';
    innerHtml += '<td class="content">&nbsp;</td>';
    innerHtml += '<td class="content">';
    innerHtml += '<form method="post">';
    innerHtml += '<input type="hidden" name="apply" value="true" />';
    innerHtml += '<input type="hidden" name="pid" value="' + obj.pid + '" />';
    innerHtml += '<input type="hidden" name="action" value="ajaxConfigManager" />';
    innerHtml += '<table border="0" width="100%">';
    if (obj.description) {
        innerHtml += '<tr class="content">';
        innerHtml += '<td class="content" colspan="2">' + obj.description + '</td></tr>';
    }
    if (obj.propertylist == 'properties') {
        innerHtml += printTextArea(obj.properties);
    } else {
        innerHtml += printForm(obj);
    }
    innerHtml += '<tr class="content">';
    innerHtml += '<td class="content">&nbsp;</td>';
    innerHtml += '<td class="content">';
    innerHtml += '<input type="submit" class="submit" name="submit" value="Save" />';
    innerHtml += '&nbsp;&nbsp;&nbsp;';
    innerHtml += '<input type="reset" class="submit" name="reset" value="Reset" />';
    innerHtml += '&nbsp;&nbsp;&nbsp;';
    innerHtml += '<input type="submit" class="submit" name="delete" value="Delete" onClick="return confirmDelete();"/>';
    if (obj.isFactory) {
        innerHtml += '&nbsp;&nbsp;&nbsp;';
        innerHtml += '<input type="submit" class="submit" name="create" value="Create Configuration"/>';
    }
    innerHtml += '</td></tr>';
    innerHtml += '</table>';
    innerHtml += '</form>';
    innerHtml += '</td></tr>';
    innerHtml += printConfigurationInfo(obj);
    span.parentNode.innerHTML = innerHtml;
}

function printTextArea(props) {
    var innerHtml = '<tr class="content">';
    innerHtml += '<td class="content" style="vertical-align: top">Properties</td>';
    innerHtml += '<td class="content" style="width: 99%">';
    innerHtml += '<textarea name="properties" style="height: 50%; width: 99%">';
    for (var key in props) {
        innerHtml += key + ' =  ' + props[key] + '\r\n';
    }
    innerHtml += '</textarea>';
    innerHtml += 'Enter Name-Value pairs of configuration properties.</td>';
    return innerHtml;
}

function printForm(obj) {
    var innerHtml = '';
    var propList;
    for (var idx in obj.propertylist) {
        var prop = obj.propertylist[idx];
        var attr = obj[prop];
        innerHtml += '<tr class="content">';
        innerHtml += '<td class="content" style="vertical-align: top">' + attr.name + '</td>';
        innerHtml += '<td class="content" style="width: 99%">';
        if (attr.value != undefined) { // check is required to also handle empty strings, 0 and false
            innerHtml += createInput(prop, attr.value, attr.type, '99%');
            innerHtml += '<br />';
        } else if (typeof(attr.type) == 'object') {
        	// assume attr.values and multiselect
        	innerHtml += createMultiSelect(prop, attr.values, attr.type, '99%');
            innerHtml += '<br />';
        } else {
            for (var vidx in attr.values) {
                var spanElement = createSpan(prop, attr.values[vidx], attr.type);
                innerHtml += '<span id="' + spanElement.id + '">';
                innerHtml += spanElement.innerHTML;
                innerHtml += '</span>';
            }
        }
        if (attr.description) {
            innerHtml += attr.description;
        }
        innerHtml += '</td>';
        if (propList) {
            propList += ',' + prop;
        } else {
            propList = prop;
        }
    }
    innerHtml += '<input type="hidden" name="propertylist" value="' + propList + '"/>';
    return innerHtml;
}

function printConfigurationInfo(obj) {
    var innerHtml = '<tr class="content">';
    innerHtml += '<th colspan="2" class="content" >Configuration Information</th></tr>';
    innerHtml += '<tr class="content">';
    innerHtml += '<td class="content">Persistent Identity (PID)</td>';
    innerHtml += '<td class="content">' + obj.pid + '</td></tr>';
    if (obj.factoryPID) {
        innerHtml += '<tr class="content">';
        innerHtml += '<td class="content">Factory Peristent Identifier (Factory PID)</td>';
        innerHtml += '<td class="content">' + obj.factoryPID + '</td></tr>';
    }
    innerHtml += '<tr class="content">';
    innerHtml += '<td class="content">Configuration Binding</td>';
    innerHtml += '<td class="content">' + obj.bundleLocation + '</td></tr>';
    return innerHtml;
}

function addValue(prop, vidx) {
    var span = document.getElementById(vidx);
    if (!span) {
        return;
    }
    var newSpan = createSpan(prop, '');
    span.parentNode.insertBefore(newSpan, span.nextSibling);
}

var spanCounter = 0;
function createSpan(prop, value, type) {
    spanCounter++;
    var newId = prop + spanCounter;
    var innerHtml = createInput(prop, value, type, '89%');
    innerHtml += '<input class="input" type="button" value="+" onClick="addValue(\'' + prop + '\',\'' + newId + '\');" style="width: 5%" />';
    innerHtml += '<input class="input" type="button" value="-" onClick="removeValue(\'' + newId + '\');" style="width: 5%" />';
    innerHtml += '<br />';
    var newSpan = document.createElement('span');
    newSpan.id = newId;
    newSpan.innerHTML = innerHtml;
    return newSpan;
}

function createInput(prop, value, type, width) {
    if (type == 11) { // AttributeDefinition.BOOLEAN
        if (value && typeof(value) != "boolean") {
            value = value.toString().toLowerCase() == "true";
        }
        var checked = value ? 'checked' : '';
        return '<input class="input" type="checkbox" name="' + prop + '" value="true" ' + checked + '/>';
    } else if (typeof(type) == "object") { // predefined values
    	var labels = type.labels;
    	var values = type.values;
    	var innerHtml = '<select class="select" name="' + prop + '" style="width: ' + width + '">';
    	for (var idx in labels) {
    		var selected = (value == values[idx]) ? ' selected' : '';
    		innerHtml += '<option value="' + values[idx] + '"' + selected + '>' + labels[idx] + '</option>';
    	}
    	innerHtml += '</select>';
    	return innerHtml;
    } else { // Simple 
        return '<input class="input" type="text" name="' + prop + '" value="' + value + '" style="width: ' + width + '"/>';
    }
}

function createMultiSelect(prop, values, options, width) {
    // convert value list into 'set'
    var valueSet = new Object();
    for (var idx in values) {
    	valueSet[ values[idx] ] = true;
    }
    
   	var labels = options.labels;
   	var values = options.values;
   	var innerHtml = '';
   	for (var idx in labels) {
   		var checked = valueSet[ values[idx] ] ? ' checked' : '';
   		innerHtml += '<label><input type="checkbox" name="' + prop + '" value="' + values[idx] + '"' + checked + '>' + labels[idx] + '</label>';
   	}
   	return innerHtml;
}

function removeValue(vidx) {
    var span = document.getElementById(vidx);
    if (!span) {
        return;
    }
    span.parentNode.removeChild(span);
}

function confirmDelete() {
    return confirm("Are you sure to delete this configuration ?");
}
