<?xml version="1.0"?>
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:html="http://www.w3.org/1999/xhtml"
                version="1.0"> 
    <xsl:param name="stylesheet"/>
    <xsl:template match="/">
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="href"><xsl:value-of select="$stylesheet"/></xsl:attribute>
                </link>
                <style type="text/css">
                    @page :left {
                      @top-left {
                        content: "Apache Felix Karaf ${project.version} User's Manual";
                      }
                    }
                </style>
            </head>
            <body>
                <xsl:apply-templates select=".//html:div[@class='wiki-content']" />
            </body>
        </html>
    </xsl:template>
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
