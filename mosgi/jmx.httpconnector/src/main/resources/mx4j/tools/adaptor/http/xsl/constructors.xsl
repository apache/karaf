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
	<xsl:param name="head.title">constructors.title</xsl:param>
	<xsl:include href="common.xsl"/>
	<xsl:include href="mbean_attributes.xsl"/>

	<xsl:param name="request.objectname"/>

	<!-- Constructor's parameters tempalte -->
	<xsl:template name="parameters" match="Parameter">
		<xsl:param name="class"/>

		<tr>
			<td colspan="3">
				<p class="darklinebottom">
				<xsl:call-template name="str">
					<xsl:with-param name="id">constructors.parameters.title</xsl:with-param>
				</xsl:call-template>
				</p>
			</td>
		</tr>
		<tr class="darkline">
			<td width="3%">
				<div class="tableheader">
					<xsl:call-template name="str">
						<xsl:with-param name="id">constructors.parameters.id</xsl:with-param>
					</xsl:call-template>
				</div>
			</td>
			<td>
				<div class="tableheader">
					<xsl:call-template name="str">
						<xsl:with-param name="id">constructors.parameters.type</xsl:with-param>
					</xsl:call-template>
				</div>
			</td>
			<td align="right">
				<div class="tableheader">
					<xsl:call-template name="str">
						<xsl:with-param name="id">constructors.parameters.value</xsl:with-param>
					</xsl:call-template>
				</div>
			</td>
		</tr>
		<xsl:for-each select="Parameter">
			<xsl:sort data-type="text" order="ascending" select="@id"/>
			<xsl:variable name="type.id" select="concat('type', position()-1)"/>
			<xsl:variable name="name.id" select="concat('value', position()-1)"/>
			<xsl:variable name="type" select="@type"/>
			<tr class="{$class}">
				<td align="left" class="constructorrow">
					<div align="left"><xsl:value-of select="@id"/></div>
				</td>
				<td align="left" class="constructorrow">
					<xsl:value-of select="@type"/>
				</td>
				<td align="right" width="15%" class="constructorrow">
					<xsl:choose>
						<xsl:when test="@type='java.lang.String'
							or @type='java.lang.Double'
							or @type='java.lang.Short'
							or @type='java.lang.Integer'
							or @type='java.lang.Long'
							or @type='java.lang.Float'
							or @type='java.lang.Byte'
							or @type='java.lang.Character'
							or @type='java.lang.Boolean'
							or @type='java.lang.Number'
							or @type='javax.management.ObjectName'
							or @type='int'
							or @type='long'
							or @type='short'
							or @type='boolean'
							or @type='byte'
							or @type='char'
							or @type='double'
							or @type='float'">
							<xsl:attribute name="valid">
								true
							</xsl:attribute>
						<xsl:call-template name="raw-input">
							<xsl:with-param name="name" select="$name.id"/>
							<xsl:with-param name="type" select="$type"/>
							<xsl:with-param name="value"/>
							<xsl:with-param name="strinit">false</xsl:with-param>
						</xsl:call-template>
						</xsl:when>
						<xsl:when test="@strinit='true'">
							<xsl:attribute name="valid">true</xsl:attribute>
							<xsl:call-template name="raw-input">
								<xsl:with-param name="name" select="$name.id"/>
								<xsl:with-param name="type" select="$type"/>
								<xsl:with-param name="value"/>
								<xsl:with-param name="strinit">true</xsl:with-param>
							</xsl:call-template>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="valid">
								false
							</xsl:attribute>
							<xsl:call-template name="str">
								<xsl:with-param name="id">constructors.parameters.unknowntype</xsl:with-param>
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
					<input type="hidden" name="{$type.id}" value="{$type}"/>
				</td>
				</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- Template for a Constructor node -->
	<xsl:template match="Constructor" name="constructors">
		<xsl:for-each select="//Constructor">
			<form action="create">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">

					<xsl:variable name="classtype">
						<xsl:if test="(position() mod 2)=1">clearline</xsl:if>
						<xsl:if test="(position() mod 2)=0">darkline</xsl:if>
					</xsl:variable>
					<xsl:variable name="hasParameters">
						<xsl:if test="count(Parameter)>0">true</xsl:if>
						<xsl:if test="count(Parameter)=0">false</xsl:if>
					</xsl:variable>
					<xsl:variable name="classname"><xsl:value-of select="@name"/></xsl:variable>
				<tr class="{$classtype}">
					<td valign="top" class="constructorrow">
						<xsl:call-template name="str">
							<xsl:with-param name="id">constructors.constructors.intro</xsl:with-param>
							<xsl:with-param name="p0"><xsl:value-of select="@name"/></xsl:with-param>
						</xsl:call-template>
					</td>
				</tr>
				<tr class="{$classtype}">
					<td>
						<table cellpadding="0" width="100%" cellspacing="0" border="0">
							<xsl:if test="$hasParameters='true'">
								<xsl:call-template name="parameters"/>
							</xsl:if>
							<xsl:if test="$hasParameters='false'">
								<tr>
									<td width="100%" colspan="3"></td>
								</tr>
							</xsl:if>
						</table>
					</td>
				</tr>
				<tr class="{$classtype}">
					<td valign="bottom" align="right" class="constructorrow">
						<xsl:call-template name="str">
							<xsl:with-param name="id">constructors.constructors.objectnameinput</xsl:with-param>
						</xsl:call-template>
						<input type="input" name="objectname"/>
					</td>
				</tr>
				<tr class="{$classtype}">
					<td align="right" class="constructorrow">
						<xsl:variable name="str.createnew">
							<xsl:call-template name="str">
								<xsl:with-param name="id">constructors.constructors.createnew</xsl:with-param>
							</xsl:call-template>
						</xsl:variable>

						<input type="submit" value="{$str.createnew}"/>
						<input type="hidden" name="class" value="{$classname}"/>
					</td>
				</tr>

			</table>
			</form>
		</xsl:for-each>
	</xsl:template>

	<!-- Exception handling template -->
	<xsl:template match="Exception" name="error">
		<xsl:for-each select="Exception">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td class="fronttab">
						<xsl:call-template name="str">
							<xsl:with-param name="id">constructors.error.exception</xsl:with-param>
							<xsl:with-param name="p0"><xsl:value-of select="@errorMsg"/></xsl:with-param>
						</xsl:call-template>
					</td>
				</tr>
			</table>
		</xsl:for-each>
	</xsl:template>

	<!-- Main template -->
	<xsl:template match="/" name="main">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">mbean</xsl:with-param>
				</xsl:call-template>
				<xsl:for-each select="Class|Exception">
					<table width="100%" cellpadding="0" cellspacing="0" border="0">
						<tr>
							<td width="100%" class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">constructors.main.title</xsl:with-param>
								<xsl:with-param name="p0"><xsl:value-of select="@classname"/></xsl:with-param>
							</xsl:call-template>
							</td>
						</tr>
					</table>
				</xsl:for-each>
				<xsl:call-template name="error"/>
				<xsl:call-template name="constructors"/>
				<xsl:call-template name="bottom"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

