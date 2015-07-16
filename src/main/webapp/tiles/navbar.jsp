<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<nav class="navbar navbar-default">
	<div class="navbar-header">
		<button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#navbar-items">
			<span class="icon-bar"></span>
			<span class="icon-bar"></span>
			<span class="icon-bar"></span>
		</button>
	</div>
	
	<div id="navbar-items" class="collapse navbar-collapse">
		<ul class="nav navbar-nav">
			<li><a href="<c:url value="/game/main.action"/>">Home</a></li>
			<li><a href="<c:url value="/game/leaderBoard.action"/>">Leader Board</a></li>
			<li><a href="<c:url value="/game/teamSchedule.action"/>">Team Schedule</a></li>
			<li><a href="<c:url value="/game/account.action"/>">My Account</a></li>
			<c:if test="${isManager}">
				<li class="dropdown">
					<a class="dropdown-toggle" data-toggle="dropdown" href="#">Admin<span class="caret"></span></a>
					<ul class="dropdown-menu">
						<li><a href="<c:url value="/manager/users_init.action"/>">Users</a></li>
						<li><a href="<c:url value="/manager/themes_init.action"/>">Themes</a></li>
						<li><a href="<c:url value="/manager/teams_init.action"/>">Teams</a></li>
						<li><a href="<c:url value="/manager/venues_init.action"/>">Venues</a></li>
						<li><a href="<c:url value="/manager/rivalries_init.action"/>">Rivalries</a></li>
						<li><a href="<c:url value="/manager/seasons_init.action"/>">Seasons</a></li>
						<li><a href="<c:url value="/manager/seasonWeeks_init.action"/>">Season Weeks</a></li>
						<li><a href="<c:url value="/manager/matchups_init.action"/>">Matchups</a></li>
					</ul>
				</li>
			</c:if>
			<li><a href="<c:url value="/logout.action"/>">Logout</a></li>
		</ul>
	</div>
</nav>