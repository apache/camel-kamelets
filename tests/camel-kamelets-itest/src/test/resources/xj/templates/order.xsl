<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xj="http://camel.apache.org/component/xj" exclude-result-prefixes="xj">
  <xsl:output method="xml" omit-xml-declaration="yes"/>
  <xsl:template match="/"><order><customer><xsl:value-of select="//object[@xj:name='name']"/></customer></order></xsl:template>
</xsl:stylesheet>
