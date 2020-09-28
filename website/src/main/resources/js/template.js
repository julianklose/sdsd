/**
 * Template for SDSD client functions.
 * 
 * @file   Defines a template for a SDSD angularJS controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.controller('TODO', function ($scope, sdsd) {
	$scope.sdsd = sdsd;

	$scope.init = function () {

	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {

		} else {

		}
	});

});