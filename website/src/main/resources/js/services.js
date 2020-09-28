/**
 * SDSD client functions for the services page.
 * 
 * @file   Defines the angularJS services controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.controller('services', function ($scope, sdsd) {
	$scope.sdsd = sdsd;

	$scope.servicelist = [];
	$scope.activeServices = [];
	$scope.completedServices = [];
	
	$scope.init = function () {

	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if(newVal) {
			$scope.loadServiceList();
			$scope.loadActiveServices();
			$scope.loadCompletedServices();
		}
		else {
			$scope.servicelist = [];
			$scope.activeServices = [];
			$scope.completedServices = [];
		}
	});

	$scope.loadServiceList = function() {
		sdsd.rpcCall("service", "listServices", [], function(data) {
			$scope.servicelist = data.services;
		});
	};
	
	$scope.loadActiveServices = function() {
		sdsd.rpcCall("service", "listActiveServices", [], function(data) {
			$scope.activeServices = data.activeServices;
		});
	};
	
	$scope.loadCompletedServices = function() {
		sdsd.rpcCall("service", "listCompletedServices", [], function(data) {
			$scope.completedServices = data.completedServices;
		});
	};
	
	$scope.activate = function(service) {
		sdsd.rpcCall("service", "activateService", [service.id], function(data) {
			$scope.loadActiveServices();
		});
	};
	
	$scope.delete = function(instance) {
		if(!confirm("Are you sure you want to delete this service instance?")) return;
		sdsd.rpcCall("service", "deleteInstance", [instance.id], function(data) {
			if(data.success) {
				let i = $scope.activeServices.indexOf(instance);
				if(i >= 0) $scope.activeServices.splice(i, 1);
				i = $scope.completedServices.indexOf(instance);
				if(i >= 0) $scope.completedServices.splice(i, 1);
			}
		});
	};
});