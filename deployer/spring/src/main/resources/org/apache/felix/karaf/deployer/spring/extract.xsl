<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:beans="http://www.springframework.org/schema/beans"
                xmlns:aop="http://www.springframework.org/schema/aop"
                xmlns:context="http://www.springframework.org/schema/context"
                xmlns:jee="http://www.springframework.org/schema/jee"
                xmlns:jms="http://www.springframework.org/schema/jms"
                xmlns:lang="http://www.springframework.org/schema/lang"
                xmlns:osgi-compendium="http://www.springframework.org/schema/osgi-compendium"
                xmlns:osgi="http://www.springframework.org/schema/osgi"
                xmlns:tool="http://www.springframework.org/schema/tool"
                xmlns:tx="http://www.springframework.org/schema/tx"
                xmlns:util="http://www.springframework.org/schema/util"
                xmlns:webflow-config="http://www.springframework.org/schema/webflow-config">
    
    <xsl:output method="text" />

	<xsl:template match="/">

		<!-- Match all attributes that holds a class or a comma delimited
		     list of classes and print them -->

		<xsl:for-each select="
				//beans:bean/@class
			|	//beans:*/@value-type
 			|	//aop:*/@implement-interface
			|	//aop:*/@default-impl
			|	//context:load-time-weaver/@weaver-class
			|	//jee:jndi-lookup/@expected-type
			|	//jee:jndi-lookup/@proxy-interface
			| 	//jee:remote-slsb/@ejbType
			|	//jee:*/@business-interface
			|	//lang:*/@script-interfaces
			|	//osgi:*/@interface
			|	//util:list/@list-class
			|	//util:set/@set-class
			|	//util:map/@map-class
			|	//webflow-config:*/@class
		">
			<xsl:value-of select="." />
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

		<!-- This seems some magic to get extra imports? -->

		<xsl:for-each select="//beans:bean[@class='org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean'
				or @class='org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean']">
			<xsl:for-each select="beans:property[@name='interfaces']">
				<xsl:value-of select="@value" />
				<xsl:text>
				</xsl:text>
			</xsl:for-each>
		</xsl:for-each>

	</xsl:template>


</xsl:stylesheet>

