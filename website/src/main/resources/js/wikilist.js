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