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

function header( /* int */ columns )
{
    document.write( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "<tr class='content'>" );
    document.write( "<th class='content'>Topic</th>" );
    document.write( "<th class='content'>Properties</th>" );
    document.write( "</tr>" );

}


function error( /* int */ columns, /* String */ message )
{
    document.write( "<tr class='content'>" );
    document.write( "<td class='content'>&nbsp;</td>" );
    document.write( "<td class='content' colspan='" + (columns - 1) + "'>" + message + "</td>" );
    document.write( "</tr>" );
}


function data( /* Array of Object */ dataArray )
{
    // render components
    if (dataArray.length == 1)
    {
        entry( dataArray[0], true );
    }
    else {
        for ( var idx in dataArray )
        {
            entry( dataArray[idx] );
        }
    }
}


function footer( /* int */ columns )
{
    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "</table>" );
}


function entry( /* Object */ dataEntry, /* boolean */ singleEntry )
{
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry, singleEntry );
    document.write( serialize( trElement ) );
}


function entryInternal( /* Element */ parent, /* Object */ dataEntry, /* boolean */ singleEntry )
{

    var id = dataEntry.id;
    var topic = dataEntry.topic;
    var properties = dataEntry.properties;

    parent.appendChild( td( "content", { width: "20%"}, [ text( topic ) ] ) );

    var tableE = createElement("table");
    var bodyE = createElement("tbody");
    tableE.appendChild(bodyE);

    for( var p in dataEntry.properties ) {
    	bodyE.appendChild(tr(null, null, [td(null, null, [text(p)] ),
    	                                  td(null, null, [text(dataEntry.properties[p])])]));
    }
    
    parent.appendChild( td( "content", null, [tableE] ) );
}



function renderEvents( /* Array of Data Objects */ bundleData )
{

    // topic and properties
    var columns = 2;
    
    header( columns );

    if (bundleData.error)
    {
        error( columns, bundleData.error );
    }
    else
    {
        data ( bundleData.data );
    }

    footer( columns );
}
