<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>

<html>

<%@ include file="resources/jsp/commonhead.jsp" %>

<body>
	<div class="container">
		<header class="page-header">
			<h1 class="text-center">Kenney's Pickem</h1>
		</header>
		
		<div id="content">
			<div class="row">
				<div class="col-sm-3"></div>
				
				<div class="col-sm-6">
					<h2>Login</h2>
					
					<form role="form" class="clearfix">
						<div class="form-group">
							<label for="j_username">Email Address</label>
							<input type="email" name="j_username" id="j_username" class="form-control">
						</div>
						
						<div class="form-group">
							<label for="j_password">Password</label>
							<input type="password" name="j_password" id="j_password" class="form-control">
						</div>
						
						<div class="pull-right">
							<button type="submit" id="loginButton" class="btn btn-default">Login</button>
							<button type="button" id="resetButton" class="btn btn-default">Reset</button>
						</div>
					</form>
					
					<div class="margin-before text-center">
						Not Registered?  Click <a href="#register">here</a>.
					</div>
				</div>
				
				<div class="col-sm-3"></div>
			</div>
		</div>
		
		<footer>
		</footer>
	</div>
</body>
</html>