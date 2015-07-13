<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="row">
	<div class="col-sm-4 col-sm-offset-4">
		<h2>Login</h2>
		
		<form action="j_security_check" method="POST" role="form" class="clearfix">
			<div class="form-group">
				<label for="j_username">Email Address</label>
				<input type="email" name="j_username" id="username" class="form-control">
			</div>
			
			<div class="form-group">
				<label for="j_password">Password</label>
				<input type="password" name="j_password" id="password" class="form-control">
			</div>
			
			<div class="pull-right">
				<button type="button" id="resetButton" class="btn btn-default">Reset</button>
				<button type="submit" id="loginButton" class="btn btn-default btn-primary">Login</button>
			</div>
		</form>
		
		<div class="margin-before text-center">
			Not Registered?  Click <a href="<c:url value="/register_init"/>">here</a>.
		</div>
	</div>
</div>