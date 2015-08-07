<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<!DOCTYPE html>

<html>
<head>
	<title><tiles:getAsString name="title"/></title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<meta http-equiv="Cache-Control" content="must-revalidate" />
	<meta http-equiv="Pragma" content="no-cache" />
	<meta http-equiv="max-age" content="3600" />
	<meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=0"/>
	<link rel="stylesheet" href="<c:url value="/resources/css/bootstrap-min-3.3.4.css"/>" type="text/css" media="all"/>
	<link rel="stylesheet" href="<c:url value="/resources/css/bootstrap-theme-min-3.3.4.css"/>" type="text/css" media="all"/>
	<link rel="stylesheet" href="<c:url value="/resources/css/bootstrap-datetimepicker-min-4.0.0.css"/>" type="text/css" media="all"/>
	<link rel="stylesheet" href="<c:url value="/resources/css/pickem.css"/>" type="text/css" media="all"/>
	<link href="http://fonts.googleapis.com/css?family=Rokkitt" rel="stylesheet" type="text/css">
</head>

<body>
	<div class="container">
		<header class="page-header">
			<tiles:insertAttribute name="header"/>
		</header>
		
		<tiles:insertAttribute name="navbar"/>
		
		<div id="content">
			<tiles:insertAttribute name="body"/>
		</div>
		
		<footer>
			<tiles:insertAttribute name="footer"/>
		</footer>
	</div>
	
	<script type="text/javascript" src="<c:url value="/resources/js/libs/modernizr-min-2.8.3.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/libs/jquery-min-2.1.3.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/libs/moment-min-2.10.3.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/libs/bootstrap-min-3.3.4.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/libs/bootstrap-datetimepicker-min-4.0.0.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/libs/underscore-min-1.8.3.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/js/app/underscore-setup.js"/>"></script>
	<tiles:insertAttribute name="scripts"/>
</body>
</html>
