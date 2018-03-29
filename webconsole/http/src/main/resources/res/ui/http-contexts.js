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

function renderFeatures( data ) {
    $(document).ready( function() {
        renderView();
        renderData( data );
    } );
}

function renderView() {
    renderStatusLine();
    renderTable( "HTTP Contexts", "context_table", ["ID", "Servlet", "Name", "State", "Alias", "urls"] );
    renderTable( "Web Contexts", "webctxt_table", ["ID", "BundleState", "Web Context", "State"] );
    renderTable(" HTTP Proxies", "proxy_table", ["URL", "ProxyTo"]);
    renderStatusLine();
}    
    
function renderStatusLine() {
    $("#plugin_content").append( "<div class='fullwidth'><div class='statusline'/></div>" );
}

function renderTable( /* String */ title, /* String */ id, /* array of Strings */ columns ) {
    var txt = "<div class='ui-widget-header ui-corner-top buttonGroup'><table class='nicetable ui-widget'><thead><tr>" +
        "<th>" +
        title + "</th></tr></thead></table></div>" +
        "<table id='" + id + "' class='nicetable ui-widget'><thead><tr>";
    for ( var name in columns ) {
      txt = txt + "<th class='col_" + columns[name] + " ui-widget-header header' >" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table>";
    $("#plugin_content").append( txt );
}

function renderData( /* Object */ data ) {
    renderStatusData( data.status );
    renderContextTableData( data.contexts );
    $("#context_table").tablesorter( {
        headers: {
            2: { sorter: false }
        },
        sortList: [[0,0]]
    } );
    
    renderWebCtxtTableData( data.web );
    $("#webctxt_table").tablesorter( {
        headers: {
            2: { sorter: false }
        },
        sortList: [[0,0]]
    } );
}

function renderStatusData( /* String */ status )  {
    $(".statusline").empty().append( status );
}

function renderContextTableData( /* array of Objects */ contexts ) {
    $("#context_table > tbody > tr").remove();
    for ( var idx in contexts ) {
        var trElement = tr( null, { id: "context-" + contexts[idx].id } );
        renderContextData( trElement, contexts[idx] );
        $("#context_table > tbody").append( trElement ); 
    }
    $("#context_table").trigger( "update" );
}

function renderWebCtxtTableData( /* array of Objects */ webctxts ) {
    $("#webctxt_table > tbody > tr").remove();
    for ( var idx in webctxts ) {
        var trElement = tr( null, { id: "webctxts-" + webctxts[idx].id } );
        renderWebCtxtData( trElement, webctxts[idx] );
        $("#webctxt_table > tbody").append( trElement ); 
    }
    $("#webctxt_table").trigger( "update" );
}

function link( url, linkText ) {
  var result = createElement("a");
  result.href = url;
  result.appendChild(text(linkText));
  return result;
}

function trimUrl( url ) {
	var result = $.trim(url);
	result = result.replace(/\*/,"");
	return result;
}

function renderContextData( /* Element */ parent, /* Object */ context ) {
    parent.appendChild( td( null, null, [ text( context.id ) ] ) );
    parent.appendChild( td( null, null, [ text( context.servlet ) ] ) );
    parent.appendChild( td( null, null, [ text( context.servletName ) ] ) );
    parent.appendChild( td( null, null, [ text( context.state ) ] ) );
    parent.appendChild( td( null, null, [ text( context.alias ) ] ) );
    
    var urlBox = td( null, null );
    for ( var idx in context.urls ) {
      urlBox.appendChild( link( trimUrl(context.urls[idx]), context.urls[idx] ) );
    }
    
    parent.appendChild( urlBox );
}

function renderWebCtxtData( /* Element */ parent, /* Object */ webCtxt ) {
    parent.appendChild( td( null, null, [ text( webCtxt.id ) ] ) );
    parent.appendChild( td( null, null, [ text( webCtxt.bundleState) ] ) );
    parent.appendChild( td( null, null, [ link( trimUrl( webCtxt.contextpath ), webCtxt.contextpath) ] ) );
    parent.appendChild( td( null, null, [ text( webCtxt.state ) ] ) );
}