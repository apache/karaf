<?xml version="1.0"?>
<!--

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
																																					-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 	version="1.0">
	<xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
	<xsl:include href="common.xsl"/>
	<xsl:include href="xalan-ext.xsl"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">about.title</xsl:param>

	<!-- Main template -->
	<xsl:template match="/MBean">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr width="100%">
						<td>
						<xsl:call-template name="toprow"/>
						<xsl:call-template name="tabs">
							<xsl:with-param name="selection">about</xsl:with-param>
						</xsl:call-template>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr class="about">
									<td colspan="2">
										<h1 align="center" class="about">
											<xsl:variable name="str.url">
												<xsl:call-template name="str">
													<xsl:with-param name="id">about.main.url</xsl:with-param>
												</xsl:call-template>
											</xsl:variable>
											<xsl:call-template name="str">
												<xsl:with-param name="id">about.main.title</xsl:with-param>
												<xsl:with-param name="p0"><a href="{$str.url}">MX4J</a></xsl:with-param>
											</xsl:call-template>
										</h1>
									</td>
								</tr>
								<tr class="about">
									<td colspan="2">
										<h2 align="center" class="about">
											<xsl:variable name="str.url">
												<xsl:call-template name="str">
													<xsl:with-param name="id">about.main.url2</xsl:with-param>
												</xsl:call-template>
											</xsl:variable>
											<xsl:call-template name="str">
												<xsl:with-param name="id">about.main.line1</xsl:with-param>
												<xsl:with-param name="p0"><a href="{$str.url}">JMX</a></xsl:with-param>
											</xsl:call-template>
										</h2>
									</td>
								</tr>
								<tr class="about">
									<td>
										<h3 align="right" class="about">
											<xsl:call-template name="str">
												<xsl:with-param name="id">about.main.implementation</xsl:with-param>
											</xsl:call-template>
										</h3>
									</td>
									<td>
										<h3 align="left" class="about">
											<xsl:value-of select="./Attribute[@name='ImplementationName']/@value"/>
										</h3>
									</td>
								</tr>
								<tr class="about">
									<td>
										<h3 align="right" class="about">
											<xsl:call-template name="str">
												<xsl:with-param name="id">about.main.implementationversion</xsl:with-param>
											</xsl:call-template>
										</h3>
									</td>
									<td>
										<h3 align="left" class="about">
											<xsl:value-of select="./Attribute[@name='ImplementationVersion']/@value"/>
										</h3>
									</td>
								</tr>
								<tr class="about">
									<td>
										<h3 align="right" class="about">
											<xsl:call-template name="str">
												<xsl:with-param name="id">about.main.serverid</xsl:with-param>
											</xsl:call-template>
										</h3>
									</td>
									<td>
										<h3 align="left" class="about">
											<xsl:value-of select="./Attribute[@name='MBeanServerId']/@value"/>
										</h3>
									</td>
								</tr>
							</table>
						<xsl:call-template name="bottom"/>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

