/**
 * SDSD client functions for the wikinormia page.
 * 
 * @file   Defines the angularJS wikinormia controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.directive('ngAutocomplete', $parse => {
	return {
		restrict: 'A',
		require: 'ngModel',
		compile: (elem, attrs) => {
			const modelAccessor = $parse(attrs.ngModel);

			return (scope, element, attrs) => {
				scope.$watch(attrs.ngAutocomplete, val => {
					element.autocomplete({
						source: val,
						select: (event, ui) => {
							scope.$apply(scope => {
								modelAccessor.assign(scope, ui.item.value);
							});
							if (attrs.onSelect)
								scope.$apply(attrs.onSelect);
							event.preventDefault();
						}
					});
				});
			}
		}
	}
});

app.directive('ngLink', $location => {
	return (scope, element, attrs) => {
		element.attr('href', attrs.ngLink);
		element.bind('click', () => {
			const ind = attrs.ngLink.lastIndexOf('?page=');
			if (ind >= 0) {
				scope.$apply($location.search('page', attrs.ngLink.slice(ind + 6)));
				return false;
			}
			return true;
		});
	}
});

app.directive('customOnChange', function() {
	return {
		restrict: 'A',
		link: function(scope, element, attrs) {
			var onChangeHandler = scope.$eval(attrs.customOnChange);
			element.on('change', (event) => {
				scope.$apply(() => onChangeHandler(event))
			});
			element.on('$destroy', function() {
				element.off();
			});
		}
	};
});

app.filter('localname', () => {
	return input => {
		if (!input) return input;
		let index = input.lastIndexOf('#');
		if (index >= 0) return input.slice(index + 1);
		index = input.lastIndexOf('=');
		if (index >= 0) return input.slice(index + 1);
		index = input.lastIndexOf('/');
		return input.slice(index + 1);
	};
});

app.controller('wikinormia', ($q, $scope, $location, $timeout, sdsd) => {
	$scope.sdsd = sdsd;
	$scope.page;

	// ============================ $watch ======================================

	$scope.$watch('sdsd.username', newVal => {
		if (newVal) {
			$scope.loadPage();
			$scope.getAutocompleteTypes();
			if(!$scope.page) $scope.getFormats();
		} else {
			$scope.page = undefined;
			$scope.input = {};
			$scope.initParserInput();
			$scope.autocomplete = {
				formats: [],
				types: [],
				instances: {}
			};
		}
	});

	$scope.$watch('search.input', newVal => {
		if (newVal) {
			const ind = newVal.lastIndexOf('?page=');
			$scope.search.page = ind >= 0 ? newVal.slice(ind + 6) : newVal;
		} else
			$scope.search.page = null;
	});

	// ============================== Load page =================================

	$scope.loadPage = () => {
		$scope.page = $location.search().page;
		$scope.input = {
			class: {
				identifier: '', label: ''
			},
			format: {
				value: ''
			},
			description: '',
			base: [],
			partof: [],
			attributes: [],
			instances: []
		};

		if ($scope.page && sdsd.username && !$scope.loading) {
			$scope.loading = true;
			$location.search('page', $scope.page);
			$scope.search.input = 'https://app.sdsd-projekt.de/wikinormia.html?page=' + $scope.page;
			sdsd.rpcCall('wikinormia', 'get', [$scope.page, !$scope.isCreate], data => {
				$scope.input = data;
			}, () => {
				$scope.loading = false;
			});
		}
	}

	// =============================== RPC Calls ================================

	/**
	 * @function
	 * @name listLiteralTypes
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list literal types.
	 * The used endpoint is format/listLiteralTypes.
	 */
	$scope.listFormats = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'listFormats', [true], data => resolve(data));
		});
	}

	$scope.getFormat = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'get', [$scope.parserInput.class.value], data => resolve(data));
		});
	}

	// ================================ $on =====================================

	$scope.$on('$locationChangeSuccess', $scope.loadPage);

	// ============================== Utitlity ==================================

	$scope.initParserInput = () => {
		$scope.parserInput = {
			class: {},
			format: '',
			parser: '',
			parseCommand: '',
			testCommand: ''
		};
	}

	$scope.removeForeignFormats = () => {
		const temp = [];
		for (const format of $scope.publishedFormats)
			if (format.author === sdsd.username)
				temp.push(format);
		$scope.publishedFormats = temp;
	}

	$scope.parserUpload = event => {
		if (event.target.files.length >= 0)
			$scope.parserInput.parser = event.target.files[0].name;
		else
			$scope.parserInput.parser = null;
	}

	// ============================== Get data ==================================

	$scope.getFormats = () => {
		$scope.listFormats().then(data => {
			$scope.unpublishedFormats = data.drafts;
			$scope.publishedFormats = data.formats;
			$scope.removeForeignFormats();
			console.log("listFormats:", $scope.publishedFormats);
		});
	}

	$scope.getParserData = () => {
		$scope.getFormat().then(data => {
			$scope.parserInput = data;
			console.log("getParserData:", $scope.parserInput);
		});
	}

	$scope.getAutocompleteTypes = () => {
		sdsd.rpcCall('wikinormia', 'listTypes', [], data => {
			if ($scope.autocomplete)
				$scope.autocomplete.types = data.types;
			else
				$scope.autocomplete = data;
		});
	}

	$scope.getAutocompleteFormats = () => {
		sdsd.rpcCall('format', 'listFormats', [], data => {
			if ($scope.autocomplete)
				$scope.autocomplete.formats = data.formats;
			else
				$scope.autocomplete = data;
		});
	}

	$scope.search = () => {
		$timeout(() => $location.search('page', $scope.search.page));
	}
});

// ======================= End wikinormia Controller ==========================
