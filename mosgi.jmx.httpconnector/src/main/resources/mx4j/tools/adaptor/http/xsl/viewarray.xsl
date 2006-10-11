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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html" indent="yes" encoding="UTF-8"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">viewarray.title</xsl:param>
	<xsl:param name="request.step">30</xsl:param>
	<xsl:param name="request.start">0</xsl:param>
	<xsl:param name="request.objectname"/>
	<xsl:param name="request.attribute"/>
	<xsl:param name="request.format"/>
	<xsl:param name="request.template"/>

	<xsl:include href="common.xsl"/>
	<xsl:include href="mbean_attributes.xsl"/>

	<!-- Main template -->
	<xsl:template match="/" name="main">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">mbean</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td colspan="2" class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">viewarray.main.title</xsl:with-param>
								<xsl:with-param name="p0"><xsl:value-of select="MBean/@objectname"/></xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
					<tr>
						<td colspan="2" class="fronttab">
							<xsl:call-template name="str">
								<xsl:with-param name="id">viewarray.main.arraytitle</xsl:with-param>
								<xsl:with-param name="p0"><xsl:value-of select="MBean/Attribute/@attribute"/></xsl:with-param>
								<xsl:with-param name="p1"><xsl:value-of select="substring-before(substring-after(MBean/Attribute/@classname, '[L'), ';')"/></xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
					<tr>
						<td class="fronttab">
							<div class="tableheader">
								<xsl:call-template name="str">
									<xsl:with-param name="id">viewarray.main.index</xsl:with-param>
								</xsl:call-template>
							</div>
						</td>
						<td class="fronttab">
							<div class="tableheader">
								<xsl:call-template name="str">
									<xsl:with-param name="id">viewarray.main.value</xsl:with-param>
								</xsl:call-template>
							</div>
						</td>
					</tr>
					<xsl:for-each select="MBean/Attribute/Array/Element">
						<xsl:sort order="ascending" select="@index"/>
						<xsl:variable name="classtype">
							<xsl:if test="(position() mod 2)=1">clearline</xsl:if>
							<xsl:if test="(position() mod 2)=0">darkline</xsl:if>
						</xsl:variable>
						<tr class="{$classtype}">
							<td class="aggregationrow"><xsl:value-of select="@index"/></td>
							<td class="aggregationrow">
								<xsl:call-template name="renderobject">
									<xsl:with-param name="objectclass" select="../@componentclass"/>
									<xsl:with-param name="objectvalue" select="@element"/>
								</xsl:call-template>
							</td>
						</tr>
					</xsl:for-each>

					<xsl:variable name="url">getattribute?objectname=<xsl:call-template name="uri-encode"><xsl:with-param name="uri"><xsl:value-of select="$request.objectname"/></xsl:with-param></xsl:call-template>&amp;attribute=<xsl:value-of select="$request.attribute"/>&amp;format=array&amp;template=viewarray&amp;locale=<xsl:value-of select="$request.locale"/></xsl:variable>
					<xsl:call-template name="aggregation-navigation">
						<xsl:with-param name="url" select="$url"/>
						<xsl:with-param name="total" select="count(MBean/Attribute/Array/Element)"/>
						<xsl:with-param name="start" select="$request.start"/>
						<xsl:with-param name="step" select="$request.step"/>
						<xsl:with-param name="str.prefix">viewarray.main</xsl:with-param>
					</xsl:call-template>

					<xsl:call-template name="mbeanview">
						<xsl:with-param name="objectname" select="MBean/@objectname"/>
						<xsl:with-param name="colspan" select="4"/>
					</xsl:call-template>

				</table>

				<xsl:call-template name="bottom"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

