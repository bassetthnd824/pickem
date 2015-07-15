<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<h2><tiles:getAsString name="screenHeader"/></h2>

<div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
	<div class="panel panel-default">
		<div class="panel-heading" role="tab" id="searchFormHeading">
			<h4 class="panel-title">
				<a role="button" data-toggle="collapse" data-parent="#accordion" href="#searchFormPanel" aria-expanded="<s:if test='modelList.size == 0'>true</s:if><s:else>false</s:else>" aria-controls="searchFormPanel">Search Criteria</a>
			</h4>
		</div>
		
		<div id="searchFormPanel" class="panel-collapse collapse <s:if test="modelList.size == 0">in</s:if>" role="tabpanel" aria-labelledby="searchFormHeading">
			<div class="panel-body">
				<tiles:insertAttribute name="searchForm"/>
			</div>
		</div>
	</div>
	
	<s:if test="modelList.size > 0">
		<div class="panel panel-default">
			<div class="panel-heading" role="tab" id="searchResultsHeading">
				<h4 class="panel-title">
					<a class="collapsed" role="button" data-toggle="collapse" data-parent="#accordion" href="#searchResultsPanel" aria-expanded="true" aria-controls="searchResultsPanel">Search Results</a>
				</h4>
			</div>
			
			<div id="searchResultsPanel" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="searchResultsHeading">
				<div class="panel-body">
					<tiles:insertAttribute name="searchResults"/>
				</div>
			</div>
		</div>
	</s:if>
</div>