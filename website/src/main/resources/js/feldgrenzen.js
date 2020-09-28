
/**
 * Feldgrenzen Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 * @author Henrik Oltmanns, Hochschule Osnabrueck
 */
app.controller('feldgrenzen', function ($scope, sdsd) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	// leaflet map
	$scope.map = null;
	
	// leaflet map layer groups
	$scope.layerGroups = {};
	// selected field layer group
	$scope.layerGroups.selectedField;
	// field layer group
	$scope.layerGroups.fields;
	
	// field data
	$scope.fields;
	// seleceted field data
	$scope.sameAs;
	
	// list index of the seleceted field
	$scope.indexSelectedField = null;
	/**
	 * 	preferred field - object which includes the preferred label and other field informations
	 *  	prefLabel - preferred Label
	 *		area - field size
	 *		geolabel - geo label
	 * 		graph - uri of the corresponding graph
	 *		label - field label
	 *		uri - uri
	*/
	$scope.preferred = {};
	$scope.preferred.prefLabel = "";
	
	/**
	 * sets selected field
	 *
	 * @param index list index
	 * @param pref preferred field
	 */
	$scope.setSelected = function (index, pref) {
		let prefLabel = "";
		if (pref.prefLabel) prefLabel = pref.prefLabel;
		if ($scope.preferred && $scope.preferred.prefLabel) prefLabel = $scope.preferred.prefLabel;
		if ($scope.preferred) delete $scope.preferred.prefLabel;
		$scope.indexSelectedField = index;
		$scope.preferred = pref;
		$scope.preferred.prefLabel = prefLabel;
		$scope.hideLayers(pref.uri);
	};
	
	// field borders
	$scope.fieldborders = [];
	// map
	$scope.tileLayers;
	// base layers have to be mutually exclusive (only one can be visible at a time), e.g. tile layers.	
	$scope.baseLayers;
	
	// statemachine for the field selection menu
	$scope.step;
	
	/**
	 * setStep - changes the current state of the field selection menu
	 *
	 * @param step new state of the field selection menu
	 */
	$scope.setStep = function(step){
			delete $scope.indexSelectedField;
		if(step != 2){
			$scope.deleteSelectedField();
			delete $scope.preferred;
			$scope.preferred = {};
			$scope.preferred.prefLabel = "";
		}
		$scope.step = step;
	};
	
	/**
	 * sortingFn - returns the preferred label if there is one, else returns the label 
	 *
	 * @param f field
	 */
	$scope.sortingFn = function( f ) {
		if(f.properties.prefLabel)
			return f.properties.prefLabel.toString().toLowerCase();
		return f.properties.label.toString().toLowerCase();
	};
	
	/**
	 * fieldLabel - returns the preferred label if there is one, else returns the label 
	 *
	 * @param f field
	 */
	$scope.fieldLabel = function( f ) {
		if(f.properties.prefLabel)
			return f.properties.prefLabel;
		return f.properties.label;
	};

	/**
	 * hideLayers - hides all layers, except the layer with the given uri, on the leaflet map
	 *
	 * @param uri field uri
	 */
	$scope.hideLayers = function(uri){
		$scope.layerGroups.selectedField.eachLayer(function (layer){
			if(layer.field.properties.uri === uri) layer.field.visible = true;
			else layer.field.visible = false;		
		});
		$scope.updateMap();
	};
	
	/**
	 * toggleVisibility - hides the given layer if its visible or shows it if its invisible on the map
	 *
	 * @param field field
	 */
	$scope.toggleVisibility = function(field){
		field.visible = !field.visible;
		$scope.updateMap();
	};

	/**
	 * init - called on startup
	 *
	 */
	$scope.init = function () {
		$scope.initMap();
	};
	
	/**
	 * watch(step) - watches the state of the selection menu. When it changes the leaflet map gets uptaded.
	 *
	 */
	$scope.$watch('step', function () {
		$scope.updateMap();
	});
	
	/**
	 * watch(sdsd.username) - watches the user variable. If theres a new user, the users fieldlist is loaded
	 *
	 * @param newVal new username
	 */
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			$scope.loadAllFields();
		} else {
			$scope.fields = [];
			$scope.sameAs = [];
			delete $scope.preferred;
			$scope.preferred = {};
			$scope.preferred.prefLabel = "";
		}
	});
	
	/**
	 * initMap - Intializes a leaflet map with 2 tile layers
	 *
	 */
	$scope.initMap = function (){
		$scope.setTileLayers();		
		$scope.map = L.map('map', {
			preferCanvas: true,
			center: [52.283784, 8.023442],
			zoom: 2,
			layers: [ $scope.tileLayer ],
			pointToLayer: function (feature, latlng) {
				return L.marker(latlng);
			}
		});
		
		$scope.baseLayers = {
				"Aerial Image": $scope.tileLayers.image,
				"Open Street Map": $scope.tileLayers.osm
		};
		// adds layer control button that toggles between the base layers
		L.control.layers($scope.baseLayers).addTo($scope.map);
	};
	
	/**
	 * setTileLayers - sets the current tile layers
	 *
	 */	
	$scope.setTileLayers = function() {
		// array with tileLayers
		$scope.tileLayers = {
			image: L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
				attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
			}),
			osm: L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
				maxZoom: 19,
				attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
			})
		};
		
		// active tileLayer
		$scope.tileLayer = $scope.tileLayers.image;
	};
	
	/**
	 * updateMap - updates the leaflet map. Loads layers that should be displayed in the current menu state and fits the layers bounds.
	 *
 	 */
	$scope.updateMap = function () {
		let bounds = null;
		let layers;
		switch($scope.step){
			case 1:
				layers = $scope.layerGroups.fields;
				if(layers != null){
					layers.addTo($scope.map);
					layers.eachLayer(function (layer){
						if(bounds){bounds.extend(layer.getBounds())}
						else {bounds = layer.getBounds();}
					});
				}
				break;
			case 2:
				bounds = $scope.addSelectedFieldsToMap();
				break;
		}
		if(bounds) $scope.map.fitBounds(bounds);
	};
	
	/**
	 * addSelectedFieldsToMap - adds scope.layerGroups.selectedField to the map, adds the field bounds to the map bounds and returns the updated map bounds
	 *
	 * @param bounds old map bounds
	 * @return updated map bounds
	 */
	$scope.addSelectedFieldsToMap = function (bounds) {
		if($scope.layerGroups.selectedField){
			$scope.layerGroups.selectedField.eachLayer(function (layer){
				if(layer.field.visible && !$scope.map.hasLayer(layer)){
					layer.addTo($scope.map);
					if(bounds){bounds.extend(layer.getBounds())}
					else {bounds = layer.getBounds();}
				} else if (!layer.field.visible && $scope.map.hasLayer(layer)){
					$scope.map.removeLayer(layer);
				}
			});
		}
		return bounds;
	};
	
	/**
	 * loadAllFields - starts a rpc call to retrive the users fields, if there are no fields yet. Sets the field selection menu in state 2
	 *
	 */
	$scope.loadAllFields = function() {
		if($scope.layerGroups.fields){
			$scope.setStep(1);
		}
		else{
			$scope.loading = true;
			sdsd.rpcCall("dashboard", "getAllFields", [], function(data) {
				$scope.fields = data.fields;
				$scope.layerGroups.fields = L.layerGroup([]);
				for(let geoJson of $scope.fields){
					let layer = $scope.createLayer(geoJson);
					layer.visible = false;
					layer.field = geoJson;
					layer.on('click', function() {
						$scope.layerOnClick(this.field);
					});
					layer.addTo($scope.layerGroups.fields);
				}
				$scope.loading = false;
				$scope.setStep(1);
			});
		}
	};
	
	/**
	 * layerOnClick - on click listener that loads the selected field
	 *
	 * @param selectedField selected Field
	 */
	$scope.layerOnClick = function(selectedField) {
		if($scope.step = 1)
			$scope.loadSameAs(selectedField);
	}
	
	/**
	 * loadSameAs - starts a rpc call to retrive the users fields that belong to the same equivalence class as the selected field. Sets the field selection menu in state 2
	 *
	 * @param selectedField selected Field
	 */
	$scope.loadSameAs = function(selectedField) {
		let fieldUri = selectedField.properties.res[0].uri;
		$scope.selectedField = selectedField;
		delete $scope.preferred;
		$scope.preferred = {};
		$scope.preferred.prefLabel = "";
		sdsd.rpcCall("feldgrenzen", "getSameAs", [fieldUri], function(data) {
			$scope.sameAs = data.sameAs;
			$scope.removeFieldsLayers();
			$scope.deleteSelectedField();
			$scope.layerGroups.selectedField = L.layerGroup([]);
			for(let field of $scope.sameAs){
				field.visible = false;
				let layer = $scope.createLayer(field);
				layer.field = field;
				layer.addTo($scope.layerGroups.selectedField);
			}
			$scope.setStep(2);
			$scope.toggleVisibility($scope.sameAs[0]);
			if($scope.sameAs[0].properties.prefLabel){
				$scope.preferred = $scope.sameAs[0].properties;
				$scope.indexSelectedField = 0;
			}
		});
	};
	
	/**
	 * deleteSelectedField - deletes all fields in layerGroups.selectedField
	 */
	$scope.deleteSelectedField = function() {
		if($scope.layerGroups.selectedField){
			$scope.layerGroups.selectedField.eachLayer(function (layer){
				$scope.map.removeLayer(layer);
			});
			delete $scope.layerGroups.selectedField;
		}
	};

	/**
	 * removeFieldsLayers - removes layerGroups.fields from the leaflet map
	 */
	$scope.removeFieldsLayers = function() {
		if($scope.layerGroups.fields){
			$scope.map.removeLayer($scope.layerGroups.fields);
		}
	};
	
	/**
	 * setPref - starts rpc call that saves the selected preferred field and its preferred label. Sets the menu in state 1 after the call.
	 */
	$scope.setPref = function() {
		sdsd.rpcCall("feldgrenzen", "setPreferred", [$scope.preferred.uri, $scope.preferred.prefLabel], function(data) {
			if(data.success){
				if($scope.selectedField && $scope.selectedField.properties){
					$scope.selectedField.properties.prefLabel = $scope.preferred.prefLabel;
					$scope.setStep(1);
				}
			}
		});
	};
	
	/**
	 * confirmCreateIsoxml - opens a user dialog that asks if the user wants to generate a xml file with the selected label. If the user confirms the dialog a file is created.
	 */
	$scope.confirmCreateIsoxml = function() {
		if(confirm("Isoxml Datei der ausgewählten Feldgrenze von " + $scope.preferred.prefLabel + " erstellen?")){
			$scope.createIsoxml();
		}
	}
	
	/**
	 * createIsoxml - start a rpc call for the creation an isoxml file. If it succeeds an alert is displayed.
	 */
	$scope.createIsoxml = function() {
		sdsd.rpcCall("feldgrenzen", "createIsoxml", [$scope.preferred.uri, $scope.preferred.prefLabel], function(data) {
			if(data.filename){
				alert(data.filename + " erstellt.");
			}
		});
	};
	
	/**
	 * createLayer - creates a layer from the geo data. returns undefined when it fails.
	 *
	 * @param geojson geo data that defines the new layer
	 * @return new layer
	 */
	$scope.createLayer = function(geojson) {
		if(geojson) {
			return L.geoJSON(geojson, {
				pointToLayer: function (feature, latlng) {
					return L.circleMarker(latlng, {radius: 2});
				}
			});
		}
		return undefined;
	};

});