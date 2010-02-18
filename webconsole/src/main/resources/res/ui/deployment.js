/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function fixId(id) { return id.replace('.', '_'); }

function data( /* Array of Object */ dataArray ) { // render components
	if (dataArray.length == 1) {
		entry( dataArray[0], true );
	} else {
		for ( var idx in dataArray ) {
			entry( dataArray[idx] );
		}
	}
}

function entry( /* Object */ dataEntry, /* boolean */ singleEntry ) {
	var id = fixId(dataEntry.id);
	var trElement = tr( 'ui-state-active', { 'id': 'entry' + id });

	// entry brief
	entryInternal( trElement,  dataEntry, singleEntry );
	plugin_table.append(trElement);

	// dataEntry detailed properties
	trElement = tr( 'ui-helper-hidden', { 
		'id'   : 'entry' + id + '_details'
	});
	if (dataEntry.props) {
		getDataEntryDetails( trElement, dataEntry.props );
	}
	plugin_table.append(trElement);
}

function entryInternal( /* Element */ parent, /* Object */ dataEntry, /* boolean */ singleEntry ) {
	var id = dataEntry.id;
	var _id = fixId(id);

	parent.appendChild( td( null, null, [ text( id ) ] ) );
	parent.appendChild( td( null, 
			{
				'onclick': 'toggleDetails("#entry' + _id + '")',
				'class': 'pkgname'
			}, 
			[
				createElement( 'span', 'ui-icon ui-icon-triangle-1-e', { id: 'entry' + _id + '_icon'} ),
				text(dataEntry.name)
			]
		)
	);
	parent.appendChild( td( null, null, [ text( dataEntry.state ) ] ) );

	for ( var aidx in dataEntry.actions ) {
		var action = dataEntry.actions[aidx];
		parent.appendChild( actionButton( action.enabled, id, action.link, action.name ) );
	}
}


/* Element */ function actionButton( /* boolean */ enabled, /* long */ id, /* String */ op, /* String */ opLabel ) {
	var buttonTd = td( "content", { align: "right" } );
	if ( op ) {
		switch (opLabel) {
			case "Uninstall" : opLabel = i18n.uninstall; break;
		}
		var input = createElement( "input", null, {
				type: 'button',
				value: opLabel,
				onclick: 'changeDataEntryState("' + id + '", "' + op + '");'
			});
		if (!enabled) {
			input.setAttribute( "disabled", true );
			$(input).addClass('ui-state-disabled');
		}
		buttonTd.appendChild( input );
	} else {
		addText( buttonTd, "\u00a0" );
	}
	return buttonTd;
}


function getDataEntryDetails( /* Element */ parent, /* Array of Object */ details )
{
	parent.appendChild( addText( td( "content"), "\u00a0" ) );
	
	var tdEl = td( null, { colspan: 4 } );
	parent.appendChild( tdEl );
	
	var tableEl = createElement( "table", null, { border: 0 } );
	tdEl.appendChild( tableEl );
	
	var tbody = createElement( "tbody" );
	tableEl.appendChild( tbody );
	for (var idx in details) {
		var prop = details[idx];
		var trEl = tr();
		var key = prop.key;
		switch (key) {
			case 'Package Name': key = i18n.package_name; break;
			case 'Version'     : key = i18n.version; break;
			case 'Bundles'     : key = i18n.bundles; break;
		}
		trEl.appendChild( addText( td( null, { noWrap: true } ), key ) );

		var proptd = td();
		trEl.appendChild( proptd );
		$(proptd).html( prop.value ? prop.value : "\u00a0");

		tbody.appendChild( trEl );
	}
 }


function changeDataEntryState(/* long */ id, /* String */ action) {
	var parm = pluginRoot + "/" + id + "?action=" + action;
	$.post(parm, null, function() { // always reload
		document.location.reload();
	}, "text");
}


function toggleDetails(name) {
	var icon = $(name + '_icon');
	var details = $(name + '_details');
	var show = icon.attr('show');
	if (typeof show == 'undefined') show = '';
	icon.attr('show', show ? '' : 'true');

	if (show == 'true') {
		icon.removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
		details.addClass('ui-helper-hidden');
	} else {
		icon.removeClass('ui-icon-triangle-1-e').addClass('ui-icon-triangle-1-s');
		details.removeClass('ui-helper-hidden');
	}
}

var plugin_table = false;
$(document).ready(function() {
	plugin_table = $('#plugin_table');
	var /* Array of Data Objects */ bundleData = packageListData;
	if (bundleData.error) {
		$('.statline').text(i18n.status_no_serv);
		$('#dps1').addClass('ui-helper-hidden');
	} else if (!bundleData.data || bundleData.data.length == 0) {
		$('.statline').text(i18n.status_no_data);
		$('#dps1').removeClass('ui-helper-hidden');
		$('#dps2').addClass('ui-helper-hidden');
	} else {
		data( bundleData.data );
		$('.statline').text(i18n.status_ok);
		$('#dps1').removeClass('ui-helper-hidden');
		$('#dps2').removeClass('ui-helper-hidden');
		initStaticWidgets(plugin_table);
	}
});

