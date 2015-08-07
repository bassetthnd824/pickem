<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<h2>Team Schedule</h2>

<div class="row margin-before">
	<div class="col-xs-5">
		<s:form>
			<s:select name="teamId" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team" />
		</s:form>
	</div>
</div>
