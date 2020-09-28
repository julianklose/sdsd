app.directive('ngAutocomplete', $parse => {
	return {
		restrict: 'A',
		require: 'ngModel',
		compile: (elem, attrs) => {
			const modelAccessor = $parse(attrs.ngModel);

			return (scope, element, attrs) => {
				scope.$watch(attrs.ngAutocomplete, val => {
					element.autocomplete({
						source: val,
						select: (event, ui) => {
							scope.$apply(scope => {
								modelAccessor.assign(scope, ui.item.value);
							});
							if (attrs.onSelect)
								scope.$apply(attrs.onSelect);
							event.preventDefault();
						}
					});
				});
			}
		}
	}
});


app.controller('wikinormia-draftmode', ($q, $scope, $location, sdsd) => {
	const FORMAT_NEW = 'new';
	const FORMAT_EDIT = 'formatEdit';
	const FORMAT_ID = 'formatId';

	$scope.sdsd = sdsd;

	$scope.formatId;
	$scope.formatEdit;
	$scope.formatData;

	$scope.classId;
	$scope.classEdit;

	$scope.autocomplete;
	$scope.uritypes;
	$scope.units;
	$scope.artypes;
	$scope.familyTree;
	$scope.activeClassUri;

	// =================== Initialization ====================

	$scope.initVars = () => {
		$scope.formatData = {};
		$scope.formatId = undefined;
		$scope.formatEdit = false;
		$scope.classId = undefined;
		$scope.classEdit = false;
		$scope.isTabForActualClass = true;
		$scope.familyTree = [];
		$scope.initClassData();
		$scope.initAutocomplete();
		$scope.uritypes = {};
		$scope.units = [];
		$scope.artypes = [];
		$scope.activeClassUri = undefined;
	}

	$scope.initClassData = () => {
		const classData = {
			uri: 'self',
			base: [],
			partOf: [],
			attributes: [],
			instances: []
		};

		if ($scope.familyTree.length)
			$scope.familyTree[0] = classData;
		else
			$scope.familyTree.push(classData);
	}

	$scope.initAutocomplete = () => {
		$scope.autocomplete = {
			formats: [],
			types: [],
			literals: [],
			instances: {
				attributes: {},
				partOf: {}
			}
		};
	}

	$scope.init = () => {
		$scope.initVars();
		$scope.formatId = $location.search().formatId;
		$scope.formatEdit = $location.search().formatEdit;
	};

	// ====================== Format Actions =======================

	$scope.createOrUpdateDraftFormat = () => {
		if ($scope.formatId === FORMAT_NEW)
			$scope.createNewDraftFormat();
		else
			$scope.updateDraftFormat();
	}

	$scope.updateDraftFormat = () => {
		if (!$scope.validateFormatData()) {
			console.log('updateDraftFormat(): Input validation failed.', $scope.formatData);
			return;
		}

		$scope.setDraftFormat($scope.formatId, $scope.formatData).then(data => {
			console.log(`updateDraftFormat(${$scope.formatId}):`, $scope.formatData, data);
			$scope.formatEdit = false;
			$location.search({
				[FORMAT_ID]: $scope.formatId
			});
		}, error => {
			console.log(error);
		});
	}

	$scope.createNewDraftFormat = () => {
		if (!$scope.validateFormatData()) {
			console.log('createNewDraftFormat(): Input validation failed.', $scope.formatData);
			return;
		}

		$scope.setDraftFormat(null, $scope.formatData).then(data => {
			console.log(`createNewDraftFormat():`, $scope.formatData, data);
			$scope.formatEdit = false;
			$scope.formatId = data.id;
			$location.search({
				[FORMAT_ID]: $scope.formatId,
			});
		}, error => {
			console.log(error);
		});
	}

	$scope.getDraftFormatData = () => {
		$scope.getDraftFormat($scope.formatId).then(data => {
			$scope.formatData = data;
		}, error => {
			console.log(error);
		});
	}

	$scope.getClassesForDraftFormat = () => {
		$scope.listDraftClass($scope.formatId).then(data => {
			console.log(`getClassesForDraftFormat(${$scope.formatId}):`, data);
			$scope.savedClasses = data.items;
			$scope.getAutocompleteTypes();
		}, error => {
			console.log(error);
		});
	}

	$scope.clearDraftFormatData = () => {
		$scope.formatData = {};
	}

	// ===================== Class Actions =======================

	$scope.createOrUpdateDraftClass = () => {
		$scope.removeRepeatingPartsAndSubclasses();

		if ($scope.classEdit)
			$scope.updateDraftClass();
		else
			$scope.createNewDraftClass();

	}

	$scope.createNewDraftClass = () => {
		if (!$scope.validateClassData()) {
			console.log('createNewDraftClass(): Input validation failed.', $scope.familyTree[0]);
			return;
		}

		$scope.setDraftClass($scope.formatId, null, $scope.cleanJson($scope.familyTree[0])).then(data => {
			console.log(`createNewDraftClass(${$scope.formatId}, null):`, $scope.familyTree[0], data);
			$scope.classId = data.id;
			$scope.clearDraftClassData();
			$scope.getClassesForDraftFormat();
		}, error => {
			console.log(error);
		});
	}

	$scope.updateDraftClass = () => {
		$scope.setDraftClass($scope.formatId, $scope.classId, $scope.cleanJson($scope.familyTree[0])).then(data => {
			console.log(`updateDraftClass(${$scope.formatId}, ${$scope.classId}):`, $scope.familyTree[0], data);
			$scope.clearDraftClassData();
			$scope.getClassesForDraftFormat();
		}, error => {
			console.log(error);
		});
	}

	$scope.getDraftClassData = classId => {
		$scope.classId = classId;
		$scope.getDraftClass($scope.classId).then(data => {
			console.log(`getDraftClass(${$scope.classId}):`, data);
			$scope.familyTree[0] = data;
			$scope.familyTree[0].uri = $scope.classId;
			$scope.classEdit = true;
			$scope.getFamilyTreeForClasses();
			$scope.activeClassUri = $scope.classId;
		}, error => {
			console.log(error);
		});
	}

	$scope.getFamilyTreeForClasses = () => {
		const cleanedBaseValues = [];
		for (const base of $scope.familyTree[0].base)
			cleanedBaseValues.push(base.value);

		$scope.getFamilyTree(cleanedBaseValues).then(data => {
			console.log(`getFamilyTree(${cleanedBaseValues}):`, data);
			$scope.initFamilyTree();
			$scope.familyTree = $scope.familyTree.concat(data.tree);
			console.log('$scope.familyTree:', $scope.familyTree);

			for (const familyMember of $scope.familyTree) {
				for (const attr of familyMember.attributes)
					attr.literal = attr.type.includes('XMLSchema#');

				familyMember.attributes.sort((a, b) => {
					return a.identifier.localeCompare(b.identifier);
				});
			}
		});
	}

	// ====================== Autocomplete =======================

	$scope.getAutocompleteFormats = () => {
		$scope.listFormats().then(data => {
			if ($scope.autocomplete)
				$scope.autocomplete.formats = data.formats;
			else
				$scope.autocomplete = data;
		});
	}

	$scope.getAutocompleteTypes = () => {
		$scope.listAutocompleteTypes().then(data => {
			$scope.autocomplete.types = data.types;
			for (const format of data.types) {
				for (const clazz of format.list) {
					$scope.uritypes[clazz.value] = {
						label: clazz.label,
						format: format.label
					};
				}
			}
		});
	}

	$scope.getAutocompleteLiterals = () => {
		$scope.listLiteralTypes().then(data => {
			$scope.autocomplete.literals = data.literals;
		});
	}

	$scope.getARTypes = () => {
		$scope.listARTypes().then(data => {
			$scope.artypes = data.artypes;
		});
	}

	$scope.getUnits = () => {
		$scope.listUnits().then(data => {
			$scope.units = data.units;
		});
	}

	$scope.getAutocompleteInstancesForPartOf = () => {
		const partOfUris = [];
		for (const partOf of $scope.familyTree[0].partOf)
			partOfUris.push(partOf.value);
		$scope.getAutocompleteInstances(partOfUris, 'partOf');
	}

	$scope.getAutocompleteInstancesForAttribute = attr => {
		if (attr.literal) return;
		$scope.getAutocompleteInstances([attr.type]);
	}

	$scope.getAutocompleteInstances = (uris, branch = 'attributes') => {
		uris.forEach((uri, i) => {
			if ($scope.hasProp($scope.autocomplete.instances[branch], uri))
				uris.splice(i, 1);
		});

		if (!uris.length) return;

		$scope.getInstances(uris).then(instances => {
			console.log(`getInstances(${uris}):`, instances);
			for (const inst of instances.instances) {
				$scope.autocomplete.instances[branch][inst.value] = {
					label: inst.label,
					list: inst.list
				}
			}

			for (const uri of uris) {
				if (!$scope.hasProp($scope.autocomplete.instances[branch], uri))
					$scope.autocomplete.instances[branch][uri] = {
						label: '',
						list: []
					}
			}

			console.log(`autocomplete.instances.${branch}:`, $scope.autocomplete.instances[branch]);
		});
	}

	// ========================= Watched ==========================

	$scope.$watchGroup([FORMAT_ID, FORMAT_EDIT], () => {
		console.log(`$scope.$watchGroup(['${FORMAT_ID}', '${FORMAT_EDIT}'])`, $scope.formatId, $scope.formatEdit);

		if ($scope.formatId !== FORMAT_NEW && !$scope.formatEdit) {
			$scope.getClassesForDraftFormat();
			$scope.getAutocompleteLiterals();
			$scope.getUnits();
		}

		if ($scope.formatEdit)
			$scope.getARTypes();

		if ($scope.formatId !== FORMAT_NEW)
			$scope.getDraftFormatData();
	});

	// ==================== Utility Functions =====================

	$scope.validateFormatData = () => {
		return $scope.validateInput($scope.formatData, ['label', 'identifier', 'shortDescription', 'mimeType', 'artype']);
	}

	$scope.validateClassData = () => {
		return $scope.validateInput($scope.familyTree[0], ['label', 'identifier', 'shortDescription']);
	}

	$scope.validateInput = (obj, attrs) => {
		for (const attr of attrs)
			if (angular.isUndefined(obj[attr]))
				return false
		return true;
	}

	$scope.cleanJson = angularJson => {
		return JSON.parse(angular.toJson(angularJson));
	}

	$scope.toCamelCase = (input, upper = false) => {
		if (!input) return;
		let str = '';
		for (let i = 0; i < input.length; ++i) {
			const c = input.charAt(i);
			if (c === ' ')
				upper = true;
			else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
				str += upper ? c.toUpperCase() : c;
				upper = false;
			} else
				upper = false;
		}
		return str;
	}

	$scope.initFamilyTree = () => {
		if ($scope.familyTree.length > 1)
			$scope.familyTree = $scope.familyTree.splice(0, 1);
	}

	$scope.convTypeUri = uri => {
		return $scope.uritypes[uri]?.label ?? 'N/A';
	}

	$scope.hasProp = (obj, prop) => {
		if (!obj) return false
		return Object.prototype.hasOwnProperty.call(obj, prop);
	}

	$scope.distinct = (arr, comp) => {
		return arr.map(e => e[comp]).map((e, i, final) => final.indexOf(e) === i && i).filter((e) => arr[e]).map(e => arr[e]);
	}

	$scope.removeRepeatingPartsAndSubclasses = () => {
		if (!$scope.familyTree.length) return;
		$scope.familyTree[0] = $scope.cleanJson($scope.familyTree[0]);
		$scope.familyTree[0].base = $scope.distinct($scope.familyTree[0].base, 'value');
		$scope.familyTree[0].partOf = $scope.distinct($scope.familyTree[0].partOf, 'value');
		for (const instance of $scope.familyTree[0].instances)
			instance.partOf = $scope.distinct(instance.partOf, 'value');
	}

	// ===================== Event Handlers =======================

	$scope.submitForm = (formId, next) => {
		const form = $(formId)[0];
		if (!form.checkValidity())
			form.reportValidity();
		else
			next();
	}

	$scope.clearDraftClassData = () => {
		$scope.classId = undefined;
		$scope.activeClassUri = undefined;
		$scope.classEdit = false;
		$scope.initClassData();
	}

	$scope.deleteSelectedDraftClass = () => {
		if (!$scope.classId || !window.confirm('Do you really want to delete the selected draft class?'))
			return;
		$scope.dropDraftClass($scope.classId).then(data => {
			console.log(`deleteSelectedDraftClass(${$scope.classId}):`, data);
			$scope.clearDraftClassData();
			$scope.getClassesForDraftFormat();
		}, error => {
			console.log(error);
		});
	}

	$scope.publishDraftFormat = () => {
		if (!window.confirm('Do you really want to publish the current draft format?'))
			return;

		$scope.publishDraft($scope.formatId).then(data => {
			console.log(`publishFormat(${$scope.formatId}):`, data);
			$scope.clearDraftClassData();

			// TODO: Comment in after implementation works
			// location.href = 'wikinormia.html';
		});
	}

	$scope.editSelectedDraftFormat = () => {
		$scope.formatEdit = true;
		$location.search({
			[FORMAT_ID]: $scope.formatId,
			[FORMAT_EDIT]: $scope.formatEdit
		});
	}

	$scope.deleteSelectedDraftFormat = () => {
		if (!window.confirm('Do you really want to delete the current draft format?'))
			return;
		$scope.dropDraftFormat($scope.formatId).then(data => {
			console.log(`deleteSelectedDraftFormat(${$scope.formatId}):`, data);
			$scope.clearDraftFormatData();
			$scope.formatEdit = true;
			$scope.formatId = FORMAT_NEW;
			$location.search({
				[FORMAT_ID]: $scope.formatId,
				[FORMAT_EDIT]: $scope.formatEdit
			});
		}, error => {
			console.log(error);
		});
	}

	$scope.autoCompleteIdentifier = source => {
		switch (source) {
			case 'format':
				$scope.formatData.identifier = $scope.toCamelCase($scope.formatData.label);
				$('#formatIdentifier').trigger('input');
				break;
			case 'class':
				$scope.familyTree[0].identifier = $scope.toCamelCase($scope.familyTree[0].label);
				$('#classIdentifier').trigger('input');
				break;
			default:
				console.assert(false);
		}
	}

	$scope.addItem = (collection, item = {}, position) => {
		if (!position) {
			collection.push(item);
		} else
			collection.splice(position, 0, item);
	}

	$scope.addInstanceItem = () => {
		if (!$scope.familyTree[0].attributes.length && $scope.familyTree.length == 1) return;
		for (const attr of $scope.familyTree[0].attributes)
			if (!attr.identifier) return;

		const instance = {
			identifier: '',
			label: '',
			partOf: [],
			attributes: {}
		};

		$scope.familyTree[0].instances.push(instance);
	}

	$scope.initInstanceMember = (iIndex, cIndex) => {
		const curInstance = $scope.familyTree[0].instances[iIndex];

		for (const familyMember of $scope.familyTree)
			if (!$scope.hasProp(curInstance.attributes, familyMember.uri))
				curInstance.attributes[familyMember.uri] = {};

		const familyMember = $scope.familyTree[cIndex];
		const curAttrs = $scope.familyTree[0].instances[iIndex].attributes;

		for (const attr of familyMember.attributes) {
			if ($scope.hasProp(curAttrs[familyMember.uri], attr.identifier)) continue;
			curAttrs[familyMember.uri][attr.identifier] = {
				type: attr.type,
				value: []
			};
		}
	}

	$scope.deleteItem = (collection, item) => {
		const index = collection.indexOf(item);
		if (index >= 0) collection.splice(index, 1);
	}

	$scope.deleteSubclassItem = (collection, item) => {
		const index = collection.indexOf(item);
		if (index >= 0) collection.splice(index, 1);
		$scope.getFamilyTreeForClasses();
	}

	$scope.changeClassTab = uri => {
		$scope.activeClassUri = uri;
	}

	// ======================== RPC calls =========================

	$scope.getDraftClass = classId => {
		return $q((resolve, reject) => {
			if (!classId) {
				reject(new Error(`[getDraftClass] classId is ${classId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'getDraft', [classId], data => resolve(data));
		});
	}

	$scope.getDraftFormat = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[getDraftFormat] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'getDraft', [formatId], data => resolve(data));
		});
	}

	$scope.listDraftClass = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[listDraftClass] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'listDraft', [formatId], data => resolve(data));
		});
	}

	$scope.setDraftFormat = (formatId, formatData) => {
		return $q((resolve, reject) => {
			if (!formatData) {
				reject(new Error(`[setDraftFormat] formatData is ${formatData}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'setDraft', [formatId, formatData], data => resolve(data));
		});
	}

	$scope.setDraftClass = (formatId, classId, classDataJson) => {
		return $q((resolve, reject) => {
			if (!formatId || !classDataJson) {
				reject(new Error('[setDraftClass] formatId or classDataJson is not set:', formatId, classDataJson));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'setDraft', [formatId, classId, classDataJson], data => resolve(data));
		});
	}

	$scope.dropDraftFormat = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[dropDraftFormat] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'dropDraft', [formatId], data => resolve(data));
		});
	}

	$scope.dropDraftClass = classId => {
		return $q((resolve, reject) => {
			if (!classId) {
				reject(new Error(`[dropDraftClass] classId is ${classId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'dropDraft', [classId], data => resolve(data));
		});
	}

	$scope.listUnits = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listUnits', [], data => resolve(data));
		});
	}

	$scope.listAutocompleteTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listAutocompleteTypes', [], data => resolve(data));
		});
	}

	$scope.listLiteralTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listLiteralTypes', [], data => resolve(data));
		});
	}

	$scope.listFormats = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'listFormats', [], data => resolve(data));
		});
	}

	$scope.listARTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'listARTypes', [], data => resolve(data));
		});
	}

	$scope.getFamilyTree = baseUris => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'getFamilyTree', [baseUris], data => resolve(data));
		});
	}

	$scope.publishDraft = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[publishDraft] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'publishDraft', [formatId], data => resolve(data));
		});
	}

	$scope.getInstances = types => {
		return $q((resolve, reject) => {
			if (!types) {
				reject(new Error(`[getInstances] types is ${types}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'getInstances', [types], data => resolve(data));
		});
	}
});

// ================== jQuery Functions ==================

$(function() {
	$(':input[required]').each((i, elem) => {
		elem.oninvalid = function() {
			const placeholder = $(elem).attr('placeholder');
			this.setCustomValidity('Fill in the ' + placeholder + '!');
		}

		elem.oninput = function() {
			this.setCustomValidity('');
		}
	});
});
