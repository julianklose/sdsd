/**
 * SDSD client functions for the register page.
 * 
 * @file   Defines the angularJS reg controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.controller('regCtrl', function ($scope, sdsd) {
	$scope.sdsd = sdsd;

	$scope.init = function () {

	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {

		} else {

		}
	});

	$scope.reg = function() {
		if(!$scope.usernameInput || $scope.usernameInput.length == 0) {
			sdsd.message = "The username is empty.";
			return;
		}
		if(!$scope.emailInput || $scope.emailInput.length == 0) {
			sdsd.message = "No email entered.";
			return;
		}
		if(!$scope.passwordInput || $scope.passwordInput.length == 0) {
			sdsd.message = "The password is empty.";
			return;
		}
		if($scope.passwordInput !== $scope.passwordRepeatInput) {
			sdsd.message = "The passwords do not match.";
			return;
		}
		
		sdsd.message = null;
		sdsd.rpcCall("user", "reg", [{
				
			username: $scope.usernameInput,
			password: $scope.passwordInput,
			email: $scope.emailInput
				
		}], function(data) {
			$scope.success = data.success;
			$scope.usernameInput = "";
			$scope.emailInput = "";
			$scope.passwordInput = "";
			$scope.passwordRepeatInput = "";
		});
	};
	
	$scope.changePassword = function() {
		if(!$scope.passwordInput || $scope.passwordInput.length == 0) {
			sdsd.message = "The password is empty.";
			return;
		}
		if($scope.passwordInput !== $scope.passwordRepeatInput) {
			sdsd.message = "The passwords do not match.";
			return;
		}
	
		sdsd.message = null;
		sdsd.rpcCall("user", "changePassword", [ 
			$scope.passwordOld, 
			$scope.passwordInput
		], function(data){
			$scope.success = data.success;
		});
	};
});