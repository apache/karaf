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
// tables container - will get hidden, when no config service available
var configContent = false;

// config table list
var configTable = false;
var configBody  = false;
var configRow = false;

// factories table list
var factoryBody  = false;
var factoryRow = false;


// editor dialog
var editor = false;
var editorMessage = false;

function configure(pid, create) {
	var uri = pluginRoot + '/' + pid;
	$.post(create ? uri + '?create=1' : uri, null, displayConfigForm, 'json');
}

function displayConfigForm(obj) {
	var parent = document.getElementById('editorTable');
	clearChildren( parent )

    var trEl = tr( );
    parent.appendChild( trEl );
    
    var tdEl = td( null, { colSpan: "2" } );
    trEl.appendChild( tdEl );
    
    var formEl = createElement( "form", null, {
			id    : "editorForm",
            method: "POST",
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
        inputEl = createElement( "input", null, {
                type: "hidden",
                name: "factoryPid",
                value: obj.factoryPid
            });
        formEl.appendChild( inputEl );
    }
    
    // add the PID filter as a hidden form field if present
    if (obj.pidFilter)
    {
        inputEl = createElement( "input", null, {
                type: "hidden",
                name: "pidFilter",
                value: obj.pidFilter
            });
        formEl.appendChild( inputEl );
    }
    
    inputEl = createElement( "input", null, {
            type: "hidden",
            name: "action",
            value: "ajaxConfigManager"
        });
    formEl.appendChild( inputEl );
    
    var tableEl = createElement( "table", null, {
            border: 0,
            width: "100%"
        });
    formEl.appendChild( tableEl );
    
    var bodyEl = createElement( "tbody" );
    tableEl.appendChild( bodyEl );
    
    if (obj.description)
    {
        trEl = tr( );
        tdEl = td( null, { colSpan: "2" } );
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

    printConfigurationInfo(parent, obj);
	if ( obj.service_location && obj.bundle_location && obj.service_location != obj.bundle_location) {
		editorMessage.removeClass('ui-helper-hidden').text(i18n.err_bind.msgFormat(obj.pid, obj.bundle_location, obj.service_location));
	} else editorMessage.addClass('ui-helper-hidden');
	initStaticWidgets(editor.attr('__pid', obj.pid).dialog('option', 'title', obj.title).dialog('open'));
}

function printTextArea(/* Element */ parent, props )
{
    
    var propsValue = "";
    for (var key in props)
    {
        propsValue += key + ' =  ' + props[key] + '\r\n';
    }

    parent.appendChild(
        tr( null, null, [
            td( null, null, [
                text( i18n.props_title )
            ]),
            td( null, { style: { width: "99%" } }, [
                createElement( "textarea", null, {
                        name: "properties",
                        style: { height: "20em", width: "99%" }
                    }, [ text( propsValue ) ] ),
                createElement( "br" ),
                text( i18n.props_enter )
            ])
        ])
    );        
}

function printForm( /* Element */ parent, obj ) {
    var propList;
    for (var idx in obj.propertylist)
    {
        var prop = obj.propertylist[idx];
        var attr = obj[prop];
  
        var trEl = tr( null, null, [
                td( null, null, [ text( attr.name ) ] )
            ]);
        parent.appendChild( trEl );

        var tdEl = td( null, { style: { width: "99%" } } );
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
            for (var i=0;i<attr.values.length;i++)
            {
                tdEl.appendChild( createSpan( prop, attr.values[i], attr.type ) );
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
    
    parent.appendChild( createElement( "input", null, {
            type: "hidden",
            name: "propertylist",
            value: propList
        })
    );
    // FIX for IE6 and above: checkbox can only be checked after it is in the DOM
    $(".checked_box").attr("checked", true).removeClass("checked_box");
}

function printConfigurationInfo( /* Element */ parent, obj )
{
    parent.appendChild( tr( null, null, [
            createElement( "th", null, { colSpan: "2" }, [
                text( i18n.cfg_title )
            ])
        ])
    );
    
    parent.appendChild( tr( null, null, [
            td( null, null, [
                text( i18n.pid )
            ]),
            td( null, null, [
                text( obj.pid )
            ])
        ])
    );

    if (obj.factoryPid)
    {
        parent.appendChild( tr( null, null, [
                td( null, null, [
                    text( i18n.fpid )
                ]),
                td( null, null, [
                    text( obj.factoryPid )
                ])
            ])
        );
    }
    
    var binding = obj.bundleLocation;
    if (!binding)
    {
        binding = i18n.unbound;
    }
    
    parent.appendChild( tr( null, null, [
            td( null, null, [
                text( i18n.binding )
            ]),
            td( null, null, [
                text( binding )
            ])
        ])
    );

}


var spanCounter = 0;
/* Element */ function createSpan(prop, value, type) {
    spanCounter++;
    var newId = prop + spanCounter;
    
    var addButton = createElement("input", null,
    		{   type: "button",
    	        style: {width : "5%"},
    	        value: "+"
    	    }
      );
    $(addButton).click(function() {addValue(prop, newId)});
    var remButton = createElement("input", null,
    		{   type: "button",
    	        style: {width : "5%"},
    	        value: "-"
    	    }
      );
    $(remButton).click(function() {removeValue(newId)});
    var spanEl = createElement( "span", null, { id: newId }, [
        createInput( prop, value, type, '89%' ), addButton, remButton,
        createElement("br")
    ]);
    
    return spanEl;
}

/* Element */ function createInput(prop, value, type, width) {
    if (type == 11) { // AttributeDefinition.BOOLEAN

        var inputEl = createElement( "input", null, {
                type: "checkbox",
                name: prop,
                value: "true"
            });
            
        if (value && typeof(value) != "boolean")
        {
            value = value.toString().toLowerCase() == "true";
        }
        if (value)
        {
        	$(inputEl).addClass("checked_box");
        }
        var hiddenEl = createElement( "input", null, {
            type: "hidden",
            name: prop,
            value: "false"
        });
        var divEl = createElement("div");
        divEl.appendChild(inputEl);
        divEl.appendChild(hiddenEl);
        return divEl;
        
    } else if (typeof(type) == "object") { // predefined values
    
        var selectEl = createElement( "select", null, {
                name: prop,
                style: { width: width }
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
        return createElement( "input", null, {
                type: "text",
                name: prop,
                value: value,
                style: { width: width }
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
        
        var labelEl = createElement( "label", "multiselect" );
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
    // FIX for IE6 and above: checkbox can only be checked after it is in the DOM
    $(".checked_box").attr("checked", true).removeClass("checked_box");
	//$(span).ready(initStaticWidgets);
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

function configConfirm(/* String */ message, /* String */ title, /* String */ location)
{
    var message = i18n.del_ask;
    
    if (title) {
        message += "\r\n" + i18n.del_config + title;
    }
    if (location) {
        message += "\r\n" + i18n.del_bundle + location;
    }
    
    return confirm(message);
}

function confirmDelete(/* String */ title, /* String */ location)
{
    return configConfirm(i18n.del_ask, title, location);
}

function confirmUnbind(/* String */ title, /* String */ location)
{
    return configConfirm(i18n.unbind_ask, title, location);
}

function addConfig(conf) {
	var tr = configRow.clone().appendTo(configBody);

	// rendering name - indented if factory pid is set
	var nms = tr.find('td:eq(0) span')
	if (conf.fpid) { 
		nms.after(conf.id); 
		tr.attr('fpid', conf.name);
	} else {
		nms.addClass('ui-helper-hidden').parent().text(conf.name);
	}

	tr.find('td:eq(0)').click(function() { // name & edit
		configure(conf.id);
	});
	tr.find('td:eq(1)').html(conf.bundle ? '<a href="' + pluginRoot + '/../bundles/' + conf.bundle + '">' + conf.bundle_name + '</a>' : '-'); // binding
	
	// buttons
	tr.find('li:eq(0)').click(function() { // edit
		configure(conf.id);
	});
	tr.find('li:eq(2)').click(function() { // delete
		if ( confirmDelete(conf.id, conf.bundle_name) ) {
			$.post(pluginRoot + '/' + conf.id + '?apply=1&delete=1', null, function() {
				document.location.href = pluginRoot;
			}, 'json');
		}
	});
	if (conf.bundle) tr.find('li:eq(1)').click(function() { // unbind
		if ( confirmUnbind(conf.id, conf.bundle_name) ) {
			$.post(pluginRoot + '/' + conf.id + '?apply=1&delete=1', null, function() {
				document.location.href = pluginRoot + '/' + conf.id;
			}, 'json');
		}
	}).removeClass('ui-state-disabled');
}

function addFactoryConfig(conf) {
	var tr = factoryRow.clone().appendTo(configTable).attr('fpid', conf.name);
	//tr.find('td:eq(1)').text(conf.id); // fpid
	tr.find('td:eq(0)').text(conf.name).click(function() { // name & edit
		configure(conf.id, true);
	});
	// buttons
	tr.find('li:eq(0)').click(function() { // edit
		configure(conf.id, true);
	});
}

function treetableExtraction(node) {
	var td = $(node);
	var text = td.text();
	if (!text) return text;

	// current sort order
	var desc = $(this)[0].sortList[0][1];

	var row = td.parent();
	var fpid = row.attr('fpid');
	
	// factory row
	if ( row.hasClass('fpid') && fpid) return fpid + (desc==0?1:0) + text;

	// bundle or name row
	if ( fpid ) return fpid + desc + text;

	return mixedLinksExtraction(node);
};

$(document).ready(function() {
	configContent = $('#configContent');
	// config table list
	configTable   = $('#configTable');
	configBody    = configTable.find('tbody');
	configRow     = configBody.find('tr:eq(0)').clone();
	factoryRow    = configBody.find('tr:eq(1)').clone();

	// setup button - cannot inline in dialog option because of i18n
	var _buttons = {};
	_buttons[i18n.abort] = function() {
		$(this).dialog('close');
	}
	_buttons[i18n.reset] = function() {
		var form = document.getElementById('editorForm');
		if (form) form.reset();
	}
	_buttons[i18n.save] = function() {
		$.post(pluginRoot + '/' + $(this).attr('__pid') + '?' + $(this).find('form').serialize());
		$(this).dialog('close');
	}
	// prepare editor, but don't open yet!
	editor = $('#editor').dialog({
		autoOpen : false,
		modal    : true,
		width    : '90%',
		closeText: i18n.abort,
		buttons  : _buttons
	});
	editorMessage = editor.find('p');

	// display the configuration data
	$(".statline").html(configData.status ? i18n.stat_ok : i18n.stat_missing);
	if (configData.status) {
		configBody.empty();
		var factories = {};

		for(var i in configData.pids) {
			var c = configData.pids[i];
			if (c.fpid) {
				if (!factories[c.fpid]) factories[c.fpid] = new Array();
				factories[c.fpid].push(c);
			} else {
				addConfig(c);
			}
		}
		for(var i in configData.fpids) {
			addFactoryConfig(configData.fpids[i]);

			var confs = factories[ configData.fpids[i].id ];
			if (confs) for (var j in confs) {
				addConfig(confs[j]);
			}
		}
		initStaticWidgets(configTable);

		// init tablesorte
		configTable.tablesorter({
			headers: { 2: { sorter: false }  },
			sortList: [[0,1]],
			textExtraction: treetableExtraction
		}).bind('sortStart', function() { // clear cache, otherwse extraction will not work
			var table = $(this).trigger('update'); 
		}).find('th:eq(0)').click();
	} else {
		configContent.addClass('ui-helper-hidden');
	}
	if (selectedPid) configure(selectedPid);
});