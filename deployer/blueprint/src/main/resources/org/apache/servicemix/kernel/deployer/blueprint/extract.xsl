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
                xmlns:bp="http://www.osgi.org/xmlns/blueprint/v1.0.0">

    <xsl:output method="text" />

	<xsl:template match="/">

		<!-- Match all attributes that holds a class or a comma delimited
		     list of classes and print them -->

		<xsl:for-each select="
				//bp:*/@class
			|	//bp:*/@type
			|	//bp:*/@value-type
			|	//bp:*/@key-type
			|	//bp:*/bp:interfaces/bp:value/text()
		">
			<xsl:value-of select="." />
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

		<!-- This seems some magic to get extra imports? -->

	</xsl:template>


</xsl:stylesheet>

