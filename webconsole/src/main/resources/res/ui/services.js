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
var tableEntryTemplate = false;
var tableBody = false;

function renderData(eventData) {
	$('.statline').empty().append(i18n.statline.msgFormat(eventData.serviceCount));
	$('#plugin_table > tbody > tr').remove();
	for ( var idx in eventData.data) {
		entry(eventData.data[idx]);
	}
	if (drawDetails) {
		renderDetails(eventData);
	}
}

function entry( /* Object */dataEntry) {
	var id = dataEntry.id;
	var name = dataEntry.id;

	var _ = tableEntryTemplate.clone().attr('id', 'entry' + id).appendTo(tableBody);
	_.find('.bIcon').attr('id', 'img' + id).click(function() {
		showDetails(id);
	}).after(drawDetails ? name : ('<a href="' + window.location.pathname + '/' + id + '">' + name + '</a>'));
	  
	_.find('td:eq(1)').text(dataEntry.types);
	_.find('td:eq(2)').html('<a href="' + bundlePath + dataEntry.bundleId + '">' + dataEntry.bundleSymbolicName + ' (' + dataEntry.bundleId + ')</a>' );
}

function showDetails(id) {
	$.get(pluginRoot + '/' + id + '.json', null, function(data) {
		renderDetails(data);
	}, 'json');
}

function hideDetails(id) {
	$('#img' + id).each(function() {
		$('#pluginInlineDetails' + id).remove();
		$(this).
			removeClass('ui-icon-triangle-1-w').//left
			removeClass('ui-icon-triangle-1-s').//down
			addClass('ui-icon-triangle-1-e').//right
			unbind('click').click(function() {showDetails(id)});
	});
}

function renderDetails(data) {
	data = data.data[0];
	$('#entry' + data.id + ' > td').eq(1).append('<div id="pluginInlineDetails' + data.id + '"/>');
	$('#img' + data.id).each(function() {
		if (drawDetails) {
			var ref = window.location.pathname;
			ref = ref.substring(0, ref.lastIndexOf('/'));
			$(this).
				removeClass('ui-icon-triangle-1-e').//right
				removeClass('ui-icon-triangle-1-s').//down
				addClass('ui-icon-triangle-1-w').//left
				attr('title', i18n.back).
				unbind('click').click(function() {window.location = ref;});
		} else {
			$(this).
				removeClass('ui-icon-triangle-1-w').//left
				removeClass('ui-icon-triangle-1-e').//right
				addClass('ui-icon-triangle-1-s').//down
				attr('title', i18n.detailsHide).
				unbind('click').click(function() {hideDetails(data.id)});
		}
	});
	var details = "";
	if (data.props) {
		details += renderObjectAsTable(data.props);
	}
	if (data.usingBundles) {
	   details += renderUsingBundlesAsTable(data.usingBundles);
	}
	if (details) {
        details = '<table border="0"><tbody>' + details + '</tbody></table>';
	    $('#pluginInlineDetails' + data.id).append( details );
    }
}

function renderObjectAsTable(/* Object*/ details) {
	var txt = '';

	for (var idx in details) {
		var prop = details[idx];

		txt += '<tr><td class="aligntop" noWrap="true" style="border:0px none">'
				+ prop.key
				+ '</td><td class="aligntop" style="border:0px none">';
		if (prop.value) {
			if ($.isArray(prop.value)) {
				var i = 0;
				for ( var pi in prop.value) {
					var value = prop.value[pi];
					if (i > 0) {
						txt = txt + '<br/>';
					}
					txt = txt + value;
					i++;
				}
			} else {
				txt = txt + prop.value;
			}
		} else {
			txt = txt + '\u00a0';
		}
		txt = txt + '</td></tr>';
	}

	return txt;
}

function renderUsingBundlesAsTable(/* Object[] */ bundles) {
	var txt = '';

	for (var idx in bundles) {
		var bundle = bundles[idx];
		txt += '<a href="' + bundlePath + '/' + bundle.bundleId + '">'
		    + bundle.bundleSymbolicName + ' (' + bundle.bundleId + ')' 
		    + '</a><br/>';
	}

    if (txt) {
        txt = '<tr><td class="aligntop" noWrap="true" style="border:0px none">'
            + i18n.usingBundles
            + '</td><td class="aligntop" style="border:0px none">'
            + txt
            + '</td></tr>';
    }

	return txt;
}


$(document).ready(function() {
	tableBody = $('#plugin_table tbody');
	tableEntryTemplate = tableBody.find('tr').clone();
	tableBody.empty();

	renderData(data);

	$('#plugin_table').tablesorter( {
		headers : {
			0 : { sorter : 'digit' }
		},
		sortList : [ [ 1, 0 ] ],
		textExtraction : mixedLinksExtraction
	});
});
