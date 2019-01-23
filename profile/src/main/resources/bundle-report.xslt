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
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous" />
                <style type="text/css">
                    body { position: relative }
                    li.active a { background-color: #eee }
                    div.bundle { padding: 5px; margin: 5px 0 }
                    #n { padding: 5px 15px }
                    #n a { padding: 5px 15px; margin: 0 5px }
                </style>
            </head>
            <body data-spy="scroll" data-target="#n" data-offset="5">
                <div class="container-fluid" style="position: fixed; z-index: 500; background-color: white; width: 100%; top: 0">
                    <div class="row">
                        <div class="col-lg-12">
                            <h3><xsl:value-of select="/k:consistency-report/@project" /><xsl:value-of select="' '" /><xsl:value-of select="/k:consistency-report/@version" /> consistency report</h3>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-lg-12">
                            <div class="container-fluid">
                                <ul class="nav nav-tabs" role="tablist">
                                    <li id="installed" class="active">
                                        <a role="tab" data-toggle="tab" href="#r1" data-t1="#t1b,#t1c,#t3,#t4" data-t2="#t1a,#t2">Installed features only</a>
                                    </li>
                                    <li>
                                        <a role="tab" data-toggle="tab" href="#r2" data-t1="#t1a,#t1c,#t2,#t4" data-t2="#t1b,#t3">Available features</a>
                                    </li>
                                    <li>
                                        <a role="tab" data-toggle="tab" href="#r3" data-t1="#t1a,#t1b,#t2,#t3" data-t2="#t1c,#t4">All features (including blacklisted)</a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-lg-12">
                            <div class="collapse navbar-collapse" id="n">
                                <ul class="nav navbar-nav">
                                    <li class="nav-item active" id="t1a">
                                        <a class="nav-link" href="#d0">
                                            Bundle <em>Duplicates</em> (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='installed']/k:duplicates/k:duplicate)" />)
                                        </a>
                                    </li>
                                    <li class="nav-item" id="t1b" style="display: none">
                                        <a class="nav-link" href="#d0">
                                            Bundle <em>Duplicates</em> (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='available']/k:duplicates/k:duplicate)" />)
                                        </a>
                                    </li>
                                    <li class="nav-item" id="t1c" style="display: none">
                                        <a class="nav-link" href="#d0">
                                            Bundle <em>Duplicates</em> (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='all']/k:duplicates/k:duplicate)" />)
                                        </a>
                                    </li>
                                    <li class="nav-item" id="t2">
                                        <a class="nav-link" href="#d1">
                                            All bundles (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='installed']/k:bundles/k:bundle)" />)
                                        </a>
                                    </li>
                                    <li class="nav-item" id="t3" style="display: none">
                                        <a class="nav-link" href="#d2">
                                            All bundles (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='available']/k:bundles/k:bundle)" />)
                                        </a>
                                    </li>
                                    <li class="nav-item" id="t4" style="display: none">
                                        <a class="nav-link" href="#d3">
                                            All bundles (<xsl:value-of select="count(/k:consistency-report/k:report[@flavor='all']/k:bundles/k:bundle)" />)
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="d0" style="height: 150px">a</div>

                <div id="report" class="container-fluid" style="z-index: -1">
                    <div class="row">
                        <div class="tab-content col-lg-12">
                            <div id="r1" class="tab-pane active">
                                <div class="container-fluid">
                                    <h4>All features that are actually installed in <code>etc/org.apache.karaf.features.cfg</code>.</h4>
                                    <div>
                                        <h1>Bundle <em>Duplicates</em></h1>
                                        <div class="help">(A <em>duplicate bundle</em> is a bundle that is referenced multiple times
                                            with the same Maven <code>groupId</code> and <code>artifactId</code> but with different versions.
                                            For each bundle that is used with different version, there's a list of all these versions and
                                            features (and their repositories) which include them.)</div>
                                        <xsl:apply-templates select="/k:consistency-report/k:report[@flavor='installed']/k:duplicates" />
                                    </div>
                                    <div style="position: relative; margin-top: -150px">
                                        <div id="d1" style="position: relative; top: 0; height: 150px"></div>
                                        <div>
                                            <h1>All bundles</h1>
                                            <xsl:call-template name="bundles">
                                                <xsl:with-param name="f" select="'installed'" />
                                            </xsl:call-template>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="r2" class="tab-pane">
                                <div class="container-fluid">
                                    <h4>All non-blacklisted features availalable to install, referenced from non-blacklisted repositories.</h4>
                                    <div>
                                        <h1>Bundle <em>Duplicates</em></h1>
                                        <div class="help">(A <em>duplicate bundle</em> is a bundle that is referenced multiple times
                                            with the same Maven <code>groupId</code> and <code>artifactId</code> but with different versions.
                                            For each bundle that is used with different version, there's a list of all these versions and
                                            features (and their repositories) which include them.)</div>
                                        <xsl:apply-templates select="/k:consistency-report/k:report[@flavor='available']/k:duplicates" />
                                    </div>
                                    <div style="position: relative; margin-top: -150px">
                                        <div id="d2" style="position: relative; top: 0; height: 150px"></div>
                                        <div>
                                            <h1>All bundles</h1>
                                            <xsl:call-template name="bundles">
                                                <xsl:with-param name="f" select="'available'" />
                                            </xsl:call-template>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="r3" class="tab-pane">
                                <div class="container-fluid">
                                    <h4>All features, including blacklisted ones.</h4>
                                    <div>
                                        <h1>Bundle <em>Duplicates</em></h1>
                                        <div class="help">(A <em>duplicate bundle</em> is a bundle that is referenced multiple times
                                            with the same Maven <code>groupId</code> and <code>artifactId</code> but with different versions.
                                            For each bundle that is used with different version, there's a list of all these versions and
                                            features (and their repositories) which include them.)</div>
                                        <xsl:apply-templates select="/k:consistency-report/k:report[@flavor='all']/k:duplicates" />
                                    </div>
                                    <div style="position: relative; margin-top: -150px">
                                        <div id="d3" style="position: relative; top: 0; height: 150px"></div>
                                        <div>
                                            <h1>All bundles</h1>
                                            <xsl:call-template name="bundles">
                                                <xsl:with-param name="f" select="'all'" />
                                            </xsl:call-template>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
                <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
                <script>
                    $(function () {
                        $("#installed").addClass("active");
                        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                            $(window).scrollTop(0);
                            $($(e.target).data("t1")).hide();
                            $($(e.target).data("t2")).show();
                            $("body").scrollspy("refresh");
                        })
                    });
                </script>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="k:duplicate">
        <div class="bundle">
            <strong class="text-danger"><xsl:value-of select="@ga" /></strong>
            <ul class="feature" style="display: block">
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
        <xsl:param name="f" />
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
                <xsl:for-each select="/k:consistency-report/k:report[@flavor=$f]/k:bundles/k:bundle">
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
