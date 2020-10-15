/**
 * SDSD client functions for the telemetry page.
 * 
 * @file   Defines the angularJS telemetry controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */
app.filter('reverse', function () {
	return function (items) {
		return items.slice().reverse();
	};
});
/**
 * telemetryCtrl Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 * @param location service that parses the browser URL and makes it available
 */
app.controller('telemetryCtrl', function ($scope, $location, sdsd) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	
	$scope.caption = [];
	$scope.telemetry = [];
	$scope.geoJSON = null;
	$scope.fileid = null;
	$scope.offset = 0;
	$scope.showlast = 50;
	$scope.total = 50;
	$scope.map = null;

	/**
	 * init - called on startup. Intializes a leaflet map and gets the url hash.
	 *
	 */
	$scope.init = function () {
		$scope.initMap();

		$scope.fileid = $location.hash();
		$scope.inFocus = true;
		$scope.update = false;
	};
	
	/**
	 * watch(sdsd.username) - watches the username variable. If theres a new user, the "listCapabilities" remote call is started.
	 *
	 * @param newVal new username
	 */
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			sdsd.rpcCall("agrirouter", "listCapabilities", [], function(data) {
				if(data.pushNotifications > 0) {
					sdsd.connectWebsocket().then(function(ws) {
						$scope.loadTelemetry(!ws);
						if(ws) {
							sdsd.setListener("telemetry", "update", $scope.fileid, function(params) {
								$scope.appendData(params[0]);
							});
						}
					});
				} else {
					$(window).focus(function () {
						if ($scope.inFocus) return;
						$scope.inFocus = true;
						if ($scope.update)
							$scope.updateTelemetry();
					});

					$(window).blur(function () {
						$scope.inFocus = false;
					});
					
					$scope.loadTelemetry(true);
				}
			}, null, function(error) {
				$scope.loadTelemetry(false);
			});
		}
	});
	
	/**
	 * initMap - Intializes a leaflet map
	 *
	 */
	$scope.initMap = function () {
		$scope.map = L.map('map', {
			'center': [0, 0],
			'zoom': 0,
			'layers': [
				L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
					attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
				})
			],
			pointToLayer: function (feature, latlng) {
				return L.marker(latlng/*, {
					'icon': L.icon({
						iconUrl: 'https://upload.wikimedia.org/wikipedia/commons/b/b5/Tractor_icon.svg',
						shadowUrl: 'marker-shadow.png',
						iconSize: [30, 30], // size of the icon
						shadowSize: [30, 30], // size of the shadow
						iconAnchor: [22, 94], // point of the icon which will correspond to marker's location
						shadowAnchor: [4, 62], // the same for the shadow
						popupAnchor: [-3, -76] // point from which the popup should open relative to the iconAnchor
					})
				}*/);
			}
		});
	};
	
	/**
	 * updateMap - updates the leaflet map. Removes the old map layer and load and displays the new layer.
	 *
 	 */
	$scope.updateMap = function () {
		if($scope.layer) $scope.layer.remove();
		$scope.layer = L.geoJSON($scope.geoJSON);
		$scope.layer.addTo($scope.map);
		$scope.map.fitBounds($scope.layer.getBounds());
	};

	$scope.doUpdate = function () {
		$scope.update = true;
		if ($scope.inFocus)
			$scope.$apply($scope.updateTelemetry());
	};
	/**
	 * loadTelemetry - starts remote call that loads the telemetry data. After the call the data gets updated and displayed on the map
	 * @param  updatetimer boolean. determines whether the telemetry data is updated every 30000 ms.
	*/
	$scope.loadTelemetry = function (updatetimer) {
		$scope.autoupdate = updatetimer;
		$scope.update = false;
		clearTimeout($scope.timer);
		if(!$scope.fileid) return;
		$scope.updateLoading = true;
		sdsd.rpcCall("telemetry", "telemetry", [$scope.fileid, -$scope.showlast, $scope.showlast], function (data) {
			$scope.caption = data.caption;
			$scope.telemetry = data.telemetry;
			$scope.geoJSON = data.geoJSON;
			$scope.offset = data.offset + data.telemetry.length;
			$scope.total = data.total;

			$scope.updateMap();

			if(updatetimer)
				$scope.timer = setTimeout($scope.doUpdate, 30000);
		}, function() {
			$scope.updateLoading = false;
		});
	};
	/**
	 * updateTelemetry - starts remote call to update the Telemtry data. 
	 *  
	*/
	$scope.updateTelemetry = function() {
		if(!$scope.autoupdate) return;
		$scope.update = false;
		clearTimeout($scope.timer);
		$scope.updateLoading = true;
		sdsd.rpcCall("telemetry", "updateTelemetry", [$scope.fileid, $scope.offset, -1], function(data) {
			$scope.offset = data.offset;
			$scope.appendData(data);
			$scope.timer = setTimeout($scope.doUpdate, 30000);
		}, function() {
			$scope.updateLoading = false;
		});
	};
		/**
	 * appendData append received telemtry data and updates the leaflet map
	 * @param  data telemtry data
	*/
	$scope.appendData = function(data) {
		if(data.telemetry.length > 0) {
			$scope.telemetry = $scope.telemetry.concat(data.telemetry);

			if($scope.geoJSON) {
				$scope.geoJSON.features = $scope.geoJSON.features.concat(data.geoJSON.features.slice(1));
				$scope.geoJSON.features[0].geometry.coordinates = $scope.geoJSON.features[0].geometry.coordinates.concat(data.geoJSON.features[0].geometry.coordinates);
			}
			else
				$scope.geoJSON = data.geoJSON;
			$scope.updateMap();
		}

		$scope.offset += data.telemetry.length;
		$scope.showlast = $scope.telemetry.length;
		$scope.total = data.total;
	};
});