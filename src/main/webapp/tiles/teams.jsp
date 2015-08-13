<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-12">
		<s:if test="modelList.size > 0">
			<table class="table table-hover">
				<tr>
					<th>Name</th>
					<th>Squad Name</th>
					<th>Conference Member</th>
					<th>Home Venue</th>
					<th>Last Updated On</th>
					<th>Last Updated By</th>
					<th>Actions</th>
				</tr>
				
				<s:iterator value="modelList">
					<s:url action="teams_edit" var="editUrl">
						<s:param name="model.id" value="id"/>
					</s:url>
					<tr>
						<td><s:property value="teamName"/></td>
						<td><s:property value="squadName"/></td>
						<td><s:property value="(conferenceMember) ? 'Yes' : 'No'"/></td>
						<td><s:property value="homeVenue.venueName"/></td>
						<td><s:date name="lastUpdateDate" format="yyyy-MM-dd hh:mm:ss a"/></td>
						<td><s:property value="lastUpdateUser"/></td>
						<td>
							<a href="<s:property value="#editUrl"/>" class="btn btn-small btn-default edit"><span class="glyphicon glyphicon-pencil"></span></a>
						</td>
					</tr>
				</s:iterator>
			</table>
		</s:if>
		<s:else>
			<p>No results found</p>
		</s:else>
		
		<div>
			<a href="<s:url action="teams_add"/>" class="btn btn-small btn-primary add"><span class="glyphicon glyphicon-plus"></span></a>
		</div>
	</div>
</div>