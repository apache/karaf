<?xml version="1.0"?>
<!--
/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.    
*/
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 	version="1.0">
	<xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
	<xsl:include href="common.xsl"/>
	<xsl:include href="xalan-ext.xsl"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">emptymbean.title</xsl:param>

	<!-- Main template -->
	<xsl:template match="/">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr width="100%">
						<td>
						<xsl:call-template name="toprow"/>
						<xsl:call-template name="tabs">
							<xsl:with-param name="selection">mbean</xsl:with-param>
						</xsl:call-template>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr>
									<td class="page_title">
										<xsl:call-template name="str">
											<xsl:with-param name="id">emptymbean.title</xsl:with-param>
										</xsl:call-template>
									</td>
								</tr>
								<tr>
									<form action="/constructors">
										<td class="page_title">
										<xsl:call-template name="str">
											<xsl:with-param name="id">emptymbean.querycontructors</xsl:with-param>
										</xsl:call-template>
										<xsl:variable name="str.query">
											<xsl:call-template name="str">
												<xsl:with-param name="id">emptymbean.query</xsl:with-param>
											</xsl:call-template>
										</xsl:variable>
										<input type="input" name="classname"/><input type="submit" value="{$str.query}"/>
										</td></form></tr>
							</table>
						<xsl:call-template name="bottom"/>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

