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
var eventsTable = false;

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function renderData( eventData )  {
	$('.statline').html(eventData.status); // FIXME:

	// append table view
	eventsBody.empty();
    for ( var i in eventData.data ) entry( eventData.data[i] );
	eventsTable.trigger('update').trigger('applyWidgets');

	// append timeline view
	timeline.empty();
    for ( var i in eventData.data ) entryTimeline( eventData.data[i] );
}


function entryTimeline( /* Object */ dataEntry ) {
	var txt = '<div class="event event' + dataEntry.category + '" style="width:' + dataEntry.width + '%">' +
		'<b>' + dataEntry.offset + '</b>&nbsp;<b>' + dataEntry.topic + '</b>';
	if ( dataEntry.info )  txt += '&nbsp;:&nbsp;' + dataEntry.info;
    txt += '</div>';
	timeline.prepend(txt);	
}

function entry( /* Object */ dataEntry ) {
    var properties = dataEntry.properties;

    var propE;
    if ( dataEntry.info ) {
    	propE = text(dataEntry.info);
    } else {
	    var bodyE = createElement('tbody');
	    for( var p in dataEntry.properties ) {
	    	bodyE.appendChild(tr(null, null, [ 
				td('propName', null, [text(p)]),
				td('propVal' , null, [text(dataEntry.properties[p])])
			]));
	    }
	    propE = createElement('table', 'propTable', null, [ bodyE ]);
    }

	$(tr( null, { id: 'entry' + dataEntry.id }, [
		td( null, null, [ text( printDate(dataEntry.received) ) ] ),
		td( null, null, [ text( dataEntry.topic ) ] ),
		td( null, null, [ propE ] )
	])).appendTo(eventsBody);
}

var timeline = false;
var timelineLegend = false;
$(document).ready(function(){
	eventsTable = $('#eventsTable');
	eventsBody  = eventsTable.find('tbody');
	timeline = $('#timeline');
	timelineLegend = $('#timelineLegend');

	$('#clear').click(function () {
		$.post(pluginRoot, { 'action':'clear' }, renderData, 'json');
	});
	$('#switch').click(function() {
		var timelineHidden = timeline.hasClass('ui-helper-hidden');
		if (timelineHidden) {
			$(this).text(i18n.displayList);
			timeline.removeClass('ui-helper-hidden');
			timelineLegend.removeClass('ui-helper-hidden');
			eventsTable.addClass('ui-helper-hidden');
		} else {
			$(this).text(i18n.displayTimeline);
			timeline.addClass('ui-helper-hidden');
			timelineLegend.addClass('ui-helper-hidden');
			eventsTable.removeClass('ui-helper-hidden');
		}
	});
	$('#reload').click(function() {
		$.get(pluginRoot + '/data.json', null, renderData, 'json');
	}).click();
});
