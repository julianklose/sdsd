
/**
 * app module
 *
 * @param sdsd sdsd module
 * @param colorpicker.module colorpicker module
 * @param dndLists provides drag and drop directives for lists
 * @param app.map map controller
 */
angular.module('app', ['sdsd', 'app.map', 'dndLists', 'colorpicker.module']);


/**
 * numkeys filter
 *
 * @returns filter that returns the objects key count
 */
app.filter('numkeys', function() {
	return function(object) {
		return Object.keys(object).length;
	}
});


/**
 * Map Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 * @param location service that parses the browser URL and makes it available
 * @param sharedMapData provides shared Data between the map controller and the map.data controller
 * @param compile compiles a HTML string
 * @author Henrik Oltmanns, Hochschule Osnabrueck
 */
angular.module('app').controller('map', function ($scope, $location, $compile, sdsd, sharedMapData) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	// leaflet map
	$scope.map = null;
	// leaflet map layers
	$scope.layers = [];
	// shaared map data with the map.data controller
	$scope.sharedMapData = sharedMapData;
	// drag and drop list model
	$scope.models = {
		dragStartIndex: null,
		selectedColoring: "default",
		selected: null,
		lists: { "activeObjects": [], "relativeValues": {"default" : 0}},
	};
	// default value of the "color by" selector
	$scope.models.defaultSelectedValue = $scope.models.lists.relativeValues["default"];
	// link representing the current state of the list of models.
	$scope.link = "";
	// map
	$scope.tileLayers;
	// base layers have to be mutually exclusive (only one can be visible at a time), e.g. tile layers.	
	$scope.baseLayers;
	
	/**
	 * setTileLayers - sets the background layers as a worldmap and a openstreetmap
	 *
	 */
	$scope.setTileLayers = function() {
		// array of tileLayers
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
	 * updateUrl - updates the url to represent the current state of the layers list
	 *
	 */
	$scope.updateUrl = function(){
		$scope.link = "";
		if($scope.models.lists.activeObjects.length > 0) {
			let urlmap = new UrlEncoder();
			for(let activeObject of $scope.models.lists.activeObjects) {
				urlmap.addLayer(activeObject.type, activeObject.file.id, activeObject.id, activeObject.activeDDI, activeObject.color, activeObject.visible);
			}
			
			let mappings = urlmap.getMappings();
			let layers = urlmap.getLayers();
			$scope.link = urlmap.getLink();
			$location.search("mappings", mappings);
			$location.search("layers", layers);
		} else {
			$location.search("mappings", null);
			$location.search("layers", null);
		}
	};	
	
	/**
	 * createLink - copys the current url to the clipboard and informs the users about it.
	 *
	 */
	$scope.createLink = function() {
		$scope.copyToClipboard('https://app.sdsd-projekt.de' + $scope.link);
		alert("Link copied to clipboard");
	};
	
	/**
	 * on(colorpicker-closed) - listener that fires when the colorpicker-closed event is triggered. Changes the color of the selected layer and updates the url after.
	 *
	 */
	$scope.$on('colorpicker-closed', function(event, colorObject){
		$scope.layers[event.targetScope.$index].setStyle({ color: colorObject.value });
		$scope.models.lists.activeObjects[event.targetScope.$index].color = colorObject.value;
		$scope.updateUrl();
	});
	
	/**
	 * changeColor - changes the models color
	 *
	 * @param color list index
	 * @param model preferred field
	 */
	$scope.changeColor = function(color, model){
		model.setStyle({ color: color.value });
	};

	/**
	 * initMapLegend - creates the legend info panel and adds it to the leaflet map
	 *
	 */
	$scope.initMapLegend = function() {
		$scope.legend = L.control({position: 'bottomright'});

		$scope.legend.onAdd = function (map) {

			var div = L.DomUtil.create('div', 'info legend');
			labels = ['<strong>Categories</strong>'],
			categories = ['Min','Max', 'Constant', 'Anomaly', 'no Data'];
			
			for (var i = 0; i < categories.length; i++) {
				div.innerHTML +=
					'<i class="circle" style="background:' + $scope.getColor(categories[i]) + '"></i> <span>' +
					categories[i] + '</span>' + (categories[i + 1] ? '<br>' : '');
			};
			return div;
		};
		
		// legend toggle button
		$scope.showLegend = true;  // default value showing the legend
		
		// adds checkbox that can toggle the visibilityinfo panel
		$scope.checkbox = L.control({position: 'bottomright'});

		$scope.checkbox.onAdd = function (map) {

			var div = L.DomUtil.create('div', 'legend');
			div.innerHTML += '<div class="custom-control custom-checkbox"><input data-ng-click="toggleLegend()" type="checkbox" checked class="custom-control-input" id="switch"><label class="custom-control-label" for="switch">toggle legend</label></div>';
			
			var angularElement = angular.element(div);
			var linkFunction = $compile(angularElement);
			var newScope = $scope.$new();
			newScope.toggleLegend = $scope.toggleLegend;
			var el = linkFunction(newScope);
			
			return el[0];
		};
		$scope.checkbox.addTo($scope.map);
		$scope.legend.addTo($scope.map);
	}
	 
	/**
	 * init - called on startup. Intializes a leaflet map with 2 tile layers and a map legend
	 *
	 */
	$scope.init = function () {
		$scope.setTileLayers();		
		$scope.map = L.map('map', {
			preferCanvas: true,
			center: [0, 0],
			zoom: 0,
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
		
		// adds map legend
		$scope.initMapLegend();
	};
	
	/**
	 * copyToClipboard - copys string to clipboard
	 *
	 */
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
	/**
	 * watch('sharedMapData.updated') - listener for changes of the sharedMapData.updated attribute. Triggers the processing of the data queue
	 *
	 */
	$scope.$watch('sharedMapData.updated', function () {
		if(sharedMapData.queue_map_data.length > 0)
			$scope.updateActiveObject(()=> $scope.updateMap());
	});
	
	/**
	 * updateActiveObject - pops the top object of the map data queue, add it to the leaflet map and updates the url after
	 *
	 * @param callback function that gets executed after the functions body
	 */
	$scope.updateActiveObject = function (callback) {
		while(sharedMapData.queue_map_data.length > 0){
			let activeObject = sharedMapData.queue_map_data.pop()
			let newFigure = {"file": activeObject.file, "label": activeObject.figure.label, "id": activeObject.figure.id || activeObject.figure.name, "type": activeObject.figure.type, "activeDDI": activeObject.figure.activeDDI, "visible": activeObject.figure.visible, "index": activeObject.figure.index};
			if(newFigure.visible == undefined)
				newFigure.visible = true;
			// calculating new object index
			let index;

			for(var i = $scope.models.lists.activeObjects.length; i >= 0; i--){
				if(newFigure.index == undefined || i==0 || $scope.models.lists.activeObjects[i-1].index == undefined || $scope.models.lists.activeObjects[i-1].index < newFigure.index){
					$scope.models.lists.activeObjects.splice(i, 0, newFigure);
					index = i;
					break;
				}
			}
			
			//inserting new layer
			$scope.layers.splice(index,0,activeObject.layer);
			$scope.layers[index].visible = newFigure.visible;
			if(activeObject.figure.color){
				newFigure.color = activeObject.figure.color;
				$scope.layers[index].setStyle({ color: newFigure.color });
			}
			if (activeObject.figure.stdColor){
				newFigure.stdColor = activeObject.figure.stdColor;
			}
			if(activeObject.figure.relativeValues){
				newFigure.relativeValues = activeObject.figure.relativeValues;
				for(let value in newFigure.relativeValues){
					if ($scope.models.lists.relativeValues[value])
						$scope.models.lists.relativeValues[value]++;
					else {
						$scope.models.lists.relativeValues[value] = 1;
					}
				}
			}
			$scope.layers[index].listItem = $scope.models.lists.activeObjects[index];
			$scope.layers[index].on('click', function() {
				$scope.layerOnClick(this.listItem);
			});
		}
		$scope.updateUrl();
		if (typeof callback === "function") {
			callback();
		}
	};
	
	/**
	 * layerOnClick - listener that gets fired when the chosen layer is clicked. The coresponding list entry get highlighted and the list automatically scrolls to it.
	 *
	 * @param listEntry list entry that represents the clicked layer
	 */
	$scope.layerOnClick = function (listEntry) {
		$scope.$apply(function(){
			$scope.models.selected = listEntry;
		});
		$scope.$apply(function(){
			let div = document.getElementsByClassName("selected")[0];
			div.scrollIntoView({ behavior: 'smooth' });
		});
	};
	
	/**
	 * deleteLayer - removes the entry from the list and the layer from the map. Updates the URL after.
	 *
	 * @param index index of the model
	 */
	$scope.deleteLayer = function (index) {
		for(let value in $scope.models.lists.activeObjects[index].relativeValues){
			if ($scope.models.lists.relativeValues[value])
				$scope.models.lists.relativeValues[value]--;
				if($scope.models.lists.relativeValues[value] == 0){
					delete $scope.models.lists.relativeValues[value];
				}
		}
		if($scope.map.hasLayer($scope.layers[index]))
			$scope.map.removeLayer($scope.layers[index]);
		$scope.layers.splice(index, 1);
		$scope.models.lists.activeObjects.splice(index,1);
		$scope.updateUrl();
	};
	
	/**
	 * hide - toggles the visibility of the given figure and removes or adds the corresponding layer to the map.
	 *
	 * @param figure figure
	 * @param layer corresponding map layer
	 */
	$scope.hide = function (figure, layer) {
		if(layer){
			if(layer.visible){
				if($scope.map.hasLayer(layer))
					$scope.map.removeLayer(layer);
				layer.visible = false;
				figure.visible = false;
			}
			else{
				layer.visible = true;
				figure.visible = true;
				$scope.addLayersToMap($scope.layers.indexOf(layer));
			}
		}
		$scope.updateUrl();
	};
	
		
	/**
	 * center - centers the map on the layers bounds
	 *
	 * @param layer layer with bounds
	 */
	$scope.center = function (layer) {
		if(layer && layer.visible){
			$scope.map.fitBounds(layer.getBounds());
		}
	};
	
	/**
	 * dropLayer - function that is called when the drag and drop action ends. Changes the lists indices around and updates the URL after to fit the new list state.
	 *
	 * @param newIndex list index in which the item is droped
	 * @param item droped item
	 */
	$scope.dropLayer = function (newIndex, item) {
		let oldIndex = $scope.models.dragStartIndex;
		if(oldIndex != newIndex && oldIndex + 1 != newIndex && oldIndex != null){
			$scope.layers.splice(newIndex, 0, $scope.layers[oldIndex]);
			$scope.models.lists.activeObjects.splice(newIndex, 0, $scope.models.lists.activeObjects[oldIndex]);
			if(oldIndex > newIndex)
				oldIndex += 1;
			if(oldIndex < newIndex)
				newIndex -= 1;
			$scope.layers.splice(oldIndex, 1);
			$scope.models.lists.activeObjects.splice(oldIndex, 1);
			$scope.models.dragStartIndex = null;
			$scope.addLayersToMap(newIndex);
			$scope.updateUrl();
		}
	};
		
	/**
	 * addLayersToMap - adds all visible layers after and including the given index to the map.
	 *
	 * @param index index from which to start. 0 for all layer.
	 */
	$scope.addLayersToMap = function (index) {
		for (let layer of $scope.layers.slice(index)){
			if(layer.visible){
				if($scope.map.hasLayer(layer))
					$scope.map.removeLayer(layer);
				layer.addTo($scope.map);
			}
		}
	};
	
	/**
	 * dragStart - function that is called when the drag and drop action starts. Saves the list index of the dragged item
	 *
	 * @param startIndex list index of the dragged item
	 */
	$scope.dragStart = function (startIndex){
		$scope.models.dragStartIndex = startIndex
	};
	
	/**
	 * updateMap - updates the layer to represent the current scope.layers array and fits the map on the new bounds
	 *
	 */
	$scope.updateMap = function () {
		let bounds = null;
		for (let [index, activeObject] of $scope.models.lists.activeObjects.entries()){
			let layer = $scope.layers[index];
			if(layer && layer.visible){
				if(!$scope.map.hasLayer(layer)){
					layer.addTo($scope.map);
				}
				if(bounds){bounds.extend(layer.getBounds())}
				else {bounds = layer.getBounds();}
			}
		}
		if(bounds)
			$scope.map.fitBounds(bounds);
	};
	
	/**
	 * coloringSelected - changes the color of all layers that have the given relative value attribute. Layers without it stay unchanged.
	 *
	 */
	$scope.coloringSelected = function(){
		let valueKey = $scope.models.selectedColoring;
		for(index in $scope.models.lists.activeObjects){
			let object = $scope.models.lists.activeObjects[index];
			// layers without relativeValues remain unchanged
			if(object.relativeValues){
				let layer = $scope.layers[index];
				let newColor = 'grey';
				if(valueKey === 'default' && object.stdColor)
					newColor = object.stdColor;
				else if(object.relativeValues[valueKey] && object.relativeValues[valueKey].color){
					 newColor = object.relativeValues[valueKey].color;
				}
				layer.setStyle({ color: newColor });
			}
		}
	};
	
	/**
	 * getColor - returns a hex color that represents the given value category. Not allowed values result in grey.
	 *
	 * @param d string enum representing a value category. Allowed values are Min, Max, Constant, Anomaly.
	 * @returns hex color that represents the given value category. Not allowed values result in grey.
	 */
	$scope.getColor = function(d){
		let minColor = jQuery.Color({ hue: 120, saturation: 1, lightness: 0.5 });
		let maxColor = jQuery.Color({ hue: 0, saturation: 1, lightness: 0.5 });
		
		return 	d === 'Min'  ? minColor.toHexString() :
				d === 'Max'  ? maxColor.toHexString() :
				d === 'Constant'  ? '#3388ff' :
				d === 'Anomaly' ? "#a332ff" :
					"grey";
	};
	
	/**
	 * toggleLegend - toggles the visibility of the legend info panel.
	 *
	 */
	$scope.toggleLegend = function() {
		if($scope.showLegend === true){
			$scope.legend.remove();
			$scope.showLegend = false; 
		}else{
			$scope.legend.addTo($scope.map);
			$scope.showLegend = true;
		}
	};
	
});