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
	<xsl:param name="head.title">mbean.title</xsl:param>
	<xsl:include href="common.xsl"/>
	<xsl:include href="mbean_attributes.xsl"/>

	<xsl:param name="request.objectname"/>

	<xsl:template name="parameters" match="Parameter">
		<xsl:param name="class"/>
		<xsl:for-each select="Parameter">
			<xsl:sort data-type="text" order="ascending" select="@id"/>
			<xsl:variable name="type.id" select="concat('type', position()-1)"/>
			<xsl:variable name="name.id" select="concat('value', position()-1)"/>
			<xsl:variable name="type" select="@type"/>
				<tr class="{$class}">
				<td width="4%" align="left" class="mbean_row">
					<div align="left"><xsl:value-of select="@id"/></div>
				</td>
				<td width="18%" align="left" class="mbean_row">
					<xsl:value-of select="@name"/>
				</td>
				<td width="18%" align="left" class="mbean_row">
					<xsl:value-of select="@description"/>
				</td>
				<td width="45%" align="left" class="mbean_row">
					<xsl:value-of select="@type"/>
				</td>
				<td align="right" width="15%" class="mbean_row">
					<xsl:choose>
						<xsl:when test="@type='java.lang.String'
							or @type='java.lang.String'
							or @type='java.lang.Double'
							or @type='java.lang.Short'
							or @type='java.lang.Integer'
							or @type='java.lang.Long'
							or @type='java.lang.Float'
							or @type='java.lang.Byte'
							or @type='java.lang.Boolean'
							or @type='java.lang.Number'
							or @type='java.lang.Character'
							or @type='javax.management.ObjectName'
							or @type='int'
							or @type='short'
							or @type='boolean'
							or @type='byte'
							or @type='double'
							or @type='long'
							or @type='char'
							or @type='float'">
							<xsl:attribute name="valid">true</xsl:attribute>
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
							<xsl:attribute name="valid">false</xsl:attribute>
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.parameters.unknown</xsl:with-param>
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
					<input type="hidden" name="{$type.id}" value="{$type}"/>
				</td>
				</tr>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="operations">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td colspan="7" width="100%" class="mbeans">
					<xsl:call-template name="str">
						<xsl:with-param name="id">mbean.operations.title</xsl:with-param>
					</xsl:call-template>
				</td>
			</tr>
		</table>
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr class="darkline">
				<td width="20%">
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">mbean.operations.name</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
				<td width="20%">
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">mbean.operations.returntype</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
				<td width="*">
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">mbean.operations.description</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
				<td/>
			</tr>
			<xsl:variable name="invoke.str">
				<xsl:call-template name="str">
					<xsl:with-param name="id">mbean.operations.invoke</xsl:with-param>
				</xsl:call-template>
			</xsl:variable>
			<xsl:for-each select="Operation">
				<xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
				<xsl:variable name="classtype">
					<xsl:if test="(position() mod 2)=1">clearline</xsl:if>
					<xsl:if test="(position() mod 2)=0">darkline</xsl:if>
				</xsl:variable>
				<xsl:variable name="hasParameters">
					<xsl:if test="count(./Parameter)>0">true</xsl:if>
					<xsl:if test="count(./Parameter)=0">false</xsl:if>
				</xsl:variable>


				<tr class="{$classtype}">
					<form action="invoke">
					<input name="operation" type="hidden" value="{$name}"/>
					<input type="hidden" name="objectname" value="{$request.objectname}"/>
					<td width="20%" align="left" class="mbean_row">
						<xsl:value-of select="@name"/>
					</td>
					<td align="left" class="mbean_row">
						<xsl:value-of select="@return"/>
					</td>
					<td align="left" class="mbean_row">
						<xsl:value-of select="@description"/>
					</td>
					<xsl:if test="$hasParameters='false'">
						<td align="center" class="mbean_row">
							<input type="submit" value="{$invoke.str}"/>
						</td>
					</xsl:if>
					<xsl:if test="$hasParameters='true'">
						<td/>
						<tr class="{$classtype}">
						<td valign="top" align="left" width="20%" class="mbean_row">
							<strong>
								<xsl:call-template name="str">
									<xsl:with-param name="id">mbean.operations.parameter</xsl:with-param>
								</xsl:call-template>
							</strong>
						</td>
						<td colspan="2">
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr class="{$classtype}">
									<td width="4%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.operations.parameterid</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td width="18%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.operations.parametername</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td width="50%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.operations.parameterdescription</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.operations.parameterclass</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
								</tr>
								<xsl:call-template name="parameters">
									<xsl:with-param name="class" select="$classtype"/>
								</xsl:call-template>
							</table>
							<td align="center" valign="bottom">
								<input style="pad-right: 1em;" type="submit" value="{$invoke.str}"/>
							</td>
						</td>
					</tr>
					</xsl:if>
					</form>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>

	<xsl:template name="constructors">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<xsl:for-each select="Constructor">
			<xsl:if test="(position())=1">
				<tr class="darkline">
					<td>
						<div class="tableheader">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.constructors.class</xsl:with-param>
							</xsl:call-template>
						</div>
					</td>
					<td>
						<div class="tableheader">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.constructors.description</xsl:with-param>
							</xsl:call-template>
						</div>
					</td>

				</tr>
			</xsl:if>
				<form action="create">
					<xsl:variable name="classtype">
						<xsl:if test="(position() mod 2)=1">clearline</xsl:if>
						<xsl:if test="(position() mod 2)=0">darkline</xsl:if>
					</xsl:variable>
					<xsl:variable name="classname"><xsl:value-of select="../@classname"/></xsl:variable>
					<tr class="{$classtype}">
						<td class="mbean_row">
							<xsl:value-of select="$classname"/>
						</td>
						<td class="mbean_row">
							<xsl:value-of select="@description"/>
						</td>
					</tr>
					<tr class="{$classtype}">
						<td valign="top" align="left" width="20%" class="mbean_row">
							<strong>
								<xsl:call-template name="str">
									<xsl:with-param name="id">mbean.constructors.parameter</xsl:with-param>
								</xsl:call-template>
							</strong>
						</td>
						<td>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr>
									<td width="4%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.constructors.parameterid</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td width="18%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.constructors.parametername</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td width="50%" class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.constructors.parameterdescription</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
									<td class="mbean_row">
										<strong>
											<xsl:call-template name="str">
												<xsl:with-param name="id">mbean.constructors.parameterclass</xsl:with-param>
											</xsl:call-template>
										</strong>
									</td>
								</tr>
								<xsl:call-template name="parameters">
									<!--<xsl:with-param name="class" select="$classtype"/>-->
								</xsl:call-template>
							</table>
							</td>
						</tr>
					<tr class="{$classtype}">
						<td class="mbean_row" align="right" colspan="2">
							<strong>
								<xsl:call-template name="str">
									<xsl:with-param name="id">mbean.constructors.objectname</xsl:with-param>
								</xsl:call-template>
							</strong>
							 <input name="objectname" value=""/>
						</td>
					</tr>
					<tr class="{$classtype}">
						<td class="mbean_row" colspan="2">
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr class="$class">
									<td align="right" colspan="4"/>
									<td align="right" class="mbean_row">
									<xsl:variable name="create.str">
										<xsl:call-template name="str">
											<xsl:with-param name="id">mbean.constructors.create</xsl:with-param>
										</xsl:call-template>
									</xsl:variable>
									<input type="submit" value="{$create.str}"/>
									<input type="hidden" name="class" value="{$classname}"/>
									</td>
								</tr>
							</table>
						</td>
					</tr>
					</form>
			</xsl:for-each>
		</table>
	</xsl:template>

	<!-- Main processing template -->
	<xsl:template match="MBean">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">mbean</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.mbean.title</xsl:with-param>
								<xsl:with-param name="p0"><xsl:value-of select="@objectname"/></xsl:with-param>
							</xsl:call-template>

						<xsl:if test="not (@description='')">
							<br/>
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.mbean.description</xsl:with-param>
								<xsl:with-param name="p0"><xsl:value-of select="@description"/></xsl:with-param>
							</xsl:call-template>
						</xsl:if>
						</td>
					</tr>
					<tr>
						<td width="100%" class="mbeans">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.mbean.attributes</xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
				</table>
				<xsl:call-template name="attribute"/>
				<xsl:call-template name="operations"/>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td colspan="7" width="100%" class="mbeans">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mbean.mbean.constructors</xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
				</table>
				<xsl:call-template name="constructors"/>
				<xsl:call-template name="bottom"/>
			</body>
	</html>
</xsl:template>
</xsl:stylesheet>

