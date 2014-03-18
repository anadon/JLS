<?xml version='1.0' encoding='ISO-8859-1' ?>

<helpset version="1.0">
  <!-- title -->
  <title>JLS Help</title>

  <!-- maps -->
  <maps>
     <homeID>top</homeID>
     <mapref location="Map.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>>Table of Contents</label>
    <type>javax.help.TOCView</type>
    <data>JLSHelpTOC.xml</data>
  </view>

<view>
	<name>Search</name>
	<label>Search</label>
	<type>javax.help.SearchView</type>
	<data engine="com.sun.java.help.search.DefaultSearchEngine">
		JavaHelpSearch</data>
</view>

</helpset>
