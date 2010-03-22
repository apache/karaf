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
// device selection
var deviceData = false;
var deviceTableBody = false;

// service selection
var serviceData = false;
var serviceDataVars = false;
var serviceDataInfoID = false;
var serviceDataInfoType = false;

// actions
var actionsContainer = false;
var actionsSelect    = false;
var actionsInvoke    = false;
var actionsTable     = false;
var actionsTableBody = false;
var actionsTableRow  = false;

// tree browser, buttons, error dialog
var browser = false;
var searching = false;
var reloadVars = false;

/* BEGIN HELPERS */

/* helper functions for tree node */
function _id(dn) { return dn.replace(/[:-\\.\\$]/g,'_') }
/* creates a node in the device tree */
function treeNode(id, name, icon, span) {
	var li = createElement('li', null, { 'id' : _id(id) }, [
		createElement('span', null, null, [
			icon ? 
				createElement('img', 'icon', { 'src' : icon }) :
				createElement('span', 'ui-icon ui-icon-' + span) ,
			text(name)
		])
	]);
	return $(li);
}
/* creates a service node in the devices tree, and associates the click action */
function servNode(udn, urn) {
	return treeNode(udn+urn, urn, null, 'extlink').click(function() {
		if (selectServiceTime) {
			clearTimeout(selectServiceTime);
			selectServiceTime = false;
		}
		$.post(pluginRoot, { 
			'action': 'serviceDetails',
			'udn' : udn,
			'urn' : urn
		}, function(data) {
			renderService(udn, urn, data)
		}, 'json');
		return false;
	});
}
/* a helper function to format device properties values - specially 
 * converts arrays and strings, if the last are links */
function _val(val) {
	var ret = '';
	if ($.isArray(val)) {
		for (i in val) ret += _val(val[i]) + '<br/>';
	} else {
		ret = (typeof val == 'string' && val.indexOf('http://') != -1) ?
			'<a target="blank" href="' + val + '">' + val + '</a>' : val;
	}
	return ret;
}


/* BEGIN UI-ELEMENTS CREATION */

/* add element to the tree, just creates the node */
function addDevice(device) {
	var udn  = device.props['UPnP.device.UDN'];
	var name = device.props['UPnP.device.friendlyName'];
	var icon = null;
	if (device.icon) icon = pluginRoot + '?icon=' + udn;

	var node = treeNode(udn, name, icon, 'lightbulb').click(function() {
		renderDevice(device);
		return false;
	});

	var ul, hasChildren;

	// services
	hasChildren = false;
	ul = $(createElement('ul', 'ui-helper-clearfix'));
	for(var i in device.services) {
		hasChildren = true;
		ul.append( servNode(udn, device.services[i]) );
	}
	if (hasChildren) node.append(ul);

	// child devices
	hasChildren = false;
	ul = $(createElement('ul'));
	for(var i in device.children) {
		hasChildren = true;
		ul.append( addDevice(device.children[i]) );
	}
	if (hasChildren) node.append(ul);

	return node;
}
/* fills in the list of state variables */
function renderVars(data) {
	serviceDataVars.empty();
	for(i in data.variables) {
		var _var = data.variables[i];
		var _tr = tr(null, null, [
			td(null, null, [ text(_var.name) ]),
			td(null, null, [ text(_var.value) ]),
			td(null, null, [ text(_var.sendsEvents) ])
		]);
		serviceDataVars.append(_tr);
	}
	initStaticWidgets();
}

/* BEGIN ACTION HANDLERS */

var selectedDevice = false; // the LI element of the selected device, reset on load
function renderDevice(device) {
	// generate content
	var table = '';
	for(var key in device.props) {
		table += '<tr><td class="ui-priority-primary">' + key + '</td><td>' + _val(device.props[key]) + '</td></tr>';
	}

	// update the UI
	deviceTableBody.html(table);
	reloadVars.addClass('ui-state-disabled');
	deviceData.removeClass('ui-helper-hidden');
	serviceData.addClass('ui-helper-hidden')
	
	// reset selected items
	if (selectedDevice) selectedDevice.css('font-weight', 'normal');
	selectedDevice = $('#' + _id(device.props['UPnP.device.UDN']) + ' span').css('font-weight', 'bold');
}

var selectedUdn = false;
var selectedUrn = false;
var selectServiceTime = false;
function renderService(udn, urn, data) {
	// save selection
	selectedUdn = udn;
	selectedUrn = urn;

	// append service info
	serviceDataInfoID.text(data.id);
	serviceDataInfoType.text(data.type);

	// append state variables
	renderVars(data);

	// append actions
	if (data.actions) {
		var html = '';
		var x = data.actions;
		for (var a in x) html += '<option value="' + a + '">' + x[a].name + '</option>';
		actionsSelect.html(html).unbind('change').change(function() {
			var index = $(this).val();
			actionSelected(udn, urn, x[index]);
		}).trigger('change');
		actionsContainer.removeClass('ui-helper-hidden');
	} else {
		actionsContainer.addClass('ui-helper-hidden');
	}

	// update UI
	deviceData.addClass('ui-helper-hidden');
	serviceData.removeClass('ui-helper-hidden');
	reloadVars.removeClass('ui-state-disabled');
	initStaticWidgets();

	// refresh once - to get updates asynchronously
	selectServiceTime = setTimeout('reloadVars.click()', 3000);
}

function actionSelected(udn, urn, action) {
	// add input arguments
	if (action.inVars) {
		actionsTableBody.empty();
		for (var i in action.inVars) {
			var _arg = action.inVars[i];
			var _tr = actionsTableRow.clone().appendTo(actionsTableBody);
			_tr.find('td:eq(0)').text(_arg.name);
			_tr.find('td:eq(1)').text(_arg.type);
			var _el = _tr.find('input').attr('id', 'arg'+i);
			_arg['element'] = _el;
		}
		actionsTable.removeClass('ui-helper-hidden');
	} else {
		actionsTable.addClass('ui-helper-hidden');
	}
	
	actionsInvoke.unbind('click').click(function() {
		invokeAction(udn, urn, action);
	});

	initStaticWidgets(actionsTableBody);
}

function invokeAction(udn, urn, action) {
	// prepare arguments
	var names = new Array();
	var vals = new Array();
	for (var i in action.inVars) {
		var x = action.inVars[i];
		names.push(x['name']);
		vals.push(x['element'].val());
	}
	// invoke action
	$.post(pluginRoot, { 
		'udn' : udn,
		'urn' : urn,
		'action': 'invokeAction',
		'actionID' : action.name,
		'names' : names,
		'vals'  : vals
	}, function(data) {
		var html = i18n.no_params_out;
		if (data.output) {
			html = '<table class="nicetable"><tr><th>'+i18n.args_name+'</th><th>'+i18n.args_type+'</th><th>' + i18n.args_value + '</th></tr>';
			for(var i in data.output) {
				var arg = data.output[i];
				html += '<tr><td>' + arg['name'] + '</td><td>' + arg['type'] + '</td><td>' + arg['value'] + '</td></tr>';
			}
			html += '</table>';
		}
		Xalert(html, i18n.dl_title_ok);
	}, 'json');
}

function listDevices() {
	browser.empty().addClass('ui-helper-hidden');
	searching.removeClass('ui-helper-hidden');
	
	$.post(pluginRoot, { 'action': 'listDevices' }, function(data) {
		if (data && data.devices) {
			$.each(data.devices, function(index) {
				var html = addDevice(this);
				browser.treeview( { add: html.appendTo(browser) } );
			});
		} else {
			browser.append('','No devices available', '');
		}

		// update selected items
		selectedDevice = false;
		selectedUdn = false;
		selectedUrn = false;
	
		// update IU elements
		browser.removeClass('ui-helper-hidden');
		searching.addClass('ui-helper-hidden');
		deviceData.addClass('ui-helper-hidden');
		serviceData.addClass('ui-helper-hidden');
	}, 'json');

	return false;
}



$(document).ready( function() {
	// init elements of style
	searching          = $('#searching');
	deviceData         = $('#deviceData');
	deviceTableBody    = $('#deviceTable tbody');

	// services
	serviceData        = $('#serviceData');
	serviceDataInfoID  = $('#serviceDataInfoID');
	serviceDataInfoType= $('#serviceDataInfoType');
	serviceDataVars    = $('#serviceDataVars tbody');

	// actions
	actionsContainer   = $('#actionsContainer');
	actionsSelect      = actionsContainer.find('select');
	actionsInvoke      = actionsContainer.find('button');
	actionsTable       = actionsContainer.find('table');
	actionsTableBody   = actionsTable.find('tbody');
	actionsTableRow    = actionsTableBody.find('tr').clone();
	actionsTableBody.empty();

	// init navigation tree
	browser = $('#browser').treeview({
		animated: 'fast',
		collapsed: true,
		unique: true
	});
	
	// reload button
	reloadVars = $('#reloadVars').click(function() {
		if (selectedUdn && selectedUrn) {
			$.post(pluginRoot, { 
				'action': 'serviceDetails',
				'udn' : selectedUdn,
				'urn' : selectedUrn
			}, renderVars, 'json');
		}
	})

	$('#reloadDevices').click(listDevices);

	listDevices();
});

