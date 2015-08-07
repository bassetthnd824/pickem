<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<h2>Leader Board</h2>

<c:set var="userRank" value="1"/>

<table class="table table-hover">
	<tr>
		<th>Rank</th>
		<th class="user-screenname">User</th>
		<th>Score</th>
	</tr>
	
	<s:iterator value="userScores">
		<tr>
			<td class="text-right">${userRank}</td>
			<td class="user-screenname"><s:property value="user.nickName"/></td>
			<td class="text-right"><s:property value="score"/></td>
		</tr>
		
		<c:set var="userRank" value="${userRank + 1}"/>
	</s:iterator>
</table>

