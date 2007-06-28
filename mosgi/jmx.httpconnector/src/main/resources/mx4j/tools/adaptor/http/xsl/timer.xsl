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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:xalan="http://xml.apache.org/xslt">
	<xsl:output method="html" indent="yes" encoding="UTF-8" xalan:indent-amount="4"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">timer.title</xsl:param>
	<xsl:include href="common.xsl"/>

	<xsl:template name="domain">
		<xsl:for-each select="Domain[MBean]">
			<tr>
				<td class="serverbydomain_domainline">
					<xsl:call-template name="str">
						<xsl:with-param name="id">timer.domain.title</xsl:with-param>
						<xsl:with-param name="p0">
							<xsl:value-of select="@name"/>
						</xsl:with-param>
					</xsl:call-template>
				</td>
			</tr>
			<tr>
				<td>
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">timer.domain.objectname</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
			</tr>
			<tr>
				<td>
					<table width="100%" cellpadding="0" cellspacing="0" border="0">
						<xsl:call-template name="mbean"/>
					</table>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="MBean" name="mbean">
		<xsl:for-each select="MBean">
			<xsl:variable name="classtype">
				<xsl:if test="(position() mod 2)=1">darkline</xsl:if>
				<xsl:if test="(position() mod 2)=0">clearline</xsl:if>
			</xsl:variable>
			<tr class="{$classtype}">
				<xsl:variable name="objectname">
					<xsl:call-template name="uri-encode">
						<xsl:with-param name="uri" select="@objectname"/>
					</xsl:call-template>
				</xsl:variable>
				<td class="mbean_row">
					<a href="mbean?objectname={$objectname}"><xsl:value-of select="@objectname"/></a>
				</td>
				<td align="right" class="mbean_row">
					<a href="delete?objectname={$objectname}">
						<xsl:call-template name="str">
							<xsl:with-param name="id">timer.mbean.unregister</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- Main template -->
	<xsl:template match="Server" name="main">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">timer</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td width="100%" class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">timer.main.title</xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
					<tr class="fronttab">
						<form action="create">
							<td align="right">
								<xsl:call-template name="str">
									<xsl:with-param name="id">timer.main.createlabel</xsl:with-param>
								</xsl:call-template>
								<xsl:variable name="str.createbutton">
									<xsl:call-template name="str">
										<xsl:with-param name="id">timer.main.createbutton</xsl:with-param>
									</xsl:call-template>
								</xsl:variable>
								<input type="input" name="objectname"/>
								<input type="hidden" name="template" value="timer_create"/>
								<input type="hidden" name="class" value="javax.management.timer.Timer"/>
								<input type="submit" value="{$str.createbutton}"/>
							</td>
							</form>
					</tr>
				</table>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<xsl:call-template name="domain"/>
				</table>
				<xsl:call-template name="bottom"/>
			</body>
	</html>
</xsl:template>
</xsl:stylesheet>

