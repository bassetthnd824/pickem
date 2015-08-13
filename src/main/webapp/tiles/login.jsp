<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<h2>Login</h2>
		
		<form action="j_security_check" method="POST" role="form" class="form-horizontal clearfix margin-before">
			<div class="form-group">
				<label class="col-sm-3 control-label" for="j_username">Email Address</label>
				<div class="col-sm-9 controls">
					<input type="email" name="j_username" id="username" class="form-control">
				</div>
			</div>
			
			<div class="form-group">
				<label class="col-sm-3 control-label" for="j_password">Password</label>
				<div class="col-sm-9 controls">
					<input type="password" name="j_password" id="password" class="form-control">
				</div>
			</div>
			
			<div class="pull-right">
				<button type="button" id="resetButton" class="btn btn-default">Reset</button>
				<button type="submit" id="loginButton" class="btn btn-default btn-primary">Login</button>
			</div>
		</form>
		
		<div class="margin-before text-center">
			Not Registered?  Click <a href="<c:url value="/register.action"/>">here</a>.
		</div>
	</div>
</div>