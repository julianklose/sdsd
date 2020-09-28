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

	$scope.$watch('sdsd.username', newVal => {
		if (newVal) {
			$scope.loadPage();
			$scope.getAutocompleteTypes();
			if(!$scope.page) $scope.getFormats();
		} else {
			$scope.page = undefined;
			$scope.input = {};
			$scope.autocomplete = {
				formats: [],
				types: [],
				instances: {}
			};
		}
	});

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
	$scope.$on('$locationChangeSuccess', $scope.loadPage);

	$scope.getFormats = () => {
		sdsd.rpcCall('format', 'listFormats', [true], data => {
			$scope.unpublishedFormats = data.drafts;
			$scope.publishedFormats = data.formats;
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

	$scope.$watch('search.input', newVal => {
		if (newVal) {
			const ind = newVal.lastIndexOf('?page=');
			$scope.search.page = ind >= 0 ? newVal.slice(ind + 6) : newVal;
		} else
			$scope.search.page = null;
	});

	$scope.search = () => {
		$timeout(() => $location.search('page', $scope.search.page));
	}
});

const pagelimit = 100;

app.controller('wikilist', ($scope, $location, sdsd) => {
	$scope.sdsd = sdsd;

	$scope.init = () => {
		$scope.type = $location.search().type;
		$scope.page = $location.search().page || 1;
	};

	$scope.$watch('sdsd.username', newVal => {
		if (newVal) {
			$scope.loadPage();
		} else {
			$scope.title = '';
			$scope.list = [];
			$scope.pos = {};
		}
	});

	$scope.loadPage = () => {
		if ($scope.type && sdsd.username && !$scope.loading) {
			$scope.loading = true;
			sdsd.rpcCall('wikinormia', 'listInstances', [$scope.type, ($scope.page - 1) * pagelimit, pagelimit, true], data => {
				$scope.title = data.title;
				$scope.list = data.list;
				$scope.pos = data.pos;
			}, () => {
				$scope.loading = false;
			});
		}
	}

	$scope.prev = () => {
		if ($scope.pos.total && $scope.pos.offset > 0) {
			$location.search('page', --$scope.page);
			$scope.loadPage();
		}
	}

	$scope.next = () => {
		if ($scope.pos.total && ($scope.pos.offset + $scope.list.length) < $scope.pos.total) {
			$location.search('page', ++$scope.page);
			$scope.loadPage();
		}
	}
});
