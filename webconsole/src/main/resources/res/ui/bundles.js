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

function renderBundle( /* Array of Data Objects */ bundleData )
{

    // number of actions plus 3 -- id, name and state
    var columns = bundleData.numActions + 3;
    var startLevel = bundleData.startLevel;
    
    header( columns );

    installForm( startLevel );
    
    if (bundleData.error)
    {
        error( columns, bundleData.error );
    }
    else
    {
        data ( bundleData.data );
    }

    installForm( startLevel );

    footer( columns );
}

function installForm( /* int */ startLevel )
{
    document.write( "<form method='post' enctype='multipart/form-data'>" );
    document.write( "<tr class='content'>" );
    document.write( "<td class='content'>&nbsp;</td>" );
    document.write( "<td class='content'>" );
    document.write( "<input type='hidden' name='action' value='install' />" );
    document.write( "<input class='input' type='file' name='bundlefile' size='50'>" );
    document.write( " - Start <input class='checkradio' type='checkbox' name='bundlestart' value='start'>" );
    document.write( " - Start Level <input class='input' type='input' name='bundlestartelevel' value='" + startLevel + "' size='4'>" );
    document.write( "</td>" );
    document.write( "<td class='content' align='right' colspan='5' noWrap>" );
    document.write( "<input class='submit' style='width:auto' type='submit' value='Install or Update'>" );
    document.write( "&nbsp;" );
    document.write( "<input class='submit' style='width:auto' type='button' value='Refresh Packages' onClick='changeDataEntryState(0, \"refreshPackages\");'>" );
    document.write( "</td>" );
    document.write( "</tr>" );
    document.write( "</form>" );
}
