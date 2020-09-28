/**
 * SDSD client functions for the service creator page.
 * 
 * @file   Defines the angularJS service_creator controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.directive('ngAutocomplete', function($parse) {
	return {
		restrict: 'A',
		require: 'ngModel',
		compile: function(elem, attrs) {
			let modelAccessor = $parse(attrs.ngModel);
			
			return function(scope, element, attrs) {
				scope.$watch(attrs.ngAutocomplete, function(val) {
					element.autocomplete({ 
						source: val,
						select: function(event, ui) {
							scope.$apply(function(scope){
								modelAccessor.assign(scope, ui.item.value);
							});
							if(attrs.onSelect)
								scope.$apply(attrs.onSelect);
							event.preventDefault();
						}
					});
				});
			}
		}
	}
});

app.controller('service_creator', function ($scope, sdsd) {
	$scope.sdsd = sdsd;
	
	$scope.myservicelist = [];

	$scope.init = function () {
		$scope.resetCreate();
		$('.datetimepicker').datetimepicker({
			format: 'Y-m-d H:i'
		});
	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			$scope.loadMyServiceList();
			$scope.getAutocompleteTypes();
		} else {
			$scope.myservicelist = [];
			delete $scope.showInstance;
		}
	});

	$scope.loadMyServiceList = function() {
		sdsd.rpcCall("service", "listMyServices", [], function(data) {
			$scope.myservicelist = data.myServices;
		});
	};
	
	$scope.showInstances = function(service) {
		if(!service) delete $scope.showInstance;
		else {
			sdsd.rpcCall("service", "listMyInstances", [service.id], function(data) {
				$scope.showInstance = data;
			});
		}
	};
	
	$scope.copyToClipboard = function(text_to_copy) {
		// create temp element
		var copyElement = document.createElement("span");
		copyElement.appendChild(document.createTextNode(text_to_copy));
		copyElement.id = 'tempCopyToClipboard';
		angular.element(document.body.append(copyElement));

		// select the text
		var range = document.createRange();
		range.selectNode(copyElement);
		window.getSelection().removeAllRanges();
		window.getSelection().addRange(range);

		// copy & cleanup
		document.execCommand('copy');
		window.getSelection().removeAllRanges();
		copyElement.remove();
	};
	
	$scope.setError = function(instance) {
		let message = prompt("Enter the error message for the user");
		if(message === null) return;
		sdsd.rpcCall("service", "errorInstance", [instance.token, message], function(data) {
			if(data.success) {
				instance.error = message;
			}
		});
	};
	
	$scope.complete = function(instance) {
		let result = prompt("Are you sure you want to complete this service instance?", "Result");
		if(result === null) return;
		sdsd.rpcCall("service", "completeInstance", [instance.token, result], function(data) {
			if(data.success) {
				instance.completed = new Date();
			}
		});
	};
	
	$scope.setVisible = function(service, visible) {
		sdsd.rpcCall("service", "setServiceVisible", [service.id, visible], function(data) {
			if(data.success)
				service.visible = visible;
		});
	};
	
	$scope.delete = function(service) {
		if(!confirm("Are you sure you want to delete this service?")) return;
		sdsd.rpcCall("service", "deleteService", [service.id], function(data) {
			if(data.success)
				$scope.myservicelist.splice($scope.myservicelist.indexOf(service), 1);
		});
	};
	
	$scope.getAutocompleteTypes = function() {
		sdsd.rpcCall("wikinormia", "listTypes", [], function(data) {
			$scope.autocomplete = data;
		});
	}
	
	$scope.addItem = function(collection, fill={}) {
		collection.push(fill);
	}
	
	$scope.deleteItem = function(collection, item) {
		let index = collection.indexOf(item);
		if(index >= 0) collection.splice(index, 1);
	}
	
	$scope.editParameter = function(p) {
		$scope.parameterModal = { parameter: p };
	}
	
	$scope.create = function() {
		try {
			if(!$scope.input.name) throw "Please enter a service name";
			if($scope.input.parameter) {
				for(let p of $scope.input.parameter) {
					if(!p.name)
						throw "Please enter names for all parameters";
					if(p.from) {
						let val = new Date(p.from);
						if(isNaN(val.getTime()))
							throw "Invalid 'from' date";
						p.from = val.toISOString();
					}
					if(p.until) {
						let val = new Date(p.until);
						if(isNaN(val.getTime()))
							throw "Invalid 'until' date";
						p.until = val.toISOString();
					}
				}
			}
			
			sdsd.rpcCall("service", "createService", [$scope.input], function(data) {
				if(data.success) {
					$scope.resetCreate();
					$scope.loadMyServiceList();
				}
				$scope.createSuccess = data.success;
			});
		} catch (e) {
			alert(e);
		}
	};
	
	$scope.resetCreate = function() {
		$scope.input = {
			name: "",
			parameter: [],
			access: []
		};
	};
});