/**
 * SDSD client functions for the serviceinstance page.
 * 
 * @file   Defines the angularJS service_instance controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.filter('wikiPageName', function() {
	return function(input) {
		return decodeURIComponent(input.slice(input.lastIndexOf('=') + 1));
	};
});

app.filter('arrayJoin', function() {
	return function(input) {
		return Array.isArray(input) ? input.join(', ') : input;
	};
});

app.controller('service_instance', function ($scope, $location, sdsd) {
	$scope.sdsd = sdsd;
	
	$scope.instance = {};
	$scope.instanceId = null;

	$scope.init = function () {
		$scope.instanceId = $location.hash();
	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			$scope.getInstance();
		} else {
			$scope.instance = {};
		}
	});

	$scope.getInstance = function() {
		sdsd.rpcCall("service", "getInstance", [$scope.instanceId], function(data) {
			$scope.instance = data.serviceInstance;
		}, function() {
			$('.datetimepicker').datetimepicker({
				format: 'Y-m-d H:i'
			});
			$('[data-toggle="tooltip"]').tooltip();
		});
	};
	
	$scope.setInstanceParameter = function() {
		let params = {};
		for(let p of $scope.instance.neededParmeters) {
			if(p.options) {
				let min = (p.min || p.min === 0) ? p.min : 1;
				let max = (p.max || p.max === 0) ? p.max : (min > 1 ? 2147483647 : 1);
				if(min > 1 || max > 1) {
					if(min > 0 && !p.value || p.value && p.value.length < min) {
						alert("Please select at least " + min + " entries for " + p.label);
						return;
					}
					if(p.value && p.value.length > max) {
						alert("Please select not more than " + max + " entries for " + p.label);
						return;
					}
				} else {
					if(min > 0 && !p.value) {
						alert("Please select a value for " + p.label);
						return;
					}
				}
			}
			else {
				if(!p.value && p.value !== 0) {
					alert("Please fill in " + p.label);
					return;
				}
				if(p.type == 'number') {
					if((p.min || p.min === 0) && p.value < p.min) {
						alert("Please set at least " + p.min + " for " + p.label);
						return;
					}
					if((p.max || p.max === 0) && p.value > p.max) {
						alert("Please set not more than " + p.max + " for " + p.label);
						return;
					}
				}
				else if(p.type == 'datetime') {
					let val = new Date(p.value);
					
					if(isNaN(val.getTime())) {
						alert("Invalid date for " + p.label);
						return;
					}
					else {
						if(p.from) {
							let from = new Date(p.from);
							if(val < from) {
								alert("Please don't set a date before " + from.toLocaleString() + " for " + p.label);
								return;
							}
						}
						if(p.until) {
							let until = new Date(p.until);
							if(val > until) {
								alert("Please don't set a date after " + until.toLocaleString() + " for " + p.label);
								return;
							}
						}
						p.value = val.toISOString();
					}
				}
			}
			params[p.name] = p.value;
		}

		sdsd.rpcCall("service", "setInstanceParameter", [$scope.instanceId, params], function(data) {
			if(data.success)
				$scope.getInstance();
		});
	};
	
	$scope.loadPermissionOptions = function(p) {
		$scope.permitModal = { permission: p };
		sdsd.rpcCall("service", "getPermissionOptions", [p.type], function(data) {
			$scope.permitModal.p = p;
			$scope.permitModal.options = data.options;
		});
	}
	
	$scope.savePermissions = function() {
		let perm = $scope.instance.permissions.map(p => { return { id: p.id, allow: p.allow, objs: p.objs } });
		let tp = { 
			from: $scope.instance.timePermission.from ? new Date($scope.instance.timePermission.from) : undefined, 
			until: $scope.instance.timePermission.until ? new Date($scope.instance.timePermission.until) : undefined
		};
		if(tp.from && tp.until && tp.from > tp.until) {
			alert("Invalid time permission: 'until' is before 'from'");
			return;
		}
		sdsd.rpcCall("service", "setInstancePermissions", [$scope.instanceId, perm, tp], function(data) {
			$scope.permissionSuccess = data.success;
			if(data.success)
				$scope.getInstance();
		});
	};
	
});