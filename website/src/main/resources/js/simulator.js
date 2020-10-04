/**
 * SDSD client functions for the simulator page.
 * 
 * @file   Defines the angularJS simulator controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

app.filter('toArray', function() { return function(obj) {
	if (!(obj instanceof Object)) return obj;
	return $.map(obj, function(val, key) {
		val.key = key;
		return val;
	});
}});
/**
 * Simulator Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 * @param interval wrapper for window.setInterval. Executes a function every x millisconds
 */
app.controller('simulator', function ($scope, $interval, sdsd) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	// files belonging to the current user
	$scope.filelist = [];
	// user options
	$scope.options = { name: null, skip: 0, interval: 5, scale: 1.0, replaceTime: false, endless: false };
	$scope.noTimelogs = true;

	$scope.progress;
	$scope.loadPromise; //Pointer to the promise created by the Angular
	
	/**
	 * init - called on startup. Sets datepicker date format and tooltips
	 *
	 */
	$scope.init = function () {
		$('[data-toggle="tooltip"]').tooltip();
		$('.form-datepicker').datetimepicker({
			format: "Y-m-d H:i:s"
		});
	};
	/**
	 * $watch('sdsd.username') - Listener for changes of the sdsd.username. Starts new websocket connection that loads files and simulation progress.
	 *
	 * @param newVal new username
	 */
	$scope.$watch('sdsd.username', function (newVal) {
		if(newVal) {
			sdsd.connectWebsocket().then(function(ws) {
				$scope.getProgress();
				$scope.loadFileList();
				$scope.noTimelogs = true;
				
				if(ws) {
					sdsd.setListener("simulator", "progress", null, function(params) {
						$scope.showProgress(params[0]);
					});
				}
			});
		}
		else {
			delete $scope.progress;
			$scope.filelist = [];
			delete $scope.file;
			$scope.noTimelogs = true;
		}
	});
	
	/**
	 * loadFileList - starts a rpc call, which return the users files.
	 *
	 */
	$scope.loadFileList = function() {
		sdsd.rpcCall("simulator", "listFiles", [], function(data) {
			$scope.filelist = data.files;
		});
	};
		
	/**
	 * watch('file') - listener that fires when the current selected file changes. Executes remote call that gets the files timelog informations.
	 *
	 * @param file chosen file
	 */
	$scope.$watch('file', function (file) {
		if(file) {
			$scope.simulatorLoading = true;
			sdsd.rpcCall("simulator", "timelogInfo", [file.id], function(data) {
				$scope.file.timelogs = data.timelogs;
			}, function() {
				$scope.noTimelogs = jQuery.isEmptyObject($scope.file.timelogs);
				$scope.simulatorLoading = false;
			});
		}
	});
	
		
	/**
	 * sendEntire - starts remote call that transfers the entire timelog. 
	 *
	 */
	$scope.sendEntire = function() {
		$scope.simulatorLoading = true;
		let replaceTime = null;
		if ($scope.options.replaceTime) {
			replaceTime = $scope.options.replaceTimestamp ? new Date($scope.options.replaceTimestamp).toISOString() : new Date().toISOString();
		}
		sdsd.rpcCall("simulator", "sendEntire", [$scope.file.id, $scope.options.name, $scope.options.skip, replaceTime], function(data) {
			alert("Successfully sent entire " + ($scope.options.name ? "timelog!" : "file!"));
		}, function() {
			$scope.simulatorLoading = false;
		});
	}
	
	/**
	 * simulate - starts simulation 
	 *
	 */
	$scope.simulate = function() {
		$scope.simulatorLoading = true;
		let replaceTime = null;
		if ($scope.options.replaceTime) {
			replaceTime = $scope.options.replaceTimestamp ? new Date($scope.options.replaceTimestamp).toISOString() : new Date().toISOString();
		}
		sdsd.rpcCall("simulator", "simulate", [$scope.file.id, $scope.options.name, $scope.options.skip, $scope.options.interval, $scope.options.scale, replaceTime, $scope.options.endless], function(data) {
			$scope.showProgress(data);
		}, function() {
			$scope.simulatorLoading = false;
		});
	}
	
	/**
	 * showProgress - calculates current progress in percent and updates user interfac accordingly
	 *
	 * @param data inludes the working state and start, end and current timestamp
	 */
	$scope.showProgress = function(data) {
		$scope.progress = data.name ? data : null;
		if($scope.progress){
			let start = new Date($scope.progress.timeLog.start);
			let end = new Date($scope.progress.timeLog.end);
			let cur = new Date($scope.progress.timeLog.current);
			$scope.progress.timeLog.percent = Math.round((cur-start)*100/(end-start));
			$("#progress-bar-timelogs").css("width", $scope.progress.timeLog.percent + "%");
			
			if($scope.progress.error) sdsd.message = $scope.progress.error;
		}
		if(data.state == 'running' && !sdsd.websocket) {
			if(!$scope.loadPromise)
				$scope.loadPromise = $interval($scope.getProgress, data.interval*1000);
		} else if($scope.loadPromise) {
			$interval.cancel($scope.loadPromise);
			delete $scope.loadPromise;
		}
	}
	
	/**
	 * getProgress - starts a remote call that gets the current data. When the call finishes the current progress is calculated and ths user interface is updated accordingly.
	 *
	 */
	$scope.getProgress = function() {
		sdsd.rpcCall("simulator", "progress", [], function(data) {
			$scope.showProgress(data);
		});
	}
	
	/**
	 * pause - remote call that pauses the simulation process and displays the current simulation progress.
	 *
	 */
	$scope.pause = function() {
		$scope.progress.state = 'paused';
		sdsd.rpcCall("simulator", "pause", [], function(data) {
			$scope.showProgress(data);
		});
	}
	
		
	/**
	 * resume - remote call that resumes the simulation process and displays the current simulation progress.
	 *
	 */
	$scope.resume = function() {
		$scope.progress.state = 'running';
		sdsd.rpcCall("simulator", "resume", [], function(data) {
			$scope.showProgress(data);
		});
	}
	
	/**
	 * forward - remote call that skips to the next timelog and displays updates simulationprogress.
	 *
	 */
	$scope.forward = function() {
		sdsd.rpcCall("simulator", "forward", [], function(data) {
			$scope.showProgress(data);
		});
	}

	/**
	 * stop - remote call that stops the simulation process and displays the current simulation progress.
	 *
	 */
	$scope.stop = function() {
		sdsd.rpcCall("simulator", "stop", [], function(data) {
			$scope.showProgress({});
		});
	}
	/**
	 * on('destroy') - event listener that fires when the destroy event is executed. Shows the current simulation progress.
	 *
	 */
	$scope.$on('$destroy', function() {
		$scope.showProgress({});
	});
});