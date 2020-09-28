/**
 * SDSD client helper functions.
 * 
 * @file   Defines the angularJS main module, filters and services.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

var app = angular.module('sdsd', ['luegg.directives']);

app.config(function($locationProvider) {
	$locationProvider.html5Mode({
		enabled: true,
		requireBase: false,
		rewriteLinks: false
	});
});

app.directive('ngEnter', function () {
	return function (scope, element, attrs) {
		element.bind("keydown", function (e) {
			if (e.which === 13) {
				scope.$apply(function () {
					scope.$eval(attrs.ngEnter, {'e': e});
				});
				// e.preventDefault();
			}
		});
	};
});

app.filter('escape', function() {
	return window.encodeURIComponent;
});

app.filter('relative', function() {
	return function(input) {
		if(input && input.startsWith('https://app.sdsd-projekt.de/'))
			return input.slice(27);
		return input;
	};
});

function getString(item, param) {
	return (typeof param === 'function') ? String(param(item)) : String(item[param]);
}

app.filter('search', function() {
	return function(input, search, param) {
		if(search == undefined) return input;
		if(typeof search === 'string') {
			if(search.length < 2) return input;
			if(search == search.toLowerCase())
				return param != undefined ? input.filter(item => getString(item, param).toLowerCase().indexOf(search) >= 0) 
						: input.filter(item => String(item).toLowerCase().indexOf(search) >= 0);
			else 
				return param != undefined ? input.filter(item => getString(item, param).indexOf(search) >= 0) 
						: input.filter(item => String(item).indexOf(search) >= 0);
		} else 
			return param != undefined ? input.filter(item => item[param] == search) 
					: input.filter(item => item == search);
	};
});

function capitalize(word) {
	return word.charAt(0).toUpperCase() + word.slice(1);
}

app.service('sdsd', ['$rootScope', function($rootScope) {
	var sdsd = {
		username: null,
		message: null,
		rpcCallId: 0,
		rpcCallbacks: {},
		rpcEventListener: {},
		rpcCall: function (endpoint, method, params, resultCallback, callback, errorCallback) {
			let request = {
				method: endpoint + "." + method, 
				params: params,
				id: ++sdsd.rpcCallId
			};
			let cb = function (result) {
				if(result.result && resultCallback) {
					$rootScope.$apply(function () {
						resultCallback(result.result);
					});
				}
				if(result.error) {
					$rootScope.$apply(function () {
						if(errorCallback)
							errorCallback(result.error);
						else
							sdsd.message = result.error.message || result.error.msg;
					});
				}
				if (callback) {
					$rootScope.$apply(function () {
						callback();
					});
				}
			};
			if(sdsd.websocket) {
				sdsd.rpcCallbacks[request.id] = cb;
				sdsd.websocket.send(JSON.stringify(request));
			} else {
				$.ajax({
					url: "/" + endpoint + "/json-rpc",
					type: "POST",
					data: JSON.stringify(request),
					dataType: "json",
					cache: false,
					contentType: "application/json",
					success: cb
				});
			}
		},
		getCookie: function (cname) {
			var name = cname + "=";
			var decodedCookie = decodeURIComponent(document.cookie);
			var ca = decodedCookie.split(';');
			for(var i = 0; i <ca.length; i++) {
				var c = ca[i];
				while (c.charAt(0) == ' ') {
					c = c.substring(1);
				}
				if (c.indexOf(name) == 0) {
					return c.substring(name.length, c.length);
				}
			}
			return "";
		},
		connectWebsocket: function () {
			return new Promise(function(resolve, reject) {
				if(WebSocket && !sdsd.websocket) {
					sdsd.websocket = new WebSocket((location.protocol == 'https:' ? 'wss://' : 'ws://') + location.host + '/websocket/sdsd');
					if(sdsd.websocket) {
						sdsd.websocket.onopen = function(event) {
							console.log("WebSocket opened");
							sdsd.websocket.send('{"SDSDSESSION": "' + sdsd.getCookie('SDSDSESSION') + '"}');
							for(let listener in sdsd.rpcEventListener) {
								let p = listener.split(',');
								sdsd.rpcCall(p[0], 'set' + capitalize(p[1]) + 'Listener', [p[2]]);
							}
							resolve(sdsd.websocket);
						};
						sdsd.websocket.onclose = function(event) {
							console.log("WebSocket closed: " + event.code + ": " + event.reason);
							if(event.code != 1000 && event.code != 1001) {
								sdsd.websocket = null;
								sdsd.connectWebsocket();
							} else
								delete sdsd.websocket;
						};
						sdsd.websocket.onerror = function(event) {
							console.error("WebSocket error", event);
							sdsd.message = event.message;
							reject(event);
						};
						sdsd.websocket.onmessage = function(event) {
							let msg = JSON.parse(event.data);
							if(msg.method) {
								let cb = sdsd.rpcEventListener[[msg.endpoint, msg.method, msg.identifier]];
								if(cb) {
									$rootScope.$apply(function () {
										cb(msg.params);
									});
								}
							} else if(msg.id) {
								let cb = sdsd.rpcCallbacks[msg.id];
								if(cb) {
									cb(msg);
									delete sdsd.rpcCallbacks[msg.id];
								}
							} else
								console.log(event.data);
						};
					}
					else
						resolve();
				}
				else
					resolve();
			});
		},
		setListener: function (endpoint, name, identifier, callback) {
			sdsd.rpcEventListener[[endpoint, name, identifier]] = callback;
			sdsd.rpcCall(endpoint, 'set' + capitalize(name) + 'Listener', [identifier]);
		}
	};
	return sdsd;
}]);

app.controller('login', function ($scope, sdsd) {
	$scope.sdsd = sdsd;
	$scope.input = {};
	
	$scope.init = function () {
		$scope.status();
	};

	$scope.$watch('username', function(newVal) {
		if(newVal) {
			$scope.getInstance();
		}
		else {
			$scope.instance = {};
		}
	});

	$scope.readStatus = function(data) {
		sdsd.username = data.username || null;
	}

	$scope.login = function() {
		sdsd.rpcCall("user", "login", [$scope.input.username, $scope.input.password], function(data) {
			$scope.readStatus(data);
			$scope.input.username = "";
			$scope.input.password = "";
		});
	};
	
	$scope.status = function() {
		sdsd.rpcCall("user", "status", [], $scope.readStatus);
	};
	
	$scope.logout = function() {
		if(sdsd.websocket) {
			sdsd.websocket.close();
			delete sdsd.websocket;
		}
		sdsd.rpcCall("user", "logout", [], function(data) {
			sdsd.username = null;
		});
	};
	
	$scope.unregister = function() {
		if(confirm("Are you sure you want to delete your account?")) {
			if(sdsd.websocket) {
				sdsd.websocket.close();
				delete sdsd.websocket;
			}
			sdsd.rpcCall("user", "unregister", [], function(data) {
				sdsd.username = null;
			});
		}
	};

});
