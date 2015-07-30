<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<h2><s:property value="welcomeMessage"/></h2>

<s:form>
	<input type="hidden" id="numberOfConferenceTeams" value="${numberOfConferenceTeams}"/>
	
	<c:forEach items="${seasonWeeks }" var="seasonWeek">
		<h3>Week ${seasonWeek.weekNumber}</h3>
		
		<div class="weekly-matchups">
			<c:set var="rank" value="${numberOfConferenceTeams}"/>
			<c:set var="matchupsList" value="${matchupUserPicks[seasonWeek.id]}"/>
			<c:set var="i" value="0"/>
			
			<c:forEach items="${matchupsList}" var="matchup">
				<div class="weekly-matchup row">
					<input type="hidden" name="model[${i}].matchup.id" value="${matchup.matchupId}"/>
					<input type="hidden" name="model[${i}].id" value="${matchup.userPickId}"/>
				
					<div class="col-xs-12 well well-sm">
						<div class="row">
							<div class="image-column col-xs-1">
								<c:choose>
									<c:when test="${matchup.pickedTeamId != null && matchup.winningTeamId != -1 && matchup.winningTeamId == matchup.pickedTeamId}">
										<img alt="" src="<c:url value="/resources/img/check-24-ns.png" />"/>
									</c:when>
									
									<c:when test="${matchup.pickedTeamId != null && matchup.winningTeamId != -1 && matchup.winningTeamId != matchup.pickedTeamId}">
										<img alt="" src="<c:url value="/resources/img/cross-24-ns.png" />"/>
									</c:when>
								</c:choose>
							</div>
							
							<div class="col-xs-10">
								<div class="row">
									<div class="matchup-date hidden-xs hidden-sm col-md-1"><fmt:formatDate value="${matchup.matchupDate}" pattern="yyyy-MM-dd"/></div>
									<div class="rivalry-name hidden-xs hidden-sm col-md-2"></div>
									
									<div class="away-team-name col-sm-2">
										<label class="radio-inline">
											<input type="radio" name="model[${i}].pickedTeam.id" value="${matchup.awayTeamId}" ${(matchup.pickedTeamId != null && matchup.awayTeamId == matchup.pickedTeamId) ? ' checked="checked"' : '' }/>
											${matchup.awayTeamName}
											<span class="hidden-xs hidden-sm">${matchup.awaySquadName}</span>
										</label>
									</div>
									
									<div class="col-sm-2">
										<div class="row">
											<div class="team-score col-xs-4">${matchup.awayTeamScore}</div>
											<div class="matchup-rank col-xs-4"><span>${(matchup.rank != null) ? matchup.rank : rank}</span><input type="hidden" name="model[${i}].rank" value="${(matchup.rank != null) ? matchup.rank : rank}"/></div>
											<div class="team-score col-xs-4">${matchup.homeTeamScore}</div>
										</div>
									</div>
									
									<div class="home-team-name col-sm-2">
										<label class="radio-inline">
											<input type="radio" name="model[${i}].pickedTeam.id" value="${matchup.homeTeamId}" ${(matchup.pickedTeamId != null && matchup.homeTeamId == matchup.pickedTeamId) ? ' checked="checked"' : '' }/>
											${matchup.homeTeamName}
											<span class="hidden-xs hidden-sm">${matchup.homeSquadName}</span>
										</label>
									</div>
									
									<div class="venue-name hidden-xs hidden-sm col-md-3">${matchup.venueName}<br/>${matchup.cityState}</div>
								</div>
							</div>
							
							<div class="sort-buttons col-xs-1">
								<div><button type="button" class="up-button btn btn-xs btn-default btn-block"><span class="glyphicon glyphicon-triangle-top"></span></button></div>
								<div><button type="button" class="down-button btn btn-xs btn-default btn-block"><span class="glyphicon glyphicon-triangle-bottom"></span></button></div>
							</div>
						</div>
					</div>
				</div>
				
				<c:set var="rank" value="${rank - 1}"/>
				<c:set var="i" value="${i + 1}"/>
			</c:forEach>
		</div>
	</c:forEach>
	
	<div class="button-container pull-right">
		<s:submit type="button" action="main" cssClass="btn btn-default">Cancel</s:submit>
		<s:submit type="button" action="main_save" cssClass="btn btn-default btn-primary">Save</s:submit>
	</div>
</s:form>