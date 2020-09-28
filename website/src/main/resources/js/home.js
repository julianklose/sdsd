/**
 * SDSD client functions for the start page.
 * 
 * @file   Defines the angularJS home controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.controller('homeCtrl', function ($scope, sdsd) {
	$scope.sdsd = sdsd;
	
	$scope.filelist = [];
	$scope.types = [];
	$scope.endpoints = {all:[], sender:[], receiver:[]};
	$scope.sublist = [];
	$scope.loglist = [];
	
	$scope.onboarded = false;
	$scope.qa = false;
	$scope.expireDays = 365;
	
	$scope.onboardLoading = false;
	$scope.offboardLoading = false;
	$scope.endpointLoading = false;
	$scope.receiveLoading = false;

	$scope.storageConf = null;
	$scope.storageTasks = [];

	$scope.init = function () {
		$('.datepicker').datepicker({
			dateFormat: 'yy-mm-ddT00:00:00.00Z'
		});
				
		$scope.resetStorageTask();
	};

	$scope.$watch('sdsd.username', function(newVal) {
		if(newVal) {
			if(true) {
				sdsd.connectWebsocket().then(function(ws) {
					$scope.status();
					$scope.loadTypes();
					$scope.loadFileList();
					$scope.loadStorageTasks();
					$scope.loadLogList();
					
					if(ws) {
						sdsd.setListener("user", "log", null, function(params) {
							$scope.loglist.push(params[0]);
						});
						sdsd.setListener("file", "file", null, function(params) {
							let file = params[0];
							$scope.updateFileTypes(file);
							let i = $scope.filelist.findIndex(f => f.id == file.id);
							if(i >= 0) $scope.filelist[i] = file;
							else $scope.filelist.unshift(file);
						});
					}
				});
			} else {
				$scope.status();
				$scope.loadTypes();
				$scope.loadFileList();
				$scope.loadStorageTasks();
				$scope.loadLogList();
			}
		}
		else {
			$scope.onboarded = false;
			$scope.filelist = [];
			$scope.loglist = [];
			$scope.storageTasks = [];
			$scope.resetStorageTask();
			if(sdsd.websocket)
				sdsd.websocket.close(1000, "User logged out");
		}
	});
	
	$scope.$watch('onboarded', function(newVal) {
		if(newVal) {
			$scope.loadEndpointList();
			$scope.loadSubList();
			$scope.loadCapabilities();
		}
		else {
			$scope.endpoints = {all:[], sender:[], receiver:[]};
			$scope.sublist = [];
			$scope.caps = {capabilities:[], pushNotifications:0};
		}
	});
	
	$scope.allFilesSelected = false;
	$scope.selectAllFiles = function(checked) {
		for(let f of $scope.filelist) {
			f.selected = checked;
		}
	}
	$scope.$watch('allFilesSelected', $scope.selectAllFiles);
	
	$scope.status = function() {
		sdsd.rpcCall("agrirouter", "status", [], function(data) {
			$scope.onboarded = !!data.onboarded;
			$scope.qa = !!data.qa;
			$scope.mqtt = !!data.mqtt;
			$scope.expireDays = data.expireDays;
			if(data.onboarding)
				$scope.secureOnboard();
		});
	};
	
	$scope.loadLogList = function(error) {
		if(error) sdsd.message = error.message;
		sdsd.rpcCall("user", "listLogs", [], function(data) {
			$scope.loglist = data.logs;
		});
	};
	$scope.updateLogs = function(error) {
		if(!sdsd.websocket) 
			$scope.loadLogList(error);
		else if(error) 
			sdsd.message = error.message;
	}
	
	$scope.clearLogList = function() {
		sdsd.rpcCall("user", "clearLogs", [], function(data) {
			if(data.success)
				$scope.loglist = [];
		});
	};
	
	$scope.reconnect = function() {
		$scope.reconnecting = true;
		sdsd.rpcCall("agrirouter", "reconnect", [], null, function() {
			$scope.reconnecting = false;
		}, $scope.updateLogs);
	}
	
	$scope.reonboard = function() {
		sdsd.rpcCall("agrirouter", "reonboard", [], function(data) {
			if(data.success)
				window.location.href = data.redirectUrl;
		}, null, $scope.updateLogs);
	}
	
	$scope.startSecureOnboarding = function(qa, mqtt) {
		sdsd.rpcCall("agrirouter", "startSecureOnboarding", [!!qa, !!mqtt], function(data) {
			if(data.success)
				window.location.href = data.redirectUrl;
		}, null, $scope.updateLogs);
	}
	
	$scope.secureOnboard = function() {
		$scope.onboardLoading = true;
		sdsd.rpcCall("agrirouter", "agrirouterSecureOnboard", [], function(data) {
			if(data.success) {
				$scope.onboarded = true;
				$scope.loadEndpointList(true);
				$scope.status();
			}
		}, function() {
			$scope.updateLogs();
			$scope.onboardLoading = false;
		});
	}
	
	$scope.offboard = function() {
		if (confirm("Are you sure you want to delete the agrirouter onboarding?\nThis does also delete the endpoint in the agrirouter control center.")) {
			$scope.offboardLoading = true;
			sdsd.rpcCall("agrirouter", "agrirouterOffboard", [], function(data) {
				$scope.onboarded = !data.success;
			}, function() {
				$scope.updateLogs();
				$scope.offboardLoading = false;
			});
		}
	}
	
	$scope.loadEndpointList = function (update = false) {
		if(update) $scope.endpointLoading = true;
		sdsd.rpcCall("agrirouter", "listEndpoints", [update], function(data) {
			$scope.endpoints = data;
		}, function() {
			if(update) $scope.endpointLoading = false;
		}, $scope.updateLogs);
	};

	$scope.loadFileList = function() {
		sdsd.rpcCall("file", "listFiles", [], function(data) {
			$scope.filelist = data.files;
			$scope.updateFileTypes();
		}, null, $scope.updateLogs);
	};
	
	$scope.renameFile = function(f) {
		sdsd.rpcCall("file", "renameFile", [f.id, f.filename], function(data) {
			f.editName = false;
		}, $scope.updateLogs);
	}
	
	$scope.loadTypes = function () {
		sdsd.rpcCall("format", "listFormats", [], function(data) {
			$scope.types = data.formats;
			$scope.typeMap = {};
			for(let t of $scope.types) {
				$scope.typeMap[t.value] = t;
			}
			$scope.updateFileTypes();
		}, null, $scope.updateLogs);
	};
	
	$scope.updateFileTypes = function (f) {
		if($scope.filelist && $scope.filelist.length > 0 && $scope.types && $scope.types.length > 0) {
			if(f) {
				let t = $scope.typeMap[f.type];
				f.artype = t.artype;
				f.typename = t.label;
			} else {
				for(let f of $scope.filelist) {
					let t = $scope.typeMap[f.type];
					f.artype = t.artype;
					f.typename = t.label;
				}
			}
		}
	};
	
	$scope.saveFileType = function(f) {
		let t = $scope.types.find(t => t.value == f.type);
		f.artype = t.artype;
		f.typename = t.label;
		sdsd.rpcCall("file", "changeFileType", [f.id, f.type], function(data) {
			f.editType = false;
		}, $scope.updateLogs);
	}
	
	$scope.toggleCoreData = function(f) {
		f.coredata = !f.coredata;
		sdsd.rpcCall("file", "setCoreData", [f.id, f.coredata], null, $scope.updateLogs);
	}
		
	$scope.deleteFiles = function(files) {
		if(confirm("Are you sure you want to delete this file?")) {
			sdsd.rpcCall("file", "deleteFiles", [files], function(data) {
				if(data.count > 0)
					$scope.loadFileList();
			}, $scope.updateLogs);
		}
	};
	
	$scope.deleteSelectedFiles = function() {
		if (confirm("Are you sure you want to delete all selected files?")) {
			$scope.multideleteLoading = true;
			let files = $scope.filelist.filter(f => f.selected).map(f => f.id);
			sdsd.rpcCall("file", "deleteFiles", [files], function(data) {
				if(data.count > 0)
					$scope.loadFileList();
				$scope.allFilesSelected = false;
				$scope.multideleteLoading = false;
			}, $scope.updateLogs);
		}
	};
	
	$scope.checkCapabilities = function(endpoints) {
		if(endpoints) {
			$scope.filelist.forEach(f => f.cantSend = false);
			for(let e of $scope.endpoints.receiver) {
				if(!e.selectedSend || e.accepts.length == 0) continue;
				for(let f of $scope.filelist) {
					f.cantSend |= !e.accepts.includes(f.artype);
				}
			}
		}
		else {
			$scope.endpoints.receiver.forEach(e => e.cantSend = false);
			for(let f of $scope.filelist) {
				if(!f.selectedSend) continue;
				if(!f.artype) {
					alert("File type is unknown, please set the type manually.");
					f.selectedSend = false;
					continue;
				}
				for(let e of $scope.endpoints.receiver) {
					e.cantSend |= e.accepts.length > 0 && !e.accepts.includes(f.artype);
				}
			}
		}
	}
	
	$scope.sendFiles = function() {
		$scope.sendLoading = true;
		let files = $scope.filelist.filter(f => f.selectedSend).map(f => f.id);
		let targets = $scope.endpoints.receiver.filter(e => e.selectedSend).map(e => e.id);
		if(files.length == 0 || targets.length == 0) {
			alert("Choose at least one file and one machine.");
			$scope.sendLoading = false;
			return;
		}

		sdsd.rpcCall("agrirouter", "sendFiles", [files, targets, true], null, function() {
			$scope.updateLogs();
			$scope.sendLoading = false;
		});
		$scope.filelist.forEach(f => f.selectedSend = false);
		$scope.endpoints.receiver.forEach(e => e.selectedSend = false);
	};
	
	$scope.publishFiles = function() {
		$scope.publishLoading = true;
		let files = $scope.filelist.filter(f => f.selectedSend).map(f => f.id);
		let targets = $scope.endpoints.receiver.filter(e => e.selectedSend).map(e => e.id);
		if(files.length == 0) {
			alert("Choose at least one file.");
			$scope.sendLoading = false;
			return;
		}

		sdsd.rpcCall("agrirouter", "publishFiles", [files, targets, true], null, function() {
			$scope.updateLogs();
			$scope.publishLoading = false;
		});
		$scope.filelist.forEach(f => f.selectedSend = false);
		$scope.endpoints.receiver.forEach(e => e.selectedSend = false);
	};
	
	$scope.receiveFiles = function() {
		$scope.receiveLoading = true;
		sdsd.rpcCall("agrirouter", "receiveFiles", [false], function(data) {
			$scope.updateLogs();
			if(data.received > 0) {
				if(sdsd.websocket) $scope.loadFileList();
				
				if(!$scope.cancelReceiving && data.moreMessagesAvailable)
					return $scope.receiveFiles();
			}
			$scope.cancelReceiving = false;
			$scope.receiveLoading = false;
		}, null, function(error) {
			$scope.updateLogs(error);
			$scope.cancelReceiving = false;
			$scope.receiveLoading = false;
		});
	};
	
	$scope.receiveRecentFiles = function() {
		$scope.receiveRecentLoading = true;
		sdsd.rpcCall("agrirouter", "receiveFiles", [true], function(data) {
			$scope.updateLogs();
			if(data.received > 0) {
				if(sdsd.websocket) $scope.loadFileList();
				
				if(!$scope.cancelReceivingRecent && data.moreMessagesAvailable)
					return $scope.receiveRecentFiles();
			}
			$scope.cancelReceivingRecent = false;
			$scope.receiveRecentLoading = false;
		}, null, function(error) {
			$scope.updateLogs(error);
			$scope.cancelReceivingRecent = false;
			$scope.receiveRecentLoading = false;
		});
	};
	
	$scope.cancelReceiveFiles = function() {
		$scope.cancelReceiving = true;
	};
	
	$scope.cancelReceiveRecentFiles = function() {
		$scope.cancelReceivingRecent = true;
	};
	
	$scope.clearFeed = function() {
		if (confirm("Are you sure you want to delete all messages in the agrirouter feed?\nThis does not delete already received files.")) {
			$scope.clearfeedLoading = true;
			sdsd.rpcCall("agrirouter", "agrirouterClearFeeds", [], null, function() {
				$scope.updateLogs();
				$scope.clearfeedLoading = false;
			});
		}
	};
	
	$scope.sendSubs = function() {
		$scope.sendsubLoading = true;
		let newsubs = $scope.sublist.filter(s => s.active).map(s => s.type);
		sdsd.rpcCall("agrirouter", "agrirouterSetSubs", [ newsubs ], null, function() {
			$scope.updateLogs();
			$scope.sendsubLoading = false;
		});
	};
	
	$scope.loadSubList = function() {
		sdsd.rpcCall("agrirouter", "agrirouterSubList", [], function(data) {
			$scope.sublist = data.subs;
		}, null, $scope.updateLogs);
	};
	
	$scope.$watch('suball', function(newVal) {
		$scope.sublist.forEach(s => s.active = newVal);
	});
	
	$scope.loadCapabilities = function() {
		sdsd.rpcCall("agrirouter", "listCapabilities", [], function(data) {
			$scope.caps = data;
		}, null, $scope.updateLogs);
	};
	
	$scope.sendCapabilities = function() {
		$scope.sendCapLoading = true;
		sdsd.rpcCall("agrirouter", "setCapabilities", [ $scope.caps.capabilities, $scope.caps.pushNotifications ], null, function() {
			$scope.updateLogs();
			$scope.sendCapLoading = false;
		});
	};

	$scope.addStorageTask = function() {
		sdsd.rpcCall("file", "addStorageTask", [$scope.storageConf], function(data) {
			$scope.loadStorageTasks();
			$scope.resetStorageTask();
		}, $scope.updateLogs);
	};
		
	$scope.resetStorageTask = function() {
		/*
		"label": "Name des Speicherauftrags",
		"type": { "value": "Name des Typs", "equals": true/false } or undefined,
		"source": { "value": "Endpunkt-ID", "equals": true/false } or undefined,
		"from": "2018-07-06T12:31:53.52Z" or undefined,
		"until": "2018-08-06T12:31:53.52Z" or undefined,
		"storeUntil": "2018-09-06T12:31:53.52Z" or undefined,
		"storeFor": 30(Anzahl Tage) or undefined
		 */
		$scope.storageConf = {
			storeType: "for",
			storeFor: 30,
			type : { equals: true},
			source : { equals: true},
		};
	};
	
	$scope.deleteStorageTask = function(task) {
		sdsd.rpcCall("file", "deleteStorageTasks", [[task.id]], function(data) {
			$scope.loadStorageTasks();
		}, $scope.updateLogs);
	};
		
	$scope.loadStorageTasks = function() {
		sdsd.rpcCall("file", "listStorageTasks", [], function(data) {
			$scope.storageTasks = data.storageTasks;
		}, null, $scope.updateLogs);
	};

});
