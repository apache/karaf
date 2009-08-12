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
    renderTable( "Feature Repositories", "repository_table", ["Name", "URL", "Actions"] );
    var txt = "<form method='post'><div class='table'><table id='repository_table_footer' class='tablelayout'><tbody>" +
        "<tr><input type='hidden' name='action' value='addRepository'/>" +
        "<td><input id='url' type='text' name='url' style='width:100%' colspan='2'/></td>" +
        "<td class='col_Actions'><input type='button' value='Add URL' onclick='addRepositoryUrl()'/></td>" +
        "</tr></tbody></table></div></form><br/>";
    $("#plugin_content").append( txt );
    renderTable( "Features", "feature_table", ["Name", "Version", "Repository", "Status", "Actions"] );
    renderStatusLine();
}

function addRepositoryUrl() {
    var url = document.getElementById( "url" ).value;
    changeRepositoryState( "addRepository", url );
}

function renderStatusLine() {
    $("#plugin_content").append( "<div class='fullwidth'><div class='statusline'/></div>" );
}

function renderTable( /* String */ title, /* String */ id, /* array of Strings */ columns ) {
    var txt = "<div class='table'><table class='tablelayout'><tbody><tr>" +
        "<td style='color:#6181A9;background-color:#e6eeee'>" +
        title + "</td></tr></tbody></table>" +
        "<table id='" + id + "' class='tablelayout'><thead><tr>";
    for ( var name in columns ) {
      txt = txt + "<th class='col_" + columns[name] + "' style='border-top:#e6eeee'>" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table></div>";
    $("#plugin_content").append( txt );
}

function renderData( /* Object */ data ) {
    renderStatusData( data.status );
    renderRepositoryTableData( data.repositories );
    renderFeatureTableData( data.features );
    $("#repository_table").tablesorter( {
        headers: {
            2: { sorter: false }
        },
        sortList: [[0,0]],
    } );
    $("#feature_table").tablesorter( {
        headers: {
           4: { sorter: false }
        },
        sortList: [[0,0]],
    } );
}

function renderStatusData( /* String */ status )  {
    $(".statusline").empty().append( status );
}

function renderRepositoryTableData( /* array of Objects */ repositories ) {
    var trElement;
    var input;
    var needsLegend = false;
    $("#repository_table > tbody > tr").remove();
    for ( var idx in repositories ) {
        var name = repositories[idx].name;
        trElement = tr( null, { id: "repository-" + name } );
        renderRepositoryData( trElement, repositories[idx] );
        $("#repository_table > tbody").append( trElement );
        if ( name[ name.length - 1 ] == "*" ) {
            needsLegend = true;
        }
    }
    $("#repository_table").trigger( "update" );
    if ( needsLegend ) {
        trElement = tr( null, null ) ;
        trElement.appendChild( td( null, { colspan: 3 },
                                   [ text( "* Installed via deploy directory" ) ] ) );
        $("#repository_table_footer > tbody").prepend( trElement );
    }
    $("#repository_table_footer").trigger( "update" );
}

function renderRepositoryData( /* Element */ parent, /* Object */ repository ) {
    parent.appendChild( td( null, null, [ text( repository.name ) ] ) );
    parent.appendChild( td( null, null, [ text( repository.url ) ] ) );

    var actionsTd = td( null, null );
    var div = createElement( "div", null, {
      style: { "text-align": "left"}
    } );
    actionsTd.appendChild( div );
    
    for ( var a in repository.actions ) {
      repositoryButton( div, repository.url, repository.actions[a] );
    }
    parent.appendChild( actionsTd );
}

function repositoryButton( /* Element */ parent, /* String */ url, /* Obj */ action ) {
    if ( !action.enabled ) {
        return;
    }
  
    var input = createElement( "input", null, {
        type: 'image',
        style: {"margin-left": "10px"},
        title: action.title,
        alt: action.title,
        src: imgRoot + '/bundle_' + action.image + '.png'
    } );
    $(input).click( function() {changeRepositoryState( action.op, url )} );

    if ( !action.enabled ) {
        $(input).attr( "disabled", true );
    }
    parent.appendChild( input );
}

function changeRepositoryState( /* String */ action, /* String */ url ) {
    $.post( pluginRoot, {"action": action, "url": url}, function( data ) {
        renderData( data );
    }, "json" ); 
}

function renderFeatureTableData( /* array of Objects */ features ) {
    $("#feature_table > tbody > tr").remove();
    for ( var idx in features ) {
        var trElement = tr( null, { id: "feature-" + features[idx].id } );
        renderFeatureData( trElement, features[idx] );
        $("#feature_table > tbody").append( trElement ); 
    }
    $("#feature_table").trigger( "update" );
}

function renderFeatureData( /* Element */ parent, /* Object */ feature ) {
    parent.appendChild( td( null, null, [ text( feature.name ) ] ) );
    parent.appendChild( td( null, null, [ text( feature.version ) ] ) );
    parent.appendChild( td( null, null, [ text( feature.repository ) ] ) );
    parent.appendChild( td( null, null, [ text( feature.state ) ] ) );
    var actionsTd = td( null, null );
    var div = createElement( "div", null, {
        style: { "text-align": "left"}
    } );
    actionsTd.appendChild( div );
    
    for ( var a in feature.actions ) {
        featureButton( div, feature.name, feature.version, feature.actions[a] );
    }
    parent.appendChild( actionsTd );
}

function featureButton( /* Element */ parent, /* String */ name, /* String */ version, /* Obj */ action ) {
    if ( !action.enabled ) {
        return;
    }
  
    var input = createElement( "input", null, {
        type: 'image',
        style: {"margin-left": "10px"},
        title: action.title,
        alt: action.title,
        src: imgRoot + '/bundle_' + action.image + '.png'
    } );
    $(input).click( function() {changeFeatureState( action.op, name, version )} );

    if ( !action.enabled ) {
        $(input).attr( "disabled", true );
    }
    parent.appendChild( input );
}

function changeFeatureState( /* String */ action, /* String */ feature, /* String */ version ) {
    $.post( pluginRoot, {"action": action, "feature": feature, "version": version}, function( data ) {
        renderData( data );
    }, "json" ); 
}
