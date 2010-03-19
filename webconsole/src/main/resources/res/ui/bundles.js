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
// ui elements
var uploadDialog = false;
var bundlesTable    = false;
var bundlesBody     = false;
var bundlesTemplate = false;

function renderData( eventData, filter )  {
	lastBundleData = eventData;
	var s = eventData.s;
    $('.statline').html(i18n.statline.msgFormat(s[0], s[1], s[2], s[3], s[4]));
	bundlesBody.empty();
    for ( var idx in eventData.data ) {
        if ( currentBundle == null || !drawDetails || currentBundle == eventData.data[idx].id) {
            entry( eventData.data[idx], filter );
        }
    }
    if ( drawDetails && eventData.data.length == 1 ) {
		$('.filterBox input, .filterBox button').addClass('ui-state-disabled');
        renderDetails(eventData.data[0]);    
    } else if ( currentBundle != null ) {
        var id = currentBundle;
        hideDetails(id);
        showDetails(id);
    }
    initStaticWidgets();
}

function entry( /* Object */ bundle, filter ) {
	var matches = !(filter && typeof filter.test == 'function') ? true :
		filter.test(bundle.id) || filter.test(bundle.name) || filter.test(bundle.symbolicName) || filter.test(bundle.version) || filter.test(bundle.category);

	if (matches) entryInternal( bundle ).appendTo(bundlesBody);
}

function hasStart(b) { return (!b.fragment) && (b.stateRaw == 2 || b.stateRaw == 4) } // !isFragment && (installed | resolved)
function hasStop(b)  { return (!b.fragment) && (b.stateRaw == 32) } // !isFragment && active
function hasUninstall(b)  { return b.stateRaw == 2 || b.stateRaw == 4 || b.stateRaw == 32 } // installed | resolved | active
function stateString(b) {
	var s = b.stateRaw;
	return  b.fragment && s == 4 ? 
		i18n.state.fragment : // fragment & resolved
		i18n.state[s] ? i18n.state[s] : i18n.state.unknown.msgFormat(s)
}

function entryInternal( /* Object */ bundle ) {
	var tr = bundlesTemplate.clone();
    var id = bundle.id;
    var name = bundle.name + '<span class="symName">' + bundle.symbolicName + '</span>';

	tr.attr('id', 'entry'+id);
	tr.find('td:eq(0)').text(id);
	tr.find('td:eq(1) span:eq(0)').attr('id', 'img'+id).click(function() {showDetails(id)});
	tr.find('td:eq(1) span:eq(1)').html( drawDetails ? name : '<a href="' + pluginRoot + '/' + id + '">' + name + '</a>' );
	tr.find('td:eq(2)').text( bundle.version );
	tr.find('td:eq(3)').text( bundle.category );
	tr.find('td:eq(4)').text( stateString(bundle) );
	if (id == 0) { // system bundle has no actions
		tr.find('td:eq(5) ul').addClass('ui-helper-hidden');
	} else {
		var start   = tr.find('td:eq(5) ul li:eq(0)');
		var stop    = tr.find('td:eq(5) ul li:eq(1)');
		var refresh = tr.find('td:eq(5) ul li:eq(2)').click(function() {changeDataEntryState(id, 'refresh')});
		var update  = tr.find('td:eq(5) ul li:eq(3)').click(function() {changeDataEntryState(id, 'update')});
		var remove  = tr.find('td:eq(5) ul li:eq(4)');
		start = hasStart(bundle) ?
			start.click(function() {changeDataEntryState(id, 'start')}) :
			start.addClass('ui-helper-hidden');
		stop = hasStop(bundle) ?
			stop.click(function() {changeDataEntryState(id, 'stop')}) :
			stop.addClass('ui-helper-hidden');
		remove = hasUninstall(bundle) ?
			remove.click(function() {changeDataEntryState(id, 'uninstall')}) :
			remove.addClass('ui-helper-hidden');
	}
	return tr;
}

function loadData() {
    $.get(pluginRoot + "/.json", null, renderData, "json"); 
}

function changeDataEntryState(/* long */ id, /* String */ action) {
    $.post(pluginRoot + "/" + id, {"action":action}, renderData, "json"); 
}

function refreshPackages() {
    $.post(pluginRoot, {"action": "refreshPackages"}, renderData, "json"); 
}

function showDetails( id ) {
    currentBundle = id;
    $.get(pluginRoot + "/" + id + ".json", null, function(data) {
        renderDetails(data.data[0]);
    }, "json");
}

function hideDetails( id ) {
    currentBundle = null;
    $("#img" + id).each(function() {
        $("#pluginInlineDetails" + id).remove();
        $(this).
            removeClass('ui-icon-triangle-1-w').//left
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e').//right
            attr("title", "Details").
            unbind('click').click(function() {showDetails(id)});
    });
}

function renderDetails( data ) {
    $("#entry" + data.id + " > td").eq(1).append("<div id='pluginInlineDetails"  + data.id + "'/>");
    $("#img" + data.id).each(function() {
        if ( drawDetails ) {
            var ref = window.location.pathname;
            ref = ref.substring(0, ref.lastIndexOf('/'));
            $(this).
                removeClass('ui-icon-triangle-1-e').//right
                removeClass('ui-icon-triangle-1-s').//down
                addClass('ui-icon-triangle-1-w').//left
                attr("title", "Back").
                unbind('click').click(function() {window.location = ref});
        } else {
            $(this).
                removeClass('ui-icon-triangle-1-w').//left
                removeClass('ui-icon-triangle-1-e').//right
                addClass('ui-icon-triangle-1-s').//down
                attr("title", "Hide Details").
                unbind('click').click(function() {hideDetails(data.id)});
        }
    });
    $("#pluginInlineDetails" + data.id).append("<table border='0'><tbody></tbody></table>");
    var details = data.props;
    for (var idx in details) {
        var prop = details[idx];
		var key = i18n[prop.key] ? i18n[prop.key] : prop.key;

        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + key + "</td><td class='aligntop' style='border:0px none'>";          
        if (prop.value) {
            if ( prop.key == 'Bundle Documentation' )  {
                txt = txt + "<a href='" + prop.value + "' target='_blank'>" + prop.value + "</a>";
            } else  {
                if ( $.isArray(prop.value) ) {
                    var i = 0;
                    for(var pi in prop.value) {
                        var value = prop.value[pi];
                        if (i > 0) { txt = txt + "<br/>"; }
                        txt = txt + value;
                        i++;
                    }
                } else {
                    txt = txt + prop.value;
                }
            }
        } else {
            txt = txt + "\u00a0";
        }
        txt = txt + "</td></tr>";
        $("#pluginInlineDetails" + data.id + " > table > tbody").append(txt);
    }
}


$(document).ready(function(){
	$('.refreshPackages').click(refreshPackages);
	$('.reloadButton').click(loadData);
	$('.installButton').click(function() {
		uploadDialog.dialog('open');
		return false;
	});

	// filter
	$('.filterApply').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		var el = $(this).parent().find('input.filter');
		var filter = el.length && el.val() ? new RegExp(el.val()) : false;
		renderData(lastBundleData, filter);
	});
	$('.filterForm').submit(function() {
		$(this).find('.filterApply').click();
		return false;
	});
	$('.filterClear').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		$('input.filter').val('');
		loadData();
	});
	$('.filterLDAP').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		var el = $(this).parent().find('input.filter');
		var filter = el.val();
		if (filter) $.get(pluginRoot + '/.json', { 'filter' : filter }, renderData, 'json');
	});

	// upload dialog
	var uploadDialogButtons = {};
	uploadDialogButtons[i18n.install_update] = function() {
		$(this).find('form').submit();
	}
	uploadDialog = $('#uploadDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '50%',
		buttons : uploadDialogButtons
	});

	// check for cookie
	var cv = $.cookies.get("webconsolebundlelist");
	var lo = (cv ? cv.split(",") : [1,0]);
	bundlesTable = $("#plugin_table").tablesorter({
		headers: {
			0: { sorter:"digit" },
			5: { sorter: false }
		},
		textExtraction:mixedLinksExtraction,
		sortList: cv ? [lo] : false
	}).bind("sortEnd", function() {
		bundlesTable.eq(0).attr("config");
		$.cookies.set("webconsolebundlelist", bundlesTable.sortList.toString());
	});
	bundlesBody     = bundlesTable.find('tbody');
	bundlesTemplate = bundlesBody.find('tr').clone();

	renderData(lastBundleData);
});

