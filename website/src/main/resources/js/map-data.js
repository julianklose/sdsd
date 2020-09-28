/**
 * app module
 *
 * @param sdsd sdsd module
 */
angular.module('app.map', ['sdsd']);
/**
 * app.map.factory(sharedMapData) factory that returns the sharedMapData queue. Shares data between the map controller and the map-data controller
 *
 */
angular.module('app.map').factory('sharedMapData', function () {	
	return  {
		queue_map_data: [],
		updated: 0,
		mapLoading: 0
	};
});
/**
 * UrlDecoder - decodes mapping and layers into figures.
 * @param mappings mappings
 * @param layers map layers
 */
class UrlDecoder {
	constructor(mappings, layers) {
		let map = mappings.split("~");
		this.figures = [];
		
		for(let layer of layers.split("~")) {
			let info = layer.split("-");
			if(info.length == 5) {
				let figure = { visible: true };
				switch(info[0]) {
					case "HG":
						figure.visible = false;
					case "G":
						figure.type = "geometry";
						break;
					case "HT":
						figure.visible = false;
					case "T":
						figure.type = "timelog";
						break;
					case "HD":
						figure.visible = false;
					case "D":
						figure.type = "grid";
						break;
				}
				figure.fileId = decodeURIComponent(map[info[1]]);
				figure.id = decodeURIComponent(map[info[2]]);
				if(info[3] !== "")
					figure.activeDDI = decodeURIComponent(map[info[3]]);
				if(info[4] !== "")
					figure.color = "#" + info[4];
				figure.index = this.figures.length;
				this.figures.push(figure);
			}
		}
	}
/**
 * getFigures - returns the decoded figures
 *
 * @returns figure - decoded figures
 */
	getFigures() {
		return this.figures;
	}
}
/**
 * map-data Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 * @param filter prvoides the filter 
 * @param location service that parses the browser URL and makes it available
 * @param sharedMapData provides shared Data between the map controller and the map.data controller
 * @author Henrik Oltmanns, Hochschule Osnabrueck
 */
angular.module('app.map').controller('data', function ($scope, $location, sdsd, $filter, sharedMapData) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	// list of the current user files
	$scope.filelist = [];
	// list of loaded figures
	$scope.figures = [];
	// types of figures that can be selected
	$scope.types = [
		{type: 'geometry', label: 'Geometries', checked: false, collapsed: false},
		{type: 'grid', label: 'Grids', checked: false, collapsed: false},
		{type: 'timelog', label: 'Timelogs', checked: false, collapsed: false}];
	// subtypes of figure that can be selected
	$scope.subtypes = [
		{subtype: 'Other', label: 'Others', checked: false, collapsed: false},
		{subtype: 'GuidancePattern', label: 'Guidance Pattern', checked: false, collapsed: false},
		{subtype: 'TreatmentZone', label: 'Treatment Zone', checked: false, collapsed: false},
		{subtype: 'Field', label: 'Field', checked: false, collapsed: false},
		{subtype: 'TimeLog', label: 'Timelog', checked: false, collapsed: false},
		{subtype: 'FildAccess', label: 'Field Access', checked: false, collapsed: false}];
	
	// queue where data shared with the map controller is stored.
	$scope.sharedMapData = sharedMapData;

	// listener that triggers when the username changes. Loads the current users files and clears old data belonging to other users
	$scope.$watch('sdsd.username', function (newVal) {
		if(newVal) {
			$scope.loadFileList($scope.executeQuery);
		}
		else {
			$scope.filelist = [];
		}
		delete $scope.fileid;
		delete $scope.file;
	});
	
/**
 * executeQuery() - extracts mapping, layers and fid parameters from the current url and decodes them. Loads them after.
 *
 */
	$scope.executeQuery = function() {
		let mappings = $location.search().mappings;
		let layers = $location.search().layers;
		let fid = $location.search().fid;
		if(mappings && layers) {
			let urlmap = new UrlDecoder(mappings, layers)
			for(let figure of urlmap.getFigures()) {
				if(figure.fileId) {
					let file = $scope.filelist.filter(file => file.id === figure.fileId)[0];
					if(file){
						switch(figure.type) {
							case "geometry":
								figure.loadGeoJson = $scope.getGeometry;
								break;
							case "timelog":
								figure.loadGeoJson = $scope.getTimelog;
								figure.loadDdiList = $scope.getTimelogDDIs;
								figure.name = figure.id;
								figure.label = figure.name;
								break;
							case "grid":
								figure.loadDdiList = $scope.getGridDDIs;
								figure.loadGeoJson = $scope.getGrid;
								figure.name = figure.id;
								figure.label = figure.name;
								break;
							case "deault":
								break;
						}
						let before = Promise.resolve();
						if(figure.activeDDI) {
							if(figure.loadDdiList)
								before = figure.loadDdiList(file, figure);
						}
						if(figure.loadGeoJson)
							before.then(() => figure.loadGeoJson(file, figure));
					}
				}
			}
		}
	};
	
	/**
	 * layerSelected - returns the number of selected layers.
	 *
	 * @returns activeLayers.length  integer representing the number of selected layers
	 */
	$scope.layerSelected = function() {
		var layersSelected = $filter("filter")($scope.figures, {
			active: true
		});
		return layersSelected.length;
	}
	
	/**
	 * createLayer - creates a new layer from geojson
	 *
	 * @returns layer that was created
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
		
	/**
	 * addData - loads the geojson from all figures
	 *
	 */
	$scope.addData = function() {
		for(let figure of $scope.figures) {
			if(figure.active)
				figure.loadGeoJson($scope.file, figure);
		}
	};
	
	/**
	 * addMapDataToSharedQueue - adds the given data to the shared queue
	 *
	 * @param file string name of the parent file
	 * @param figure figure
	 * @param layer map layer representing the figure
	 *
	 */
	$scope.addMapDataToSharedQueue = function(file, figure, layer) {
		$scope.sharedMapData.queue_map_data.push({file: file, figure: figure, layer: layer});
		$scope.sharedMapData.updated++;
	}
	
	/**
	 * createDdiList - creates a ddi list from a ddi map
	 *
	 * @param ddimap map of ddis
	 * @returns list of ddis orderd by group labels
	 *
	 */
	$scope.createDdiList = function(ddimap) {
		if(ddimap) {
			let groups = {};
			for(let key in ddimap) {
				let group = ddimap[key].groups ? ddimap[key].groups.join(' - ') : " ";
				if(!groups[group]) groups[group] = { label: group, list: [] };
				groups[group].list.push({ value:key, label:ddimap[key].label });
			}
			let out = [];
			for(let group in groups) {
				groups[group].list.sort(function(a, b) { return a.label.localeCompare(b.label) });
				out.push(groups[group]);
			}
			out.sort(function(a, b) { return a.label.localeCompare(b.label) });
			return out.length > 0 ? out : undefined;
		}
		return undefined;
	};

	/**
	 * loadFileList - starts a remote call, which return the users files.
	 *
	 * @param callback callback function that is called when after the remote call finishes
	 *
	 */
	$scope.loadFileList = function(callback) {
		sdsd.rpcCall("map", "listMapFiles", [], function(data) {
			$scope.filelist = data.files;
			if(callback)
				callback();
		});
	};
	
	/**
	 * watch('file') - listener that fires when the current selected file changes. Resets the checked and collapsed items and lists the file content.
	 *
	 *
	 */
	$scope.$watch('file', function(file) {
		$scope.figures = [];
		for(let t of $scope.types){
			t.checked = false;
			t.collapsed = false;
		}
		for(let st of $scope.subtypes){
			st.checked = false;
			st.collapsed = false;
		}
		if(file)
			$scope.listMapContent(file);
	}), true;

	/**
	 * ('#modalAddLayer').on('hidden.bs.modal'
	 *
	 *
	 */
	$('#modalAddLayer').on('hidden.bs.modal', function (e) {
		$scope.$apply(function(){
			delete $scope.file;
			$scope.types.forEach(t => t.checked = false);
		});
	})

	/**
	 * selectFigure - loads the ddiList of the selected Figure
	 *
	 * @param figure selected figure
	 */
	$scope.selectFigure = function(figure) {
		var type = $scope.types.filter(i => i.type === figure.type)[0];
		type.checked = false;
		$scope.loadDdiList(figure);
	};
	
	/**
	 * loadDdiList - loads the ddiList of the selected Figure
	 *
	 * @param figure selected figure
	 */
	$scope.loadDdiList = function(figure) {
		if(figure && figure.loadDdiList && figure.active && figure.ddiList == undefined){
			figure.loadDdiList($scope.file, figure);
		}
	};
	
	/**
	 * selectType - toogles the active attribute from all figures belonging to a type.
	 *
	 * @param type the type can be a subtype or a type.
	 */
	$scope.selectType = function(type) {
		let figures;
		if(type.type){
			figures = $scope.figures.filter(i => i.type === type.type);
				if(type.type == 'geometry'){
					for(let st of $scope.subtypes){st.checked = type.checked;}
				}
		}
		else if(type.subtype)
			figures = $scope.figures.filter(i => i.subtype === type.subtype);
			
		for(let figure of figures){
			figure.active = type.checked;
			if(figure.active) $scope.loadDdiList(figure);
		}
	};
	
	/**
	 * updateTooltips - updates the layers with the corresponding figures ddi and color information
	 *
	 * @param figure geo figure with ddi and color informatitons
	 * @param layers layers corresponding to the figure
	 */
	$scope.updateTooltips = function(figure, layers) {
		
		layers.eachLayer(function(layer) {
			let p = layer.feature.properties;
			if(p) {
				let tt = '';
				if(p.time) tt += $filter("date")(p.time, 'medium');
				
				let ddi = figure.activeDDI;
				if(ddi) {
					if(p[ddi]) {
						if(tt) tt += "<br/>";
						tt += p[ddi].label;
						if(figure.ddimap && figure.ddimap[ddi]) {
							let v = p[ddi].value, 
								min = figure.ddimap[ddi].min, 
								max = figure.ddimap[ddi].max;
							layer.setStyle({ color: $scope.getColorTint(v, min, max)});
						} else 
							layer.setStyle({ color: "grey" });
					} else 
						layer.setStyle({ color: "grey" });
				}
				else {
					for(let key in p) {
						if(key === 'color'){
							layer.setStyle({ color: p.color })
							figure.stdColor = p.color;
						}
						if(key === 'time' || typeof p[key] === 'object' || p[key] === '') continue;
						if(tt) tt += "<br/>";
						tt += key + ": " + p[key];
						if(figure.relativeValues && figure.relativeValues[key]){
							let v = p[key], 
								min = figure.relativeValues[key].min, 
								max = figure.relativeValues[key].max;
							figure.relativeValues[key].color = $scope.getColorTint(v,min,max);							
						}
					}			
				}
				if(tt) layer.bindTooltip(tt);
				else layer.unbindTooltip();
			}
		});
	};
	
	/**
	 * updateTooltips - updates the layers with the corresponding figures ddi and color information
	 *
	 * @param figure geo figure with ddi and color informatitons
	 * @param layers layers corresponding to the figure
	 */
	$scope.getColorTint = function(value, min, max){
		if(min == max) // constant
			return "#3388ff";
		else if(value == 0 && value < min) // no value
			return "grey";
		else if(value < min || value > max) // anomaly
			return "#a332ff";
		else {
			let relval = (value - min) / (max - min);
			let color = jQuery.Color({ hue: (1 - relval) * 120, saturation: 1, lightness: 0.5 });
			return color.toHexString();
		}
	}
	/**
	 * stopLoading - decreases the count of loading processes
	 *
	 */
	$scope.stopLoading = function() {
		if($scope.sharedMapData.mapLoading > 0)
			--$scope.sharedMapData.mapLoading;
	};
	
	/**
	 * getTimefilter
	 * @returns current timefilter applied to the map
	 *
	 */
	$scope.getTimefilter = function(map) {
		let timefilter = null;
		if(!map.timefilter) {
			return null;
			/*
			map.timefilter = {
					from: $filter('date')(map.from, 'yyyy-MM-ddTHH:mm:ss'),
					until: $filter('date')(map.until, 'yyyy-MM-ddTHH:mm:ss')
			};
			*/
		}
			
		timefilter = {};
		if(map.timefilter.from) {
			let val = new Date(map.timefilter.from);
			if(isNaN(val.getTime())) {
				alert("Invalid 'from' date");
				return;
			}
			timefilter.from = val.toISOString();
		}
		if(map.timefilter.until) {
			let val = new Date(map.timefilter.until);
			if(isNaN(val.getTime())) {
				alert("Invalid 'until' date");
				return;
			}
			timefilter.until = val.toISOString();
		}
		return timefilter;
	};
	
	/**
	 * listMapContent - starts a remote call that lists the files geometries, grids and timelogs
	 * @param file selected file
	 *
	 */	
	$scope.listMapContent = function(file) {
		++$scope.sharedMapData.mapLoading;
		sdsd.rpcCall("map", "listMapContent", [file.id], function(data) {
			for(const geo of data.geometries) {
				geo.loadGeoJson = $scope.getGeometry;
				geo.subtype = geo.type;
				geo.type = "geometry";
				geo.activeDDI = null;
				$scope.figures.push(geo);
			}
			
			for(const grid of data.grids) {
				grid.label = grid.name;
				grid.loadDdiList = $scope.getGridDDIs;
				grid.loadGeoJson = $scope.getGrid;
				grid.type = "grid";
				grid.activeDDI = null;
				$scope.figures.push(grid);
			}
			for(const tlg of data.timelogs) {
				tlg.label = tlg.name + " (" + tlg.count + ")";
				tlg.loadDdiList = $scope.getTimelogDDIs;
				tlg.loadGeoJson = $scope.getTimelog;
				tlg.type = "timelog";
				tlg.activeDDI = null;
				$scope.figures.push(tlg);
			}
		}, $scope.stopLoading);
	};
	
	/**
	 * getGridDDIs - starts a remote call that lists the grids corresponding ddis
	 * @param file selected file
	 * @param grid selected grid
	 *
	 */	
	$scope.getGridDDIs = function(file, grid) {
		++$scope.sharedMapData.mapLoading;
		if(grid.name) {
			return new Promise(function(resolve, reject) {
				sdsd.rpcCall("map", "getGridDDIs", [file.id, grid.name], function(data) {
					grid.ddimap = data;
					grid.ddiList = $scope.createDdiList(data);
				}, function() {
					$scope.stopLoading();
					resolve();
				}, function(error) {
					reject(error);
				});
			});
		}
		return Promise.resolve();
	};

	/**
	 * getTimelogDDIs - starts a remote call that lists the grids corresponding timelogs
	 * @param file selected file
	 * @param grid selected grid
	 *
	 */		
	$scope.getTimelogDDIs = function(file, timelog) {
		++$scope.sharedMapData.mapLoading;
		return new Promise(function(resolve, reject) {
			sdsd.rpcCall("map", "getTimelogDDIs", [file.id, timelog.name], function(data) {
				timelog.ddimap = data;
				timelog.ddiList = $scope.createDdiList(data);
			}, function() {
				$scope.stopLoading();
				resolve();
			}, function(error) {
				reject(error);
			});
		});
	};
	
	/**
	 * getGrid - starts a remote call that gets more infos about the selected grid
	 * @param file selected file
	 * @param grid selected grid
	 *
	 */		
	$scope.getGrid = function(file, grid) {
		if(grid.name && grid.activeDDI) {
			let ddi = null;
			let grid_tmp = grid;
			if(grid.ddimap && grid.activeDDI && grid.ddimap[grid.activeDDI]) {
				ddi = grid.ddimap[grid.activeDDI];
			}
			++$scope.sharedMapData.mapLoading;
			sdsd.rpcCall("map", "getGrid", [file.id, grid.name, grid.activeDDI], function(data) {
				if(ddi) {
					grid_tmp.label += " | " + ddi.label;
					ddi.min = data.min;
					ddi.max = data.max;
				}
				let layer = $scope.createLayer(data.geojson);
				$scope.updateTooltips(grid_tmp, layer);
				$scope.addMapDataToSharedQueue(file, grid_tmp, layer);
			}, $scope.stopLoading);
		}
	};
	
	
	/**
	 * getTimelogDDIs - starts a remote call that gets more infos about the selected timelog
	 * @param file selected file
	 * @param timelog selected timelog
	 *
	 */	
	$scope.getTimelog = function(file, timelog) {
		if(timelog.name) {
			let ddi = null;
			if(timelog.ddimap && timelog.activeDDI && timelog.ddimap[timelog.activeDDI]) {
				ddi = timelog.ddimap[timelog.activeDDI];
			}
			++$scope.sharedMapData.mapLoading;
			sdsd.rpcCall("map", "getTimelog", [file.id, timelog.name, timelog.activeDDI, $scope.getTimefilter(timelog), -1], function(data) {
				let tl = timelog;
				if(ddi) {
					tl.label += " | " + ddi.label;
					ddi.min = data.min;
					ddi.max = data.max;
				}
				let layer = $scope.createLayer(data.geojson);
				$scope.updateTooltips(timelog, layer);
				$scope.addMapDataToSharedQueue(file, tl, layer);
			}, $scope.stopLoading);
		}
	};
	/**
	 * getTimelogDDIs - starts a remote call that gets more infos about the selected geometry:
	 * @param file selected file
	 * @param geometry selected geometry
	 *
	 */	
	$scope.getGeometry = function(file, geometry) {
		if(geometry.id) {
			++$scope.sharedMapData.mapLoading;
			let geo = geometry;
			sdsd.rpcCall("map", "getGeometry", [file.id, geometry.id], function(data) {
				let layer = $scope.createLayer(data.geojson);
				if(data.geojson.properties.label)
					geo.label = data.geojson.properties.label;
				if(data.relativeValues)
					geo.relativeValues = data.relativeValues;
				$scope.updateTooltips(geo, layer);
				$scope.addMapDataToSharedQueue(file, geo, layer);
			}, $scope.stopLoading);
		}
	};
	
});