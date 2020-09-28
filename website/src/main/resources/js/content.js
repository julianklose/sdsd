app.controller('contentCtrl', function ($scope, $sce, $timeout, sdsd) {
	$scope.sdsd = sdsd;
	
	$scope.filelist = [];
	$scope.addressBar = "";
	$scope.addressBarType = "File";
	$scope.types = ["File", "Resource", "Type"];
	$scope.content = [];
	$scope.content_html = "";
	$scope.history = [];
	$scope.historyIndex = 0;
	$scope.accept = "text/html";
	
	//paging
	$scope.offset = 0;
	$scope.limitDefault = 20;
	$scope.limit = $scope.limitDefault;

	$scope.init = function () {
		$scope.interceptLinks();
		$scope.home();
	};
	
	$scope.$watch('sdsd.username', function (newVal) {
		if (newVal) {
			$scope.loadFileList();
		} else {
			$scope.filelist = [];
		}
	});
	
	$scope.loadFileList = function () {
		sdsd.rpcCall("file", "listFiles", [], function (data) {
			console.log(data.files);
			
			//no protobuf (EFDI)
			data.files = data.files.filter(f => f.type !== 'application/x-protobuf');
			
			$scope.filelist = data.files;
		});
	};
	
	$scope.switchType = function(type) {
		$scope.addressBarType = type;
		$scope.browse();
	};
	
	$scope.selectFile = function(file) {
		$scope.addressBarType = "File";
		$scope.addressBar = "/?view=graph&uri=" + encodeURIComponent("sdsd:" + file.id);
		$scope.browse();
	};
	
	$scope.browseWith = function(type, address, noHistory) {
		//$scope.addressBarType = type;
		$scope.addressBar = address;
		$scope.offset = 0;
		$scope.limit = $scope.limitDefault;
		$scope.browse(noHistory);
	};
	
	$scope.browse = function(noHistory) {
		$scope.content = [];
		
		if(!noHistory) {
			var l = $scope.history.length - $scope.historyIndex;
			$scope.history.splice($scope.historyIndex, l);

			//only if last added is not the same
			var last = $scope.history[$scope.history.length-1];
			var doPush = true;
			if(last) {
				doPush = !(last.address === $scope.addressBar && last.type === $scope.addressBarType);
			}
			
			if(doPush)
				$scope.history.push({ type: $scope.addressBarType, address: $scope.addressBar});
			
			$scope.historyIndex = $scope.history.length;
		}
		
		$scope.wait = true;
		
		sdsd.rpcCall("sdb", "browse", [$scope.addressBar, $scope.accept], function(resp) {
					
						if(resp.json) {
							$scope.content_html = "<pre>" + JSON.stringify(resp.json, null, 2) + "</pre>";
						} else if(resp.html) {
							$scope.content_html = resp.html;
						}
					
			//$timeout(function() {
				//$scope.interceptLinks();
			//});
			
			$scope.wait = false;
		});
		
		/*
		sdsd.rpcCall("content", "browse", [$scope.addressBarType, $scope.addressBar, $scope.offset, $scope.limit], function(resp) {
			$scope.content = resp;
			$scope.wait = false;
		});
		 */
	};

	$scope.home = function() {
		//first page
		$scope.browseWith("Resource", "/", false);
	};

	$scope.historyPrev = function() {
		$scope.historyIndex -= 1;
		
		//min
		if($scope.historyIndex < 1) {
			$scope.historyIndex = 1;
		} else {
			$scope.historyBrowse();
		}
	};
	
	$scope.historyNext = function() {
		$scope.historyIndex += 1;
		
		//max
		if($scope.historyIndex > $scope.history.length) {
			$scope.historyIndex = $scope.history.length;
		} else {
			$scope.historyBrowse();
		}
	};
	
	$scope.historyBrowse = function() { 
		var target = $scope.history[$scope.historyIndex-1];
		if(target) {
			$scope.browseWith(target.type, target.address, true);
		}
	};

	$scope.nextPage = function() {
		$scope.offset += $scope.limit;
		$scope.browse(true);
	};
	
	$scope.prevPage = function() {
		$scope.offset -= $scope.limit;
		if($scope.offset < 0)
			$scope.offset = 0;
		$scope.browse(true);
	};

	$scope.renderHtml = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

	$scope.interceptLinks = function() {
		$(".contentWindow").on("click", "a", function(e) {

			e.preventDefault();

			var href = $(this).attr("href");
			
			//try to cut host part
			try {
				var url = new URL(href);
				href = url.pathname + url.search + url.hash;
			} catch(e) {
				
			}

			console.log(href);

			if(href.startsWith("/wikinormia.html")) {
				window.open(href, '_blank');
			} else {
				$scope.browseWith("Resource", href, false);
			}
		});
	};


});