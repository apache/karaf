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
	<xsl:param name="head.title">monitor.title</xsl:param>
	<xsl:include href="common.xsl"/>

	<!-- Domain template -->
	<xsl:template name="domain">
		<xsl:for-each select="Domain[MBean]">
			<tr>
				<td class="domainline">
					<div class="domainheading"><xsl:value-of select="@name"/></div>
					<table width="100%" cellpadding="0" cellspacing="0" border="0">
						<xsl:call-template name="mbean"/>
					</table>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- MBean template -->
	<xsl:template match="MBean" name="mbean">
		<xsl:for-each select="MBean">
			<tr>
				<xsl:variable name="classtype">
					<xsl:if test="(position() mod 2)=1">darkline</xsl:if>
					<xsl:if test="(position() mod 2)=0">clearline</xsl:if>
				</xsl:variable>
				<xsl:variable name="objectname">
					<xsl:call-template name="uri-encode">
						<xsl:with-param name="uri" select="@objectname"/>
					</xsl:call-template>
				</xsl:variable>
				<td class="{$classtype}">
					<a href="mbean?objectname={$objectname}"><xsl:value-of select="@objectname"/></a>
				</td>
				<td align="right" class="{$classtype}">
					<a href="delete?objectname={$objectname}">
						<xsl:call-template name="str">
							<xsl:with-param name="id">monitor.mbean.unregister</xsl:with-param>
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
					<xsl:with-param name="selection">monitor</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td colspan="7" width="100%" class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">monitor.main.title</xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
					<tr class="fronttab">
						<form action="create">
							<xsl:variable name="str.createstring.button">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.createstring.button</xsl:with-param>
								</xsl:call-template>
							</xsl:variable>
							<td width="100%" align="right">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.createstring.label</xsl:with-param>
								</xsl:call-template>
								<input type="input" name="objectname"/>
								<input type="hidden" name="template" value="monitor_create"/>
								<input type="hidden" name="class" value="javax.management.monitor.StringMonitor"/>
							</td>
							<td align="right">
								<input type="submit" style="width: 15em;" value="{$str.createstring.button}"/>
							</td>
						</form>
					</tr>
					<tr class="fronttab">
						<form action="create">
							<xsl:variable name="str.creategauge.button">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.creategauge.button</xsl:with-param>
								</xsl:call-template>
							</xsl:variable>
							<td align="right">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.creategauge.label</xsl:with-param>
								</xsl:call-template>
								<input type="input" name="objectname"/>
								<input type="hidden" name="template" value="monitor_create"/>
								<input type="hidden" name="class" value="javax.management.monitor.GaugeMonitor"/>
							</td>
							<td align="right">
								<input type="submit" style="width: 15em;" value="{$str.creategauge.button}"/>
							</td>
						</form>
					</tr>
					<tr class="fronttab">
						<form action="create">
							<xsl:variable name="str.createcounter.button">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.createcounter.button</xsl:with-param>
								</xsl:call-template>
							</xsl:variable>
							<td width="100%" align="right">
								<xsl:call-template name="str">
									<xsl:with-param name="id">monitor.main.createcounter.label</xsl:with-param>
								</xsl:call-template>
								<input type="input" name="objectname"/>
							</td>
							<td align="right">
								<input type="hidden" name="template" value="monitor_create"/>
								<input type="hidden" name="class" value="javax.management.monitor.CounterMonitor"/>
								<input type="submit" style="width: 15em;" value="{$str.createcounter.button}"/>
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

