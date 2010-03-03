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
 
var repoTable = false;
var repoTableTemplate = false;
var addRepoUri = false;
var resTable = false;
var searchField = false;
var ifStatusOK = false;

/* displays a date in the user's local timezone */
function localTm(time) {
	return (time ? new Date(time) : new Date()).toLocaleString();
}

function doRepoAction(action, url) {
	if ( !url ) {
		Xalert('Invalid URI: ' + url, 'Error');
	} else {
		$.post(pluginRoot, {
			'action' : action,
			'url'    : url
		}, renderData, 'json');
	}
}

function renderResource(res) {
	// aply filtering
	var match = searchField.val();
	if (match) {
		match = new RegExp( match );
		if ( !match.test(res.presentationname) ) return;
	}
	
	// proceed with resource
	var _id = res.symbolicname.replace(/\./g, '_');
	var _tr = resTable.find('#' + _id);

	if (_tr.length == 0) { // not created yet, create it
		var _select = createElement('select', null, { name : 'bundle' }, [
			createElement( 'option', null, { value : '-' }, [
				text( i18n.selectVersion)
			]),
			createElement( 'option', null, { value : res.id }, [
				text( res.version + (res.installed ? ' *' : '') )
			])
		]);
		_tr = tr( null, { 'id' : _id } , [
			td( null, null, [ _select ] ),
			td( null, null, [ text(res.presentationname) ] ),
			td( null, null, [ text(res.installed ? res.version : '') ] )
		]);
		resTable.append( _tr );
	} else { // append the additional version
		_tr.find( 'select' ).append (
			createElement( 'option', null, { value : res.id }, [
				text( res.version  + (res.installed ? ' *' : '') )
			])
		);
		if (res.installed) _tr.find( 'td:eq(2)' ).text( res.version );
	}
}

function renderRepository(repo) {
	var _tr = repoTableTemplate.clone();
	_tr.find('td:eq(0)').text( repo.name );
	_tr.find('td:eq(1)').text( repo.url );
	_tr.find('td:eq(2)').text( localTm(repo.lastModified) );
	_tr.find('li:eq(0)').click(function() {
		doRepoAction('refresh', repo.url);
	});
	_tr.find('li:eq(1)').click(function() {
		doRepoAction('delete', repo.url);
	});
	repoTable.append(_tr);
	
	for(var i in repo.resources) {
		renderResource( repo.resources[i] );
	}
}

function renderData(data) {
	obrData = data;
	repoTable.empty();
	resTable.empty();
	if ( data.status ) {
		$('.statline').html(i18n.status_ok);
		ifStatusOK.removeClass('ui-helper-hidden');
		for (var i in data.repositories ) {
			renderRepository( data.repositories[i] );
		}
	} else {
		$('.statline').html(i18n.status_no);
		ifStatusOK.addClass('ui-helper-hidden');
	}
}

$(document).ready( function() {
	repoTable = $('#repoTable tbody');
	repoTableTemplate = repoTable.find('tr').clone();
	addRepoUri = $('#addRepoUri');
	resTable = $('#resTable tbody').empty();
	searchField = $('#searchField');
	ifStatusOK = $('#ifStatusOK');

	$('#addRepoBtn').click(function() {
		doRepoAction('add', addRepoUri.val());
	});
	$('#searchBtn').click(function() {
		renderData(obrData);
		return false;
	});

	renderData(obrData);
});