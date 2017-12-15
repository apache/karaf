<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:k="urn:apache:karaf:consistency:1.0">

    <!--

        Licensed to the Apache Software Foundation (ASF) under one or more
        contributor license agreements.  See the NOTICE file distributed with
        this work for additional information regarding copyright ownership.
        The ASF licenses this file to You under the Apache License, Version 2.0
        (the "License"); you may not use this file except in compliance with
        the License.  You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
    -->

    <xsl:output doctype-public="html" encoding="UTF-8" method="html" />

    <xsl:template match="/">
        <html>
            <head>
                <meta charset="utf-8" />
                <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous" />
                <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous" />

                <style type="text/css">
                    body { position: relative }
                    h1 { margin: 2em 0 0.5em 0 }
                    div.bundle { padding: 5px; margin: 5px 0 }
                </style>
                <script src="https://code.jquery.com/jquery-3.2.1.min.js"></script>
                <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
                <script>
                    $(function() {
                        var hidden = true;
                        $("#toggle").click(function(ev) {
                            if (hidden) {
                                $(".feature").show();
                                hidden = false;
                                $("#toggle").prop('value', 'Hide details');
                            } else {
                                $(".feature").hide();
                                hidden = true;
                                $("#toggle").prop('value', 'Show details');
                            }
                        });
                    });
                </script>
            </head>
            <body data-spy="scroll" data-target="#n">
                <div id="n">
                    <nav id="nav" class="navbar navbar-inverse navbar-fixed-top" role="navigation">
                        <div class="container">
                            <div class="navbar-collapse collapse">
                                <ul class="nav navbar-nav" role="tablist">
                                    <li class="active">
                                        <a href="#duplicates">Bundle <em>Duplicates</em> (<xsl:value-of select="count(/k:consistency-report/k:duplicates/k:duplicate)" />)</a>
                                    </li>
                                    <li>
                                        <a href="#bundles">All bundles (<xsl:value-of select="count(/k:consistency-report/k:bundles/k:bundle)" />)</a>
                                    </li>
                                    <li style="min-height: 50px">
                                        <input id="toggle" class="btn btn-default" style="margin-top: 8px" type="button" value="Show details" />
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </nav>
                </div>

                <div class="container-fluid">
                    <a id="duplicates" />
                    <h1>Bundle <em>Duplicates</em></h1>
                    <div class="help">(A <em>duplicate bundle</em> is a bundle that is referenced multiple times
                        with the same Maven <code>groupId</code> and <code>artifactId</code> but with different versions.
                        For each bundle that is used with different version, there's a list of all these versions and
                        features (and their repositories) which include them.)</div>
                    <xsl:apply-templates select="/k:consistency-report/k:duplicates" />
                    <a id="bundles" />
                    <h1>All bundles</h1>
                    <xsl:call-template name="bundles" />
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="k:duplicate">
        <div class="bundle">
            <strong class="text-danger"><xsl:value-of select="@ga" /></strong>
            <ul class="feature" style="display: none">
                <xsl:for-each select="k:bundle">
                    <li>
                        <strong class="text-primary"><xsl:value-of select="@uri" /></strong>
                        <ul>
                            <xsl:for-each select="k:feature">
                                <li><xsl:value-of select="text()" /> <span class="text-muted"> (<xsl:value-of select="@repository" />)</span></li>
                            </xsl:for-each>
                        </ul>
                    </li>
                </xsl:for-each>
            </ul>
        </div>
    </xsl:template>

    <xsl:template name="bundles">
        <table class="table table-condensed" style="table-layout: fixed">
            <col width="30%" />
            <col width="70%" />
            <thead>
                <tr>
                    <th>bundle</th>
                    <th>feature(s)</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="/k:consistency-report/k:bundles/k:bundle">
                    <xsl:element name="tr">
                        <xsl:attribute name="class">
                            <xsl:if test="@duplicate='true'">danger</xsl:if>
                        </xsl:attribute>
                        <xsl:element name="td">
                            <span class="text-primary"><xsl:value-of select="@uri" /></span>
                        </xsl:element>
                        <xsl:element name="td">
                            <xsl:for-each select="k:feature">
                                <div><xsl:value-of select="text()" /></div>
                            </xsl:for-each>
                        </xsl:element>
                    </xsl:element>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template name="package">
        <xsl:param name="bundles-title" select="'bundles'" />
        <div class="bundle">
            <p class="bg-primary">
                <xsl:value-of select="@package" />
            </p>
            <div class="text-info" style="padding: 2px 10px">
                <xsl:value-of select="$bundles-title" />
            </div>
            <xsl:for-each select="k:version">
                <div class="version">
                    <div class="version-id">
                        <xsl:value-of select="@version" />
                    </div>
                    <ul>
                        <xsl:for-each select="k:by-bundle">
                            <li><xsl:value-of select="@symbolic-name" />:<xsl:value-of select="@version" />
                            </li>
                        </xsl:for-each>
                    </ul>
                </div>
            </xsl:for-each>
        </div>
    </xsl:template>

    <xsl:template name="import-export">
        <tr>
            <td>
                <xsl:value-of select="@package" />
            </td>
            <td>
                <xsl:value-of select="@version" />
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
