'use strict';

/**
 * Class to obtain data to be shown in the dashboard.
 *
 * @class
 */
class DashboardData {

	/**
	 * Constructor for the DashboardData class.
	 * With the sdsd parameter access to the API is given.
	 * @parameter {object} sdsd - Parameter to get access to the API.
	 *
	 * @constructor
	 */
	constructor(sdsd) {
		this._sdsd = sdsd;
	}

	/**
	 * Getter function for sdsd object.
	 */
	get sdsd() {
		return this._sdsd;
	}

	/**
	 * Functions to obtain all stored machines from the API.
	 * Machines sorted by serialnumber.
	 * @return {Promise} Promise with all machines.
	 */
	getAllMachines() {
		return new Promise(resolve => {
			this.sdsd.rpcCall('dashboard', 'getAllMachines', [], data => {
				data.machines.sort((a, b) => {
					const lhs = a.serialnumber;
					const rhs = b.serialnumber;
					if (lhs) return rhs ? lhs.localeCompare(rhs) : -1;
					if (rhs) return lhs ? rhs.localeCompare(lhs) : 1;
				});
				resolve(data.machines);
			});
		});
	}

	/**
	 * Function to get machine type by uri from API.
	 *
	 * @param {string} uri - Unique identifier for a machine.
	 * @return {Promise} Promise with requested machine type.
	 */
	getMachineTypeByUri(uri) {
		return new Promise(resolve => {
			this.sdsd.rpcCall('dashboard', 'getMachineTypeByUri', [uri], machineType => {
				resolve(machineType);
			});
		});
	}

	/**
	 * Function to obtain all fields.
	 * Fields are sorted by label.
	 * @return {Promise} Promise with all fields.
	 */
	getAllFields() {
		return new Promise(resolve => {
			this.sdsd.rpcCall('dashboard', 'getAllFields', [], data => {
				data.fields.sort((a, b) => {
					const labelA = join(Dashboard.getFieldLabels(a));
					const labelB = join(Dashboard.getFieldLabels(b));
					return labelA.localeCompare(labelB, 'de', {
						ignorePunctuation: true
					});
				});
				resolve(data.fields);
			});
		});
	}

	/**
	 * Function to obtain FieldInfo.
	 *
	 * @param {Array} res - Holds graph and uri information for API call.
	 * @return {Promise} - Promise with requested FieldInfo in JSON format.
	 *
	 */
	getFieldInfo(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['fieldinfo', {
					field: res
				}, 'application/json'],
				resp => resolve(resp.json.resultset)
			);
		});
	}

	/**
	 * Function to obtain WorkerInfo.
	 *
	 * @param {String} taskId - Id of the task containing the worker.
	 * @param {String} graphId - Id of the graph containing the worker.
	 * @return {Promise} - Promise with requested worker infos in JSON format.
	 *
	 */
	getWorkerInfo(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['workerinfo', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => resolve(resp.json.resultset)
			);
		});
	}

	/**
	 * Function to obtain customer info
	 *
	 * @param {String} taskId - Id of the task to get customer info for.
	 * @param {String} graphId - Id of the graph to get customer info.
	 * @return {Promise} - Promise with requested customer info in JSON format.
	 *
	 */
	getCustomerInfo(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['customerinfo', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => resolve(resp.json.resultset)
			);
		});
	}
	/**
	 * Function to obtain farm info.
	 *
	 * @param {String} taskId - Id of the task to get farm info for.
	 * @param {String} graphId - Id of the graph to get farm info.
	 * @return {Promise} - Promise with requested farm info in JSON format.
	 *
	 */
	getFarmInfo(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['farminfo', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => resolve(resp.json.resultset)
			);
		});
	}

	/**
	 * Function to obtain product allocation  info.
	 *
	 * @param {String} taskId - Id of the task to get product allocation info for.
	 * @param {String} graphId - Id of the graph to get product allocation info.
	 * @return {Promise} - Promise with requested product allocation info in JSON format.
	 *
	 */
	getProductAllocationInfo(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['productallocationinfo', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => resolve(resp.json.resultset)
			);
		});
	}

	/**
	 * Function to obtain machine type.
	 *
	 * @param {String} clientName - Client name to get machine type for.
	 * @return {Promise} - Promise with requested machine type.
	 *
	 */
	getMachineType(clientName) {
		return new Promise(resolve => {
			if (isHex(clientName)) {
				this.sdsd.rpcCall(
					'dashboard', 'getMachineType',
					[clientName],
					resp => resolve(resp.system)
				);
			} else {
				resolve({
					label: 'N/A',
					value: '#'
				});
			}
		});
	}

	/**
	 * Function to obtain task.
	 *
	 * @param {Array} res - Holds graph and uri information for API call.
	 * @return {Promise} - Promise with requested task as JSON.
	 *
	 */
	getTasks(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['tasks', {
					field: res
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	/**
	 * Function to get isoxml time values.
	 *
	 * @param {String} taskId - Holds id of task.
	 * @param {String} graphId - Holds id of grap.
	 * @return {Promise} - Promise with requested time values as JSON.
	 *
	 */
	getTimes(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['times', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	/**
	 * Function to obtain device elements.
	 *
	 * @param {Array} res - Holds graph and uri information for API call.
	 * @return {Promise} - Promise with requested device elements as JSON.
	 *
	 */
	getDeviceElements(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['deviceelements', {
					device: res
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	/**
	 * Function to obtain device element properties.
	 *
	 * @param {Array} res - Holds graph and uri information for API call.
	 * @return {Promise} - Promise with requested device element properties as JSON.
	 *
	 */
	getDeviceElementProperties(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['deviceproperties', {
					device: res
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	/**
	 * Function to obtain device element process data.
	 *
	 * @param {Array} res - Holds graph and uri information for API call.
	 * @return {Promise} - Promise with requested device element process data as JSON.
	 *
	 */
	getDeviceElementProcessData(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['deviceprocessdata', {
					device: res
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	/**
	 * Function to obtain TimeLogs for a specific machine.
	 *
	 * @param {String} res - Machine id and grapth uri to get TimeLogs for.
	 * @return {Promise} - Promise with requested TimeLogs.
	 *
	 */
	getDeviceTimeLogs(res) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get', ['devicetimelogs', {
					device: res
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}

	getTaskTimeLogs(taskId, graphId) {
		return new Promise(resolve => {
			this.sdsd.rpcCall(
				'sdb', 'get',
				['tasktimelogs', {
					'uri': taskId,
					'graph': graphId
				}, 'application/json'],
				resp => {
					removeTypes(resp.json.resultset);
					resolve(resp.json.resultset);
				}
			);
		});
	}
}
