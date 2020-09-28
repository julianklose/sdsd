/**
 * SDSD client functions for the admin page.
 * 
 * @file   Defines the angularJS admin controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.controller('adminCtrl', function ($scope, sdsd) {
	$scope.sdsd = sdsd;
	
	$scope.isLogin = false;
	$scope.createSuccess = false;
	$scope.userlist = [];
	
	
	$scope.init = function () {
		$scope.status();
	};
	
	$scope.$watch('isLogin', function(newVal) {
		if(newVal) {
			$scope.loadUserList();
		}
		else {
			$scope.userlist = [];
		}
	});

	$scope.login = function() {
		sdsd.message = null;
		sdsd.rpcCall("admin", "adminLogin", [$scope.loginPasswordInput], function(data) {
			$scope.isLogin = data.success;
		}, function() {
			$scope.loginPasswordInput = "";
		});
	};
	
	$scope.status = function() {
		sdsd.rpcCall("admin", "adminStatus", [], function(data) {
			$scope.isLogin = !!data.isLogin;
		});
	};
	
	$scope.logout = function() {
		sdsd.rpcCall("admin", "adminLogout", [], function(data) {
			$scope.isLogin = false;
		});
	};
	
	$scope.create = function() {
		if(!$scope.createUsernameInput || $scope.createUsernameInput.length == 0) {
			sdsd.message = "The username is empty.";
			return;
		}
		if(!$scope.createEmailInput || $scope.createEmailInput.length == 0) {
			sdsd.message = "No email entered.";
			return;
		}
		if(!$scope.createPasswordInput || $scope.createPasswordInput.length == 0) {
			sdsd.message = "The password is empty.";
			return;
		}
		if($scope.createPasswordInput !== $scope.createPasswordRepeatInput) {
			sdsd.message = "The passwords do not match.";
			return;
		}
		
		sdsd.message = null;
		$scope.createSuccess = false;
		sdsd.rpcCall("admin", "adminCreateUser", [{
			username: $scope.createUsernameInput,
			password: $scope.createPasswordInput,
			email: $scope.createEmailInput
		}], function(data) {
			$scope.createSuccess = data.success;
			$scope.createUsernameInput = "";
			$scope.createEmailInput = "";
			$scope.createPasswordInput = "";
			$scope.createPasswordRepeatInput = "";
			$scope.loadUserList();
		});
	};
	
	$scope.loadUserList = function (update = false) {
		if(update) $scope.userLoading = true;
		sdsd.rpcCall("admin", "adminListUsers", [update], function(data) {
			$scope.userlist = data.users;
			$scope.sorting = { num: true };
		}, function() {
			if(update) $scope.userLoading = false;
		});
	};
	
	$scope.sort = function (param, asc) {
		$scope.sorting = { [param]: asc };
		$scope.userlist.sort(function (a, b) {
			let c;
			if(a[param] == undefined) return 1;
			else if(b[param] == undefined) return -1;
			else if(a[param] < b[param]) c = -1;
			else if(a[param] > b[param]) c = 1;
			else c = 0;
			return asc ? c : -c;
		});
	}
	
	$scope.loginUser = function (username) {
		sdsd.rpcCall("admin", "adminUserLogin", [username], function(data) {
			if(data.success) {
				for(let u of $scope.userlist) {
					u.adminLogin = (u.username == username);
				}
			}
		});
	};
	
	$scope.resetPassword = function (username) {
		let password = prompt("Enter the new password");
		if(password) {
			sdsd.rpcCall("admin", "adminUserResetPassword", [username, password]);
		}
	};
	
	$scope.connectAllMqtt = function () {
		$scope.reconnecting = true;
		sdsd.rpcCall("admin", "adminConnectAllMqtt", [], null, function() {
			$scope.reconnecting = false;
		});
	};
	
	$scope.disconnectAllMqtt = function () {
		$scope.reconnecting = true;
		sdsd.rpcCall("admin", "adminDisconnectAllMqtt", [], null, function() {
			$scope.reconnecting = false;
		});
	};
	
});
