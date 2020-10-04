'use strict';

/**
 * Class encapsulating business logic for the SDSD Dashboard.
 * Instantiated in app controller <code>dashboard</code>.
 *
 * @class 
 */
class Dashboard {

	/**
	 * Creates a Dashboard instance and initiates its object variables.
	 * Passes the sdsd API to the <code>DashboardData</code> class
	 * containing only RPC calls to the SDSD endpoints.
	 *
	 * @param {Object} sdsd - Connectivity to the SDSD API.
	 *
	 * @constructor
	 */
	constructor(sdsd) {
		this._data = new DashboardData(sdsd);
		this._map = null;
		this._bbox = null;
		this._allFields = new Map();
		this._candidateFields = [];
		this._selectedField = null;
		this._machineTypes = new Map();
	}

	/**
	 * Getter for the Dashboard's data interface.
	 * @return {DashboardData} Dashboard's data interface. 
	 */
	get data() {
		return this._data;
	}

	/**
	 * Getter for the Leaflet map.
	 * @return {Object} Leaflet map object. 
	 */
	get map() {
		return this._map;
	}

	/**
	 * Getter for the bounding box of all fields.
	 * @return {Object} Bounding box of all fields. 
	 */
	get bbox() {
		return this._bbox;
	}

	/**
	 * Getter for all fields.
	 * @return {Map} Leaflet ID maps to field object. 
	 */
	get allFields() {
		return this._allFields;
	}

	/**
	 * Getter for candidate fields.
	 * @return {Object[]} Candidate fields.
	 */
	get candidateFields() {
		return this._candidateFields;
	}

	/**
	 * Getter for selected field.
	 * @return {Object} Selected field.
	 */
	get selectedField() {
		return this._selectedField;
	}

	/**
	 * Getter for machine types.
	 * @return {Map} Machine serialnumber maps to machine type.
	 */
	get machineTypes() {
		return this._machineTypes;
	}

	/**
	 * Setter for Leaflet map.
	 * @param {Object} map - Leaflet map.
	 */
	set map(map) {
		this._map = map;
	}

	/**
	 * Setter for bounding box of all fields.
	 * @param {Object} bbox - Bounding box of all fields.
	 */
	set bbox(bbox) {
		this._bbox = bbox;
	}

	/**
	 * Setter for all fields.
	 * @param {Map} allFiels - All fields mapping Leaflet ID to field object.
	 */
	set allFields(allFields) {
		this._allFields = allFields;
	}

	/**
	 * Setter for candidate fields.
	 * @param {Object[]} candidateFields - Candidate fields.
	 */
	set candidateFields(candidateFields) {
		this._candidateFields = candidateFields;
	}

	/**
	 * Setter for selected field.
	 * @param {Object} selectedField - Selected field.
	 */
	set selectedField(selectedField) {
		this._selectedField = selectedField;
	}

	/**
	 * Setter for machine types.
	 * @param {Map} machineTypes - Machine types mapping serialnumber to machine type.
	 */
	set machineTypes(machineTypes) {
		this._machineTypes = machineTypes;
	}

	/**
	 * Extracts the field URIs from the feature properties of a Leaflet layer (field).
	 * @param {Object} field - Field
	 * @return {string[]} Field URIs
	 */
	static getFieldIds(field) {
		return field.feature.properties.uri;
	}

	/**
	 * Extracts the field and graph URIs from the feature properties of a Leaflet layer (field).
	 * @param {Object} field - Field
	 * @return {Object} - Field and Graph URIs
	 */
	static getFieldUrisAndGraphs(field) {
		return field.feature.properties.res;
	}

	/**
	 * Extracts the labels of a field from its properties.
	 * @param {Object} field - Field
	 * @return {string[]} Field labels
	 */
	static getFieldLabels(field) {
		return field.properties.label;
	}

	/**
	 * Extracts the field area from the feature properties.
	 * @param {Object} field - Field
	 * @return {number} - Field area in square meters.
	 */
	static getFieldArea(field) {
		return field.feature.properties.area;
	}

	/**
	 * Extracts the coordinates (points) of a field polygon.
	 * @param {Object} field - Field
	 * @return {LatLng[]} Coordinates of the field polygon.
	 */
	static getFieldCoords(field) {
		return field.geometry.coordinates[0];
	}

	/**
	 * Creates a two column info table. The first column contains the attribute names,
	 * the second its values. Displayed attributes can be filtered by the means of the 
	 * <code>filter</code> parameter.
	 * @param {Object[]} dbRows - Rows of a database query.
	 * @param {string[]} filter - Attribute identifiers to be filtered out.
	 * @param {string=} responsive - Type of info table responsiveness.
	 * @return {(Table|string)} - Info table object or no data string. 
	 */
	static createInfoTable(dbRows, filter = [], responsive = 'table-responsive-lg') {
		let element = NO_DATA;

		if (!emptyGroupBy(dbRows, 'values')) {
			element = new Table({
				header: ['Attribute', 'Values'],
				striped: false,
				responsive: responsive
			});

			for (const dbRow of dbRows) {
				const attribute = dbRow.attribute;
				const values = dbRow.values;
				if (values && !filter.includes(attribute)) {
					element.addRow([
						new Link({
							href: dbRow.attributeUri,
							text: attribute
						}),
						values
					]);
				}
			}
		}

		return element;
	}

	/**
	 * Creates an overview table for a TimeLog.
	 * @param {string} tlgLabel - Label used as a header for the foldable UI element.
	 * @param {Object} tlgProps - Attributes of a TimeLog to be shown in the overview table.
	 * @param {Object} numberMap - Contains on the deepest nesting level the TimeLog object.
	 * @return {Table} Overview table for TimeLog.
	 */
	static createTimeLogOverviewTable(tlgLabel, tlgProps, numberMap) {
		const tlgOverviewTable = new Table({
			header: ['Attribute', 'Values'],
			responsive: 'table-responsive-sm'
		});

		const tlgObj = Dashboard.getTlgObj(numberMap);
		for (const prop in tlgProps) {
			let value = tlgObj[prop];

			if (value) {
				switch (prop) {
					case 'duration':
						value = ((value / 60).toFixed(2)) + ' min.'
						break;
					case 'start':
					case 'stop':
					case 'from':
					case 'until':
						value = dateFormat(value);
						break;
				}
			}

			tlgOverviewTable.addRow([tlgProps[prop], value ?? NA]);
		}

		return tlgOverviewTable;
	}

	/**
	 * Creates the header for the second layer of foldable 
	 * UI elements inside a TimeLog or DDI card.
	 * @param {Object} ddiMap - Maps DDIs to TimeLog rows.
	 * @return {string} Header for the second layer of foldable UI elements.
	 */
	static createNumberSummary(ddiMap) {
		const ddiObj = values(ddiMap)[0][0];
		return `${ddiObj.number} - ${ddiObj.label} (${ddiObj.typeLabel})`;
	}

	/**
	 * Creates a two column table for DDI attributes and their values.
	 * Links DDIs to the SDSD Map view.
	 * @param {Object} ddiMap - Maps DDIs to TimeLog rows.
	 * @param {Object} ddiProps - Attributes of a DDI to be shown in the DDI table.
	 * @param {string} graphId - Graph ID of the encompassing TimeLog needed for map link generation.
	 * @param {string} tlgLabel - Label of the TimeLog needed for map link generation.
	 * @return {Table} Table of DDIs sorted by the DDI number.
	 */
	static createDdiTable(ddiMap, ddiProps, graphId, tlgLabel) {
		const ddiTable = new Table({
			header: ['DDI'].concat(values(ddiProps))
		});

		if (hasProp(values(ddiMap)[0][0], 'dlv'))
			ddiTable.header.push('Details');

		for (const ddiUri in ddiMap) {
			const ddiArray = ddiMap[ddiUri];
			for (const ddiObj of ddiArray) {
				const ddiLink = new Link({
					href: ddiUri,
					text: ddiFromUri(ddiUri)
				});

				const ddiRow = [ddiLink];
				for (const prop in ddiProps) {
					let value = ddiObj[prop] ?? NA;
					if (value !== NA) {
						switch (prop) {
							case 'value':
								value = (((+ddiObj.value) + (ddiObj.offset ?? 0)) * (ddiObj.scale ?? 1)).toFixed(ddiObj.numberOfDecimals ?? 2);
								break;
						}
					}
					ddiRow.push(value);
				}

				if (hasProp(ddiObj, 'tlgname') && hasProp(ddiObj, 'dlv')) {
					const ddiLink = new Link({
						href: timeLogLink(graphId, tlgLabel, ddiObj.dlv),
						text: 'Map'
					});
					ddiRow.push(ddiLink);
				}

				ddiTable.addRow(ddiRow);
			}
		}
		ddiTable.rows.sort((a, b) => a[0].text - b[0].text);
		return ddiTable;
	}

	/**
	 * Extracts the TimeLog object from the deepest nesting level of a <code>numberMap</code>.
	 * @param {Object} - Map of a TLG number to an array of corresponding DDIs, further referring to a TLG object.
	 * @return {Object} - TimeLog object from the deepest nesting level of <code>numberMap</code>. 
	 */
	static getTlgObj(numberMap) {
		const numberMapValues = values(numberMap);
		if (!numberMapValues.length) return {};
		const firstNumberMapValue = numberMapValues[0];
		const valuesOfNumberMapValues = values(firstNumberMapValue);
		if (!valuesOfNumberMapValues.length) return {};
		const firstValueOfNumberMapValues = valuesOfNumberMapValues[0];
		if (!firstValueOfNumberMapValues.length) return {};
		return firstValueOfNumberMapValues[0];
	}

	/**
	 * Checks wether the deepest object inside a <code>numberMap</code> belongs to a TimeLog.
	 * @param {Object} - Map of a TLG number to an array of corresponding DDIs, further referring to a TLG object.
	 * @return {bool} <code>True</code> if the deepest object belongs to a TimeLog, <code>false</code> otherwise. 
	 */
	static hasParentTimeLog(numberMap) {
		return hasProp(Dashboard.getTlgObj(numberMap), 'tlgname');
	}

	/**
	 * Creates foldable table for number and DDI details.
	 * In case of a TimeLog, this UI element will also contains the overview table for that TimeLog.
	 * @param {Object[]} timeRows - TimeLog rows returned from database by RPC call.
	 * @param {Object} tlgProps - TimeLog attributes to be shown as overview.
	 * @param {Object} ddiProps - DDI attributes to be shown in a DDI table.
	 * @param {string} graphId - Graph ID of the corresponding TimeLog.
	 * @param {bool} [containsTimeLog=true] - Indicates whether the <code>timeRows</code> belong to an actual TimeLog.
	 * @return {Details[]} Collection of foldable UI elements with a depth of two.
	 */
	static createTimeLogDetails(timeRows, tlgProps, ddiProps, graphId, containsTimeLog = true) {
		let tlgMap;
		if (containsTimeLog)
			tlgMap = Dashboard.groupTimeLogData(timeRows);
		else
			tlgMap = {
				numbersOnly: Dashboard.groupByNumber(timeRows)
			};

		const tlgDetails = [];
		for (const tlgLabel in tlgMap) {
			const numberMap = tlgMap[tlgLabel];

			// 1. Create TLG Detail which is pushed into tlgDetails
			const tlgLink = Dashboard.hasParentTimeLog(numberMap) ? ' (' + new Link({
				href: timeLogLink(graphId, tlgLabel),
				text: 'Show on Map'
			}) + ')' : '';

			// This tlgDetail gets the number Details pushed into its 'element' member
			const tlgDetail = new Details({
				summary: tlgLabel + tlgLink,
				element: [],
				$class: 'pb-2'
			});

			// 2. Create TLG overview table
			if (containsTimeLog) {
				const tlgOverviewTable = Dashboard.createTimeLogOverviewTable(tlgLabel, tlgProps, numberMap);
				tlgDetail.element.push(tlgOverviewTable);
			}

			// 3. Create Number Details
			for (const number in numberMap) {
				const ddiMap = numberMap[number];
				// This numberDetail gets the DDI Tables pushed into its 'element' member
				const numberDetail = new Details({
					summary: Dashboard.createNumberSummary(ddiMap),
					element: [],
					$class: containsTimeLog ? 'pl-3' : 'pb-2'
				});

				// 4. Create ddiTables which are shown as dropdown of Number Details
				const ddiTable = Dashboard.createDdiTable(ddiMap, ddiProps, graphId, tlgLabel);
				numberDetail.element.push(ddiTable);

				if (containsTimeLog)
					tlgDetail.element.push(numberDetail);
				else
					tlgDetails.push(numberDetail);
			}

			if (containsTimeLog)
				tlgDetails.push(tlgDetail);
		}

		return tlgDetails;
	}

	/**
	 * Groups TimeLog data rows into one object.
	 * The following mapping structure is generated: <code>tlgName</code> maps to <code>row.number</code> maps to <code>row.ddi</code> maps to <code>row</code>.
	 * @param {Object[]} rows - TimeLog data rows from database.
	 * @return {Object} Grouped TimeLog object.
	 */
	static groupTimeLogData(rows) {
		const obj = {};
		for (const row of rows) {
			const tlgName = row.tlgname ?? dateFormat(row.from);

			if (!hasProp(obj, tlgName))
				obj[tlgName] = {};

			if (!hasProp(obj[tlgName], row.number))
				obj[tlgName][row.number] = {};

			if (!hasProp(obj[tlgName][row.number], row.ddi))
				obj[tlgName][row.number][row.ddi] = [];

			obj[tlgName][row.number][row.ddi].push(row);
		}
		return obj;
	}

	/**
	 * Groups data rows containing a <code>number</code> attribute.
	 * The following mapping structure is generated: <code>row.number</code> maps to <code>row.ddi</code> maps to <code>row</code>.
	 * @param {Object[]} rows - Rows from database containing <code>number</code> attribute.
	 * @return {Object} Object grouped by <code>number</code> attribute.
	 */
	static groupByNumber(rows) {
		const obj = {};
		for (const row of rows) {
			if (!hasProp(obj, row.number))
				obj[row.number] = {};

			if (!hasProp(obj[row.number][row.ddi], row.ddi))
				obj[row.number][row.ddi] = [];
			obj[row.number][row.ddi].push(row);
		}
		return obj;
	}

	/**
	 * Creates the Leaflet map with satellite and OSM map layers.
	 * Centers map using the center coordinate of the bounding box
	 * of all fields.
	 */
	createMap() {
		if (this.map) return;

		const standard = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
		});

		const satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
			attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
		});

		this.map = L.map(replace(DASH_MAP_CLASS, {
			'.': ''
		}), {
			center: this.bbox ? this.bbox.getCenter() : [52.283784, 8.023442],
			zoom: 14,
			layers: [satellite]
		});

		const baseMaps = {
			"Aerial Image": satellite,
			"Open Street Map": standard
		};

		L.control.layers(baseMaps).addTo(this.map);
	}

	/**
	 * Calculates and sets the bounding box of all fields.
	 * Loop through all fields and record the most extreme
	 * coordinates for bottom left and top right.
	 * @param {Object[]} fields - All fields.
	 */
	calcMapBoundingBox(fields) {
		if (this.bbox) return;

		if (empty(fields)) {
			this.bbox = null;
			return;
		}

		const bbox = {
			minLat: Infinity,
			minLon: Infinity,
			maxLat: -Infinity,
			maxLon: -Infinity
		};

		for (const field of fields) {
			const coords = Dashboard.getFieldCoords(field);

			for (const coord of coords) {
				const lon = coord[0];
				const lat = coord[1];
				if (lat < bbox.minLat) bbox.minLat = lat;
				if (lon < bbox.minLon) bbox.minLon = lon;
				if (lat > bbox.maxLat) bbox.maxLat = lat;
				if (lon > bbox.maxLon) bbox.maxLon = lon;
			}
		}

		this.bbox = new L.LatLngBounds([
			[bbox.maxLat, bbox.maxLon],
			[bbox.minLat, bbox.minLon]
		]);
	}

	/**
	 * Clears HTML containers defined by <code>TO_CLEAR</code>, the map, all field polygons and their bounding box.
	 */
	clear() {
		for (const elem of TO_CLEAR)
			clear(elem);

		if (this.map && this.map.remove) {
			this.map.off();
			this.map.remove();
			this.map = null;
			setAttr(DASH_MAP_CLASS, 'class', replace(DASH_MAP_CLASS, {
				'.': ''
			}));
		}

		this.bbox = null;
		this.allFields = new Map();
	}

	/**
	 * Closes all open popups of the map.
	 */
	closePopups() {
		if (this.map)
			this.map.eachLayer(layer => layer.closePopup());
	}

	/**
	 * Invalidates the current map size allowing it to reflow into its current (resized) HTML container. Map is then zoomed to fit a bounding box defined by <code>bounds</code>.
	 * @param {LatLngBounds} bounds - Bounding box to fit map to. 
	 */
	refreshMap(bounds) {
		this.invalidateMapSize();
		this.fitMapToBounds(bounds);
	}

	/**
	 * Invalidates the current map size.
	 */
	invalidateMapSize() {
		if (this.map)
			this.map.invalidateSize();
	}

	/**
	 * Zooms map to fit a specified bounding box.
	 * @param {LatLngBounds} bounds - Bounding box to fit map to. 
	 */
	fitMapToBounds(bounds) {
		if (!this.map || !bounds) return;

		if (bounds.getBounds)
			bounds = bounds.getBounds();

		this.map.fitBounds(bounds);
	}

	/**
	 * Zooms and centers map depending on the center of the bounding box of <code>fields</code>.
	 * @param {Object[]} fields - Field polygons for bounding box calculation.
	 */
	showMapCenterZoomed(fields) {
		this.calcMapBoundingBox(fields);
		this.createMap();

		this.map.on('load', removeLoading(DASH_MAP_LARGE_ID));
		this.fitMapToBounds(this.bbox);
	}

	/**
	 * Sets and highlights the currently selected field polygon. A selection may contain multiple fields if they are layered above each other. 
	 * @param {string} leafletId - Leaflet field ID of the selected field.
	 */
	setSelectedField(leafletId) {
		this.resetCandidateFields();
		this.selectedField = this.allFields.get(+leafletId)._layers[leafletId - 1];
		this.candidateFields.push(this.selectedField);
		this.setCandidateFieldColor();
	}

	/**
	 * Adds field polygon feature (geometry) to map. 
	 * @param {Object} feature - Polygon feature to add to map.
	 */
	showFeature(feature) {
		feature.properties.graph = Array.from(new Set(feature.properties.graph));
		const addedFeature = L.geoJSON(feature).addTo(this.map);
		const leafletId = addedFeature._leaflet_id;

		const labels = Dashboard.getFieldLabels(feature);
		if (!empty(labels.length)) {
			const popupNav = new Icon({
				name: 'arrow-right-short',
				size: 24,
				$class: replace(DASH_POPUP_NAV_CLASS, {
					'.': ''
				}) + ' pointer ml-1',
				data: DATA_LEAFLETID + '="' + leafletId + '"'
			});
			const popupHtml = join(labels) + popupNav;
			addedFeature.bindPopup(popupHtml, {
				autoClose: false,
				maxWidth: 350
			});
		}

		this.allFields.set(leafletId, addedFeature);
	}

	/** Loops through all field geometries, displays them on the map and registers their click handlers.
	 * @param {Object[]} fields - Field geometries to display. 
	 */
	showFields(fields) {
		for (const field of fields)
			this.showFeature(field);

		this.allFields.forEach(field => this.fieldMapClickHandler(field));
	}

	/**
	 * Creates and displays scrollable list of all fields. List entries are clickable.
	 * This list consists of all the names of the fields, which are also shown on the map.
	 */
	showFieldList() {
		const items = [];
		this.allFields.forEach((field, leafletId) => {
			const labels = field._layers[leafletId - 1].feature.properties.label;

			if (labels.length > 1)
				labels[0] = new Bold({
					text: first(labels)
				});

			const item = new ListItem({
				element: join(labels),
				data: DATA_LEAFLETID + '="' + leafletId + '"'
			});

			items.push(item);
		});

		const searchInput = new Input({
			id: replace(DASH_MAP_FIELDLIST_SEARCH_ID, {
				'#': ''
			}),
			placeholder: 'Search for field'
		});

		const fieldList = new List({
			id: replace(DASH_MAP_FIELDLIST_FIELDS_ID, {
				'#': ''
			}),
			items: !empty(items) ? items : [NO_DATA]
		});

		fieldList.items.unshift(searchInput);
		html(DASH_MAP_FIELDLIST_ID, fieldList);
	}

	/**
	 * Resolves hexadecimal client names of machines.
	 * @param {Object[]} dataRows - Machine rows from the database.
	 * @param {string} hexCol - Attribute name of the client name in base 16.
	 * @return {Promise<Object[]>} Machine rows with extra attribute <code>machineType</code>.
	 */
	resolveMachineClientName(dataRows, hexCol) {
		const p = [];
		for (const row of dataRows) {
			const hex = Array.isArray(row[hexCol]) ? row[hexCol][0] : row[hexCol];
			if (!this.machineTypes.has(hex)) {
				const machineType = this.data.getMachineType(hex);
				this.machineTypes.set(hex, machineType);
			}

			p.push(this.machineTypes.get(hex));
		}

		return Promise.all(p).then(machineTypes => {
			dataRows.forEach((dataRow, i) => {
				dataRow.machineType = machineTypes[i];
			});
			return dataRows;
		});
	}

	/**
	 * Creates and displays the table of tasks.
	 */
	showTasksTable() {
		showLoading(DASH_TASKS_TABLE_ID);

		const fieldRes = Dashboard.getFieldUrisAndGraphs(this.selectedField);
		this.data.getTasks(fieldRes).then(taskRows => {
			if (empty(taskRows)) return;

			return this.resolveMachineClientName(taskRows, 'clientname');
		}).then(taskRows => {
			let table = NO_DATA;

			const headers = {
				'label': 'Name',
				'start': 'Start',
				'customer': 'Customer',
				'dvcDesignator': 'Machines',
				'serialnumber': 'Serial numbers',
				'machineType': 'Types',
			};

			if (taskRows) {
				const taskMap = new Map();
				for (const taskRow of taskRows) {
					const taskRef = JSON.stringify({
						task: taskRow.task,
						graph: taskRow.graph
					});

					if (!taskMap.has(taskRef))
						taskMap.set(taskRef, []);

					taskMap.get(taskRef).push(taskRow);
				}

				table = new Table({
					header: values(headers).concat(['Details'])
				});

				taskMap.forEach((taskRows, taskRef) => {
					taskRef = JSON.parse(taskRef);
					const mergedRow = {};

					for (const header in headers) {
						mergedRow[header] = [];
						for (const taskRow of taskRows) {
							const value = taskRow[header] ?? NA;
							if (!mergedRow[header].includes(value))
								mergedRow[header].push(value);
						}
					}

					const row = [];
					for (const header in headers) {
						let values = mergedRow[header];

						if (header === 'start') {
							values.sort((a, b) => a.localeCompare(b));
							values = values.splice(0, 1);
						}

						values.forEach((value, i) => {
							switch (header) {
								case 'machineType':
									values[i] = new Link({
										href: value.value,
										text: value.label
									});
									break;
								case 'start':
									values[i] = dateFormat(value);
									break;
							}
						});
						row.push(join(values, '<br />'));
					}

					row.push(new Icon({
						name: 'arrow-right-short',
						size: 32,
						$class: replace(DASH_TASK_NAV_CLASS, {
							'.': ''
						}) + ' pointer',
						data: `${DATA_TASKID}="${taskRef.task}" ${DATA_TASKGRAPH}="${taskRef.graph}"`
					}));

					table.addRow(row);
				});
			}

			const tasksCard = new Card({
				title: 'Tasks',
				elements: [table]
			});

			html(DASH_TASKS_TABLE_ID, tasksCard);
		});
	}

	/**
	 * Creates and displays the field information.
	 */
	showFieldInfo() {
		showLoading(DASH_FIELD_INFO_ID);
		showLoading(DASH_MAP_SMALL_ID, true);

		const fieldRes = Dashboard.getFieldUrisAndGraphs(this.selectedField);
		this.data.getFieldInfo(fieldRes).then(fieldInfoRows => {
			for (const row of fieldInfoRows) {
				if (row.attribute === 'partfieldArea') {
					row.values = Math.round(Dashboard.getFieldArea(this.selectedField)) + ' m²';
					break;
				}
			}

			const element = Dashboard.createInfoTable(fieldInfoRows, ['partfieldCode', 'partfieldId'], 'table-responsive');

			const fieldInfo = new Card({
				title: 'Field',
				elements: [element]
			});

			html(DASH_FIELD_INFO_ID, fieldInfo);

			this.showMapZoomed();
		});
	}

	/**
	 * Displays the label of a task as the header of the detailed task view.
	 * @param {string} taskLabel - Task label to show as header.
	 */
	showSelectedTaskLabel(taskLabel) {
		html(DASH_DETAILS_TASKLABEL_ID, new Row({
			elements: [new Header({
				text: capitalize(taskLabel)
			})]
		}));
	}

	/**
	 * Creates and displays the worker information on the detailed task view.
	 * @param {string} taskId - ID of the task the worker belongs to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showWorkerInfo(taskId, graphId) {
		showLoading(DASH_DETAILS_WORKERINFO_ID);

		this.data.getWorkerInfo(taskId, graphId).then(workerInfoRows => {
			const table = Dashboard.createInfoTable(workerInfoRows);

			const workerInfo = new Card({
				title: 'Worker',
				elements: [table]
			});

			html(DASH_DETAILS_WORKERINFO_ID, workerInfo);
		});
	}

	/**
	 * Creates and displays the customer information on the detailed task view.
	 * @param {string} taskId - ID of the task the customer belongs to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showCustomerInfo(taskId, graphId) {
		showLoading(DASH_DETAILS_CUSTOMERINFO_ID);

		this.data.getCustomerInfo(taskId, graphId).then(customerInfoRows => {
			const table = Dashboard.createInfoTable(customerInfoRows);

			const customerInfo = new Card({
				title: 'Customer',
				elements: [table]
			});

			html(DASH_DETAILS_CUSTOMERINFO_ID, customerInfo);
		});
	}

	/**
	 * Creates and displays the farm information on the detailed task view.
	 * @param {string} taskId - ID of the task the farm belongs to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showFarmInfo(taskId, graphId) {
		showLoading(DASH_DETAILS_FARMINFO_ID);
		this.data.getFarmInfo(taskId, graphId).then(farmInfoRows => {
			const table = Dashboard.createInfoTable(farmInfoRows);

			const farmInfo = new Card({
				title: 'Farm',
				elements: [table]
			});

			html(DASH_DETAILS_FARMINFO_ID, farmInfo);
		});
	}

	/**
	 * Creates and displays the TIM (times) data information on the detailed task view.
	 * @param {string} taskId - ID of the task the TIM belongs to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showTimes(taskId, graphId) {
		showLoading(DASH_DETAILS_TIMES_ID);

		this.data.getTimes(taskId, graphId).then(timeRows => {
			const tlgProps = {
				'from': 'From',
				'until': 'Until',
				'duration': 'Duration',
				'status': 'Status'
			};

			const ddiProps = {
				'designator': 'Designator',
				'value': 'Value',
				'unit': 'Unit'
			};

			const timeLogTotalDetails = Dashboard.createTimeLogDetails(timeRows, tlgProps, ddiProps, graphId);
			const timeLogTotalsCard = new Card({
				title: 'Times',
				elements: empty(timeLogTotalDetails) ? [NO_DATA] : timeLogTotalDetails
			});

			html(DASH_DETAILS_TIMES_ID, timeLogTotalsCard);
		});
	}

	/**
	 * Creates and displays the TimeLog information on the detailed task view.
	 * @param {string} taskId - ID of the task the TimeLogs belong to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showTaskTimeLogs(taskId, graphId) {
		showLoading(DASH_DETAILS_TASKTIMES_ID);

		this.data.getTaskTimeLogs(taskId, graphId).then(timeRows => {
			const tlgProps = {
				'from': 'From',
				'until': 'Until',
				'count': 'Count'
			};

			const ddiProps = {
				'designator': 'Designator',
				'unit': 'Unit'
			};

			const taskTimeLogsDetails = Dashboard.createTimeLogDetails(timeRows, tlgProps, ddiProps, graphId);
			const taskTimeLogsCard = new Card({
				title: 'Time Logs',
				elements: empty(taskTimeLogsDetails) ? [NO_DATA] : taskTimeLogsDetails
			});

			html(DASH_DETAILS_TASKTIMES_ID, taskTimeLogsCard);
		});
	}

	/**
	 * Creates and displays the product allocation information on the detailed task view.
	 * @param {string} taskId - ID of the task the product allocation belongs to.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 */
	showProductAllocationInfo(taskId, graphId) {
		showLoading(DASH_DETAILS_PRODUCTALLOCATIONINFO_ID);

		this.data.getProductAllocationInfo(taskId, graphId).then(productAllocationRows => {
			let table = NO_DATA;

			if (!empty(productAllocationRows)) {
				const headers = {
					'name': 'Product',
					'value': 'Quantity',
					'group': 'Group'
				};

				table = new Table({
					header: values(headers)
				});

				for (const productAllocationRow of productAllocationRows) {
					const row = [];
					for (const header in headers) {
						let value = productAllocationRow[header];
						if (value) {
							switch (header) {
								case 'value':
									value += ' ' + (productAllocationRow.unit ?? '');
									break;
							}
						} else {
							value = NA;
						}
						row.push(value);
					}
					table.addRow(row);
				}
			}

			const productAllocationCard = new Card({
				title: 'Product Allocations',
				elements: [table]
			});

			html(DASH_DETAILS_PRODUCTALLOCATIONINFO_ID, productAllocationCard);
		});
	}

	/**
	 * Creates and displays the machines table in its own tab on the home screen.
	 */
	showMachinesTable() {
		showLoading(DASH_MACHINES_TABLE_ID);

		this.data.getAllMachines().then(allMachinesRows => {
			let table = NO_DATA;

			if (!empty(allMachinesRows)) {
				const headers = {
					'label': 'Name',
					'serialnumber': 'Serial number',
					'system': 'System',
					'function': 'Function',
					'group': 'Group',
					'res': 'Details'
				};

				table = new Table({
					header: values(headers)
				});

				for (const machineRow of allMachinesRows) {
					const row = [];
					for (const header in headers) {
						let value = machineRow[header];
						if (value) {
							switch (header) {
								case 'res':
									value = new Icon({
										name: 'arrow-right-short',
										size: 32,
										$class: replace(DASH_MACHINE_NAV_CLASS, {
											'.': ''
										}) + ' pointer',
										data: `${DATA_MACHINERES}='${JSON.stringify(value)}'`
									});
									break;
								case 'system':
								case 'function':
								case 'group':
									value = new Link({
										href: value.value,
										text: value.label
									});
									break;
								case 'label':
									value = !empty(value) ? join(value) : NA;
									break;
							}
						} else {
							value = NA;
						}
						row.push(value);
					}
					table.addRow(row);
				}
			}

			const machinesCard = new Card({
				title: 'Machines',
				elements: [table]
			});

			html(DASH_MACHINES_TABLE_ID, machinesCard);
		});
	}


	/** 
	 * Displays all UI elements (cards) of the detailed task view.
	 * @param {string} taskId - ID of the encompassing task.
	 * @param {string} graphId - ID of the graph the task belongs to.
	 * @param {string} taskLabel - Label of the task extracted from the first column of the task table.
	 */
	showTaskDetails(taskId, graphId, taskLabel) {
		hide(DASH_FIELD_TASK_INFO_ID);

		setAttr(DASH_NAV_BACK_ID, DATA_TARGET, 'tasks');
		show(DASH_DETAILS_ID);

		this.showSelectedTaskLabel(taskLabel);
		this.showWorkerInfo(taskId, graphId);
		this.showCustomerInfo(taskId, graphId);
		this.showFarmInfo(taskId, graphId);
		this.showTimes(taskId, graphId);
		this.showTaskTimeLogs(taskId, graphId);
		this.showProductAllocationInfo(taskId, graphId);
	}

	/**
	 * Displays all UI elements (cards) of the detailed machine view.
	 * @param {Object} machineRes - Machine and graph URIs of the selected machine.
	 * @param {string} machineLabel - Label of the selected machine extracted from the first column of the machines table.
	 */
	showMachineDetails(machineRes, machineLabel) {
		hide(DASH_NAV_TABS_ID);
		hide(DASH_MACHINES_TABLE_ID);

		setAttr(DASH_NAV_BACK_ID, DATA_TARGET, 'machines');
		show(DASH_NAV_BACK_ID);
		show(DASH_MACHINE_DETAILS_ID);

		this.showSelectedMachineLabel(machineLabel);
		this.showDeviceElementInfo(machineRes);
		this.showDeviceTimeLogs(machineRes);
		this.showDeviceElementProperties(machineRes);
		this.showDeviceElementProcessData(machineRes);
	}

	/**
	 * Displays the label of the selected machine as the header of the detailed machine view.
	 * @param {string} machineLabel - Label of the machine to show as header.
	 */
	showSelectedMachineLabel(machineLabel) {
		html(DASH_MACHINE_DETAILS_MACHINELABEL_ID, new Row({
			elements: [new Header({
				text: capitalize(machineLabel)
			})]
		}));
	}

	/**
	 * Creates and displays the device element information on the detailed machine view.
	 * @param {Object} machineRes - Machine and graph URIs of the selected machine.
	 */
	showDeviceElementInfo(machineRes) {
		showLoading(DASH_MACHINE_DETAILS_DEVICEELEMENTINFO_ID);

		this.data.getDeviceElements(machineRes).then(deviceElementRows => {
			let table = NO_DATA;

			if (!empty(deviceElementRows)) {
				const headers = {
					'name': 'Name',
					'typeLabel': 'Type'
				};

				table = new Table({
					header: values(headers)
				});

				for (const deviceElementRow of deviceElementRows) {
					const row = [];
					for (const header in headers)
						row.push(deviceElementRow[header]);
					table.addRow(row);
				}
			}

			const deviceElementsCard = new Card({
				title: 'Device Elements',
				elements: [table]
			});

			html(DASH_MACHINE_DETAILS_DEVICEELEMENTINFO_ID, deviceElementsCard);
		});
	}

	/**
	 * Creates and displays the device element property information on the detailed machine view.
	 * @param {Object} machineRes - Machine and graph URIs of the selected machine.
	 */
	showDeviceElementProperties(machineRes) {
		showLoading(DASH_MACHINE_DETAILS_DEVICEELEMENTPROPERTIES_ID);

		this.data.getDeviceElementProperties(machineRes).then(deviceElementPropertyRows => {
			const ddiProps = {
				'name': 'Designator',
				'value': 'Value',
				'unit': 'Unit'
			};

			const deviceElementPropertyDetails = Dashboard.createTimeLogDetails(deviceElementPropertyRows, undefined, ddiProps, machineRes[0].graph, false);
			const deviceElementPropertyCard = new Card({
				title: 'Device Element Properties',
				elements: empty(deviceElementPropertyDetails) ? [NO_DATA] : deviceElementPropertyDetails
			});

			html(DASH_MACHINE_DETAILS_DEVICEELEMENTPROPERTIES_ID, deviceElementPropertyCard);
		});
	}

	/**
	 * Creates and displays the device element process data information on the detailed machine view.
	 * @param {Object} machineRes - Machine and graph URIs of the selected machine.
	 */
	showDeviceElementProcessData(machineRes) {
		showLoading(DASH_MACHINE_DETAILS_DEVICEELEMENTPROCESSDATA_ID);

		this.data.getDeviceElementProcessData(machineRes).then(deviceElementProcessDataRows => {
			const ddiProps = {
				'name': 'Designator',
				'unit': 'Unit',
				'property': 'Property',
				'trigger': 'Trigger'
			};

			const deviceElementProcessDataDetails = Dashboard.createTimeLogDetails(deviceElementProcessDataRows, undefined, ddiProps, machineRes[0].graph, false);
			const deviceElementProcessDataCard = new Card({
				title: 'Device Element Process Data',
				elements: empty(deviceElementProcessDataDetails) ? [NO_DATA] : deviceElementProcessDataDetails
			});

			html(DASH_MACHINE_DETAILS_DEVICEELEMENTPROCESSDATA_ID, deviceElementProcessDataCard);
		});
	}

	/**
	 * Creates and displays the TimeLogs information of a device on the detailed machine view.
	 * @param {Object} machineRes - Machine and graph URIs of the selected machine.
	 */
	showDeviceTimeLogs(machineRes) {
		showLoading(DASH_MACHINE_DETAILS_TIMELOGINFO_ID);

		this.data.getDeviceTimeLogs(machineRes).then(timeRows => {
			const tlgProps = {
				'from': 'From',
				'until': 'Until',
				'count': 'Count'
			};

			const ddiProps = {
				'designator': 'Designator',
				'unit': 'Unit'
			};

			const graphId = machineRes.length ? machineRes[0].graph : undefined;
			const machineTimeLogDetails = Dashboard.createTimeLogDetails(timeRows, tlgProps, ddiProps, graphId);

			const machineTimeLogCard = new Card({
				title: 'Time Logs',
				elements: empty(machineTimeLogDetails) ? [NO_DATA] : machineTimeLogDetails
			});

			html(DASH_MACHINE_DETAILS_TIMELOGINFO_ID, machineTimeLogCard);
		});
	}

	/**
	 * Navigates to the home view and restores the DOM to its starting configuration.
	 */
	navHome() {
		historyReplaceState(HASH_HOME);
		hide(DASH_NAV_BACK_ID);
		hide(DASH_FIELD_TASK_INFO_ID);
		hide(DASH_DETAILS_ID);
		move(DASH_MAP_ID, DASH_MAP_LARGE_ID);
		show(DASH_NAV_TABS_ID);
		show(DASH_MAP_FIELDLIST_ID);
		resetHeight(DASH_MAP_ID);

		this.selectedField = null;
		this.closePopups();
		this.resetCandidateFields();
		this.refreshMap(this.bbox);
	}

	/**
	 * Navigates to the task table view after selecting a field polygon on the map or field list.
	 */
	navTasks() {
		historyReplaceState(HASH_TASKS);
		setAttr(DASH_NAV_BACK_ID, DATA_TARGET, 'home');
		hide(DASH_DETAILS_ID);
		show(DASH_FIELD_TASK_INFO_ID);
		this.refreshMap(this.selectedField);
	}

	/**
	 * Navigates to the machine details view after selecting a machine from the machines table.
	 */
	navMachines() {
		historyReplaceState(HASH_HOME);
		setAttr(DASH_NAV_BACK_ID, DATA_TARGET, 'home');
		hide(DASH_NAV_BACK_ID);
		hide(DASH_MACHINE_DETAILS_ID);
		show(DASH_MACHINES_TABLE_ID);
		show(DASH_NAV_TABS_ID);
	}

	/**
	 * Navigation handler for the Back-Button.
	 */
	navBackClickHandler() {
		$(DASHBOARD_ID).on('click', DASH_NAV_BACK_ID, e => {
			const target = getAttr(e.currentTarget, DATA_TARGET);
			switch (target) {
				case 'home':
					this.navHome();
					break;
				case 'tasks':
					this.navTasks();
					break;
				case 'machines':
					this.navMachines();
					break;
			}
		});
	}

	/**
	 * Click handler for the "Fields" and "Machines" tabs.
	 */
	tabClickHandler() {
		$(DASHBOARD_ID).on('click', DASH_NAV_TABS_ID + ' .nav-tab', e => {
			hide(DASH_TAB_CLASS);
			removeClass(DASH_NAV_TABS_ID + ' .active', 'active');
			addClass(e.currentTarget, 'active');

			const tab = getAttr(e.currentTarget, DATA_TAB);
			switch (tab) {
				case 'fields':
					this.showFieldsTab();
					break;
				case 'machines':
					this.showMachinesTab();
					break;
			}
		});
	}

	/**
	 * Popstate handler for linking the browser's back functionality to the UI Back-Button of the dashboard.
	 */
	historyHandler() {
		window.addEventListener('popstate', () => {
			if (visible(DASH_NAV_BACK_ID))
				$(DASH_NAV_BACK_ID).trigger('click');
		});
	}

	/**
	 * Task details click handler for selecting and navigating to the detailed view of that task.
	 */
	taskClickHandler() {
		$(DASHBOARD_ID).on('click', DASH_TASK_NAV_CLASS, e => {
			const taskId = getAttr(e.currentTarget, DATA_TASKID);
			const graphId = getAttr(e.currentTarget, DATA_TASKGRAPH);
			historyPushState(HASH_TASK_DETAILS);
			const taskLabel = getRowLabel(e.currentTarget);
			this.showTaskDetails(taskId, graphId, taskLabel);
		});
	}

	/**
	 * Machine details click handler for selecting and navigating to the detailed view of that machine.
	 */
	machineClickHandler() {
		$(DASHBOARD_ID).on('click', DASH_MACHINE_NAV_CLASS, e => {
			historyPushState(HASH_MACHINE_DETAILS);
			const machineRes = JSON.parse(getAttr(e.currentTarget, DATA_MACHINERES));
			const machineLabel = getRowLabel(e.currentTarget);
			this.showMachineDetails(machineRes, machineLabel);
		})
	}

	/**
	 * Field list search input handler for filtering matching field labels.
	 */
	fieldListSearchHandler() {
		$(DASHBOARD_ID).on('input', DASH_MAP_FIELDLIST_SEARCH_ID, e => {
			const needleVal = $(e.currentTarget).val();
			search(needleVal, DASH_MAP_FIELDLIST_FIELDS_ID + ' a');
		});
	}

	/**
	 * Universal click handler for selecting a field either from the map or the field list.
	 * @param {Event} e - Click event triggered by the user. 
	 */
	fieldClickHandler(e) {
		historyPushState(HASH_TASKS);
		const leafletId = getAttr(e.currentTarget, DATA_LEAFLETID);
		this.setSelectedField(leafletId);
		this.showTasksView();
	}

	/**
	 * Click handler for selecting a field from the field list. Forwards click event to <code>fieldClickHandler</code>.
	 */
	fieldListClickHandler() {
		$(DASHBOARD_ID).on('click', DASH_MAP_FIELDLIST_ID + ' a', e => {
			this.fieldClickHandler(e);
		});
	}

	/**
	 * Click handler for selecting a field via the arrow inside the popup of a field polygon on the map. Forwards click event to <code>fieldClickHandler</code>. 
	 */
	fieldPopupClickHandler() {
		$(DASHBOARD_ID).on('click', 'img' + DASH_POPUP_NAV_CLASS, e => {
			this.fieldClickHandler(e);
		});
	}

	/**
	 * Click handler directly bound to the leaflet field polygon. Always triggered if such a polygon is clicked on the map. Uses the turf.js library to detect and highlight overlapping field geometries. Shows popups of every overlapping field.
	 * @param {Object} field - The leaflet field feature the click handler should be bound to.
	 */
	fieldMapClickHandler(field) {
		field.on('click', e => {
			this.closePopups();
			this.resetCandidateFields();

			this.allFields.forEach((otherField, otherLeafletId) => {
				const clickPos = turf.point([e.latlng.lng, e.latlng.lat]);
				const feature = otherField._layers[otherLeafletId - 1].feature;
				if (turf.booleanPointInPolygon(clickPos, feature)) {
					const centroid = turf.centroid(feature).geometry.coordinates;
					otherField.getPopup().setLatLng(L.latLng(centroid[1], centroid[0]));
					otherField.openPopup();
					this.candidateFields.push(otherField);
				}
			});

			this.setCandidateFieldColor();
		});
	}

	/**
	 * Removes the highlighting from candidate (overlapping) fields and cleears the corresponding array. A candidate field is a field offered to the user for selection using the right arrow in its popup. 
	 */
	resetCandidateFields() {
		for (const candidateField of this.candidateFields)
			candidateField.setStyle({
				color: '#3388ff'
			});
		this.candidateFields = [];
	}

	/**
	 * Highlights every candidate field polygon on the map in a predefined color (yellow).
	 */
	setCandidateFieldColor() {
		for (const candidateField of this.candidateFields) {
			candidateField.setStyle({
				color: 'yellow'
			});
			candidateField.bringToFront();
		}
	}

	/**
	 * Zooms, resizes and moves the map into the 50% width container of the task/field info view.
	 */
	showMapZoomed() {
		move(DASH_MAP_ID, DASH_MAP_SMALL_ID);
		removeLoading(DASH_MAP_SMALL_ID);
		show(DASH_MAP_ID);
		setHeight(DASH_MAP_ID, getHeight(DASH_FIELD_INFO_ID + ' .card'));
		this.refreshMap(this.selectedField);
	}

	/**
	 * Displays the task/field info view after the user has selected a field through the map or the field list.
	 */
	showTasksView() {
		if (!this.selectedField || empty(Dashboard.getFieldIds(this.selectedField))) return;

		this.closePopups();

		hide(DASH_NAV_TABS_ID);
		hide(DASH_MAP_ID);
		hide(DASH_MAP_FIELDLIST_ID);

		show(DASH_FIELD_TASK_INFO_ID);
		show(DASH_NAV_BACK_ID);

		this.showTasksTable();
		this.showFieldInfo();
	}

	/**
	 * Displays the map and field list after the user has clicked on the "Fields" tab. 
	 */
	showFieldsTab() {
		show(DASH_TAB_FIELDS_ID);
		this.invalidateMapSize();
		this.showFieldList();
	}

	/**
	 * Displays the machines table after the user has clicked on the "Machines" tab. 
	 */
	showMachinesTab() {
		show(DASH_TAB_MACHINES_ID);
		this.showMachinesTable();
	}

	/**
	 * Entry point of the dashboard for loading all the UI elements (cards, map, etc.) and registering all the event handlers.
	 */
	show() {
		show(DASH_TAB_FIELDS_ID);

		showLoading(DASH_MAP_LARGE_ID, true);
		showLoading(DASH_MAP_FIELDLIST_ID);

		this.data.getAllFields().then(fields => {
			this.showMapCenterZoomed(fields);
			this.showFields(fields);

			this.showFieldsTab();

			this.fieldListClickHandler();
			this.fieldPopupClickHandler();
			this.fieldListSearchHandler();
			this.navBackClickHandler();
			this.taskClickHandler();
			this.tabClickHandler();
			this.machineClickHandler();
			this.historyHandler();
		});
	}
}

app.controller('dashboard', ($scope, sdsd) => {
	$scope.sdsd = sdsd;

	$scope.init = () => $scope.dashboard = new Dashboard(sdsd);

	$scope.$watch('sdsd.username', newVal => {
		if (newVal) {
			$scope.dashboard.show();
		} else {
			$scope.dashboard.navHome();
			$scope.dashboard.clear();
		}
	});
});
