/**
 * SDSD client functions for the validator page.
 * 
 * @file   Defines the angularJS validator controller.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

/**
 * Map Controller
 *
 * @param scope variable scope
 * @param sdsd sdsd module
 */
app.controller('validator', function ($scope, sdsd) {
	// sdsd module scope
	$scope.sdsd = sdsd;
	// list of the current user files
	$scope.filelist = [];
	// selected files name
	$scope.filename = "";
	// outptut message
	$scope.output = [];

	/**
	 * init - called on startup.
	 *
	 */
	$scope.init = function () {

	};
	
	/**
	 * watch('sdsd.username') - listener that triggers when the username changes. Loads the current users files and clears old data belonging to other users
	 * @param newVal new username
	 */
	$scope.$watch('sdsd.username', function (newVal) {
		if(newVal) {
			$scope.loadFileList();
			$scope.filename = "";
			$scope.output = [];
		}
		else {
			$scope.filelist = [];
			$scope.filename = "";
			$scope.output = [];
		}
	});

	/**
	 * loadFileList - starts a remote call, which return the users files.
	 *
	 */

	$scope.loadFileList = function() {
		sdsd.rpcCall("validator", "listIsoXmlFiles", [], function(data) {
			$scope.filelist = data.files;
		});
	};
	
	/**
	 * validate - starts a remote call, which validates the selected file. Updates the output message.
	 *
	 * @param file file that is validated
	 */
	$scope.validate = function(file) {
		$scope.filename = "";
		$scope.output = [];
		if(file) {
			$scope.validatorLoading = true;
			sdsd.rpcCall("validator", "validate", [file.id], function(data) {
				$scope.filename = data.filename;
				$scope.output = data.output;
			}, function() {
				$scope.validatorLoading = false;
			});
		}
	};
	/**
	 * watch('file') - on change listener that starts the validation process after a new file ts selected.
	 *
	 */
	$scope.$watch('file', $scope.validate);
});