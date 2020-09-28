/**
 * SDSD client functions for formats.
 * 
 * @file   Defines the angularJS format controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.config(function($locationProvider) {
	$locationProvider.html5Mode({
		enabled: true,
		requireBase: false,
		rewriteLinks: false
	});
});

app.directive('customOnChange', function() {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var onChangeHandler = scope.$eval(attrs.customOnChange);
			element.on('change', (...args) => scope.$apply(() => onChangeHandler(...args)));
			element.on('$destroy', function() {
				element.off();
			});
		}
	};
});

app.filter('relative', function() {
	return function(input) {
		if(input && input.startsWith('https://app.sdsd-projekt.de/'))
			return input.slice(27);
		return input;
	};
});

app.controller('format', function ($scope, $location, sdsd) {
	$scope.sdsd = sdsd;

	$scope.init = function () {

	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			$scope.loadFormat();
			$scope.loadARTypes();
		} else {
			$scope.input = {};
			$scope.artypes = [];
		}
	});

	$scope.parserUpload = function (event) {
		if(event.target.files.length > 0)
			$scope.input.parser = event.target.files[0].name;
		else
			$scope.input.parser = null;
	}
	
	$scope.loadFormat = function() {
		let format = $location.search().format;
		$scope.input = {
			class: { identifier: "", label: "" },
			description: "",
			mimetype: "",
			artype: "",
			parser: "",
			parseCommand: "",
			testCommand: ""
		};
		if(format && sdsd.username && !$scope.loading) {
			$scope.loading = true;
			$location.search('format', format);
			sdsd.rpcCall("format", "get", [format], function(data) {
				$scope.input = data;
			}, function() {
				$scope.loading = false;
			});
		}
	}
	$scope.$on('$locationChangeSuccess', $scope.loadFormat);
	
	$scope.loadARTypes = function () {
		sdsd.rpcCall("format", "listARTypes", [], function(data) {
			$scope.artypes = data.artypes;
		}, null, $scope.loadLogList);
	};
	
	$scope.save = function() {
		$scope.success = null;
		$scope.loading = true;
		sdsd.rpcCall("format", "create", [$scope.input], function(data) {
			if(data.success)
				$scope.success = $scope.input.class.value = data.uri;
		}, function() {
			$scope.loading = false;
		});
	}

});