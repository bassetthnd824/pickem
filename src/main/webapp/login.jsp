<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>

<html>

<jsp:include page="resources/jsp/commonhead.jsp"/>

<body>
	<script type="text/javascript">
		$(document).ready(function() {
			$('#resetButton').click(function(event) {
				$('#j_username,#j_password').val('');
			});
		});
	</script>
	
	<div class="container">
		<header class="page-header">
			<h1 class="text-center">Kenney's Pickem</h1>
		</header>
		
		<div id="content">
			<div class="row">
				<div class="col-sm-4"></div>
				
				<div class="col-sm-4">
					<h2>Login</h2>
					
					<form action="j_security_check" method="POST" role="form" class="clearfix">
						<div class="form-group">
							<label for="j_username">Email Address</label>
							<input type="email" name="j_username" id="j_username" class="form-control">
						</div>
						
						<div class="form-group">
							<label for="j_password">Password</label>
							<input type="password" name="j_password" id="j_password" class="form-control">
						</div>
						
						<div class="pull-right">
							<button type="button" id="resetButton" class="btn btn-default">Reset</button>
							<button type="submit" id="loginButton" class="btn btn-default btn-primary">Login</button>
						</div>
					</form>
					
					<div class="margin-before text-center">
						Not Registered?  Click <a href="../register/index.html">here</a>.
					</div>
				</div>
				
				<div class="col-sm-4"></div>
			</div>
		</div>
		
		<footer>
		</footer>
	</div>
</body>
</html>