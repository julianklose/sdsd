/**
 * @global
 * @author Noah Grosse Starmann
 * @author Andreas Schliebitz
 */

/**
 * @function
 * @name appdirective
 * @description A marker on a DOM-Element to obtain ngAutocomplete functionality.Implements autocomplete.
 */
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

/**
 * @class
 * @name appcontroller
 * @description Controller for wikinormia-draftmode functionalities.It depends on sdsd controller.
 * @param {Object} q variable to allow promises functionality in Angular.js (see documentation of Angular)
 * @param {Object} scope variable to obtain the scope used by controller
 * @param {Object} location variable to obtain location for controller
 * @param {Object} sdsd variable to get all base functionalities of SDSD website (User management, API access)
 */
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
	/**
	 * @function
	 * @name initVars
	 * @memberOf appcontroller
	 * @description Function to initialize all used variables empty in given scope. It calls initClassData and initAutocomplete.
	 */
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

	/**
	 * @function
	 * @name initClassData
	 * @memberOf appcontroller
	 * @description Function to initialize an empty structure for Wikinormia class data. It contains uri, base, partOf, attributes and instances.
	 */
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

	/**
	 * @function
	 * @name initAutocomplete
	 * @memberOf appcontroller
	 * @description Function to initialize autocomplete structure. It contains formats, types, literals and instances with attributes and partOf assignments.
	 */
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

	/**
	 * @function
	 * @name init
	 * @memberOf appcontroller
	 * @description Function to initialize all used variables and formatId and formatEdit with correct values from URL.
	 * It calls the initVars method.
	 */
	$scope.init = () => {
		$scope.initVars();
		$scope.formatId = $location.search().formatId;
		$scope.formatEdit = $location.search().formatEdit;
	};

	// ====================== Format Actions =======================
	/**
	 * @function
	 * @name createOrUpdateDraftFormat
	 * @memberOf appcontroller
	 * @description A function to create or update a specified draft format.
	 * It uses createNewDraftFormat and  updateDraftFormat functions.
	 * The createNewDraftFormatFunction is used, when $scope.formatId contains FORMAT_NEW constant as id.
	 */
	$scope.createOrUpdateDraftFormat = () => {
		if ($scope.formatId === FORMAT_NEW)
			$scope.createNewDraftFormat();
		else
			$scope.updateDraftFormat();
	}

	/**
	 * @function
	 * @name updateDraftFormat
	 * @memberOf appcontroller
	 * @description A function to update actual draft format being shown on the website. It validates the
	 * specified format data and if validation did not fail it updates the format via the setDraftFormat function to execute the RPC call.
	 * It obtains the formatId via location.search.
	 */
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

	/**
	 * @function
	 * @name createNewDraftFormat
	 * @memberOf appcontroller
	 * @description A function to create a new draft format after successfull validation. It also uses the setDraftFormat function to execute the RPC call.
	 * It obtains the formatId via location.search.
	 */
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

	/**
	 * @function
	 * @name getDraftFormatData
	 * @memberOf appcontroller
	 * @description A function to obtain a draft format from the database. It uses the getDraftFormat function to execute the RPC call.
	 */
	$scope.getDraftFormatData = () => {
		$scope.getDraftFormat($scope.formatId).then(data => {
			$scope.formatData = data;
		}, error => {
			console.log(error);
		});
	}

	/**
	 * @function
	 * @name getClassesForDraftFormat
	 * @memberOf appcontroller
	 * @description A function to obtain all classes for a draft format. It uses the listDraftClass function to execute the RPC call.
	 */
	$scope.getClassesForDraftFormat = () => {
		$scope.listDraftClass($scope.formatId).then(data => {
			console.log(`getClassesForDraftFormat(${$scope.formatId}):`, data);
			$scope.savedClasses = data.items;
			$scope.getAutocompleteTypes();
		}, error => {
			console.log(error);
		});
	}

	/**
	 * @function
	 * @name getClassesForDraftFormat
	 * @memberOf appcontroller
	 * @description A function to empty the format data.
	 */
	$scope.clearDraftFormatData = () => {
		$scope.formatData = {};
	}

	// ===================== Class Actions =======================
	/**
	 * @function
	 * @name createOrUpdateDraftClass
	 * @memberOf appcontroller
	 * @description A function to create or update a draft class. If class is created or
	 * update depends on classEdit variable. At first all repeating parts and subclasses are
	 * removed via the function removeRepeatingPartsAndSubclasses. It uses updateDraftClass and
	 * createNewDraftClass functions.
	 */
	$scope.createOrUpdateDraftClass = () => {
		$scope.removeRepeatingPartsAndSubclasses();

		if ($scope.classEdit)
			$scope.updateDraftClass();
		else
			$scope.createNewDraftClass();
	}

	/**
	 * @function
	 * @name createNewDraftClass
	 * @memberOf appcontroller
	 * @description A function to create a new draft class. It uses setDraftClass function for the RPC call.
	 * After the RPC call is done all local existing draft class data are cleared. Furthermore all classes
	 * for a draft format are newly obtained to have consistency on the website shown to the user.
	 */
	$scope.createNewDraftClass = () => {
		if (!$scope.validateClassData()) {
			console.log('createNewDraftClass(): Input validation failed.', $scope.familyTree[0]);
			return;
		}

		$scope.setDraftClass($scope.formatId, null, $scope.cleanJson($scope.familyTree[0])).then(data => {
			console.log(`createNewDraftClass(${$scope.formatId}, null):`, $scope.familyTree[0], data);
			$scope.classId = data.id;
			$scope.getClassesForDraftFormat();
		}, error => {
			console.log(error);
		});
	}

	/**
	 * @function
	 * @name updateDraftClass
	 * @memberOf appcontroller
	 * @description A function to update a draft class. It uses setDraftClass function for the RPC call.
	 * After the RPC call is done all local existing draft class data are cleared. Furthermore all classes
	 * for a draft format are newly obtained to have consistency on the website shown to the user.
	 */
	$scope.updateDraftClass = () => {
		$scope.setDraftClass($scope.formatId, $scope.classId, $scope.cleanJson($scope.familyTree[0])).then(data => {
			console.log(`updateDraftClass(${$scope.formatId}, ${$scope.classId}):`, $scope.familyTree[0], data);
			$scope.getClassesForDraftFormat();
		}, error => {
			console.log(error);
		});
	}

	/**
	 * @function
	 * @name getDraftClassData
	 * @memberOf appcontroller
	 * @description A function to obtain data for a specified draft class. It uses getDraftClass function for the RPC call.
	 * After the RPC call is done familyTree is setted to be able to show the inheritance hierarchy for the obtained class.
	 * Therefore getFamilyTreeForClasses function is used. Furthermore the classEdit variable is setted to true because editing
	 * is starting. The activeClassUri variable is setted to the uri of the obtained class.
	 */
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

	/**
	 * @function
	 * @name getFamilyTreeForClasses
	 * @memberOf appcontroller
	 * @description A function to obtain a family tree for a class of the wikinormia. A family tree is an inheritance hierarchy.
	 * For this an array with cleaned base values has to be made For this an array with cleaned base values has to be made. All
	 * family members are sorted by its identifier.
	 */
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
	/**
	 * @function
	 * @name getAutocompleteFormat
	 * @memberOf appcontroller
	 * @description A function to obtain all available formats for auto completion.
	 * Therefore the listFormats function is used.
	 */
	$scope.getAutocompleteFormats = () => {
		$scope.listFormats().then(data => {
			if ($scope.autocomplete)
				$scope.autocomplete.formats = data.formats;
			else
				$scope.autocomplete = data;
		});
	}

	/**
	 * @function
	 * @name getAutocompleteFormat
	 * @memberOf appcontroller
	 * @description A function to obtain all types for auto completion. Therefore
	 * listAutocompleteTypes function is used. After the execution of the listAutocompleteTypes
	 * function all results are added to autocomplete.types variable.
	 */
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

	/**
	 * @function
	 * @name getAutocompleteLiterals
	 * @memberOf appcontroller
	 * @description A function to obtain all literals for auto completion. Therefore
	 * listLiteralTypes function is used. After the execution of the listLiteralTypes
	 * function all results are added to autocomplete.literals variable.
	 */
	$scope.getAutocompleteLiterals = () => {
		$scope.listLiteralTypes().then(data => {
			$scope.autocomplete.literals = data.literals;
		});
	}

	/**
	 * @function
	 * @name getARTypes
	 * @memberOf appcontroller
	 * @description A function to obtain all ARTypes for auto completion. Therefore
	 * listARTypes function is used. After the execution of the listARTypes
	 * function all results are added to artypes variable.
	 */
	$scope.getARTypes = () => {
		$scope.listARTypes().then(data => {
			$scope.artypes = data.artypes;
		});
	}

	/**
	 * @function
	 * @name getUnits
	 * @memberOf appcontroller
	 * @description A function to obtain all units for auto completion. Therefore
	 * listUnits function is used. After the execution of the listUnits
	 * function all results are added to units variable.
	 */
	$scope.getUnits = () => {
		$scope.listUnits().then(data => {
			$scope.units = data.units;
		});
	}

	/**
	 * @function
	 * @name getAutocompleteInstancesForPartOf
	 * @memberOf appcontroller
	 * @description A function to obtain all Instances for auto completion of partOf view. Therefore
	 * getAutocompleteInstances function is used. For parameter of the getAutocompleteInstances function
	 * an array with all partOfs is generated.
	 */
	$scope.getAutocompleteInstancesForPartOf = () => {
		const partOfUris = [];
		for (const partOf of $scope.familyTree[0].partOf)
			partOfUris.push(partOf.value);
		$scope.getAutocompleteInstances(partOfUris, 'partOf');
	}

	/**
	 * @function
	 * @name getAutocompleteInstancesForAttribute
	 * @memberOf appcontroller
	 * @description A function to obtain all instances for auto completion. Therefore
	 * getAutocompleteInstances function is used.
	 */
	$scope.getAutocompleteInstancesForAttribute = attr => {
		if (attr.literal) return;
		$scope.getAutocompleteInstances([attr.type]);
	}

	/**
	 * @function
	 * @name getAutocompleteInstances
	 * @memberOf appcontroller
	 * @description A function to get instances for autocomplete. Instances can be obtaines
	 * for partof or for attributes.
	 * @param {String[]} uris The uris to obtain instances for.
	 * @param {String} branch Variable to set if attributes or partOf instances should be obtained. As a
	 * standard attributes are obtained. No parameter has to be specified then in the call of
	 * the method.
	 */
	$scope.getAutocompleteInstances = (uris, branch = 'attributes') => {
		const newUris = [];

		for (const uri of uris)
			if (!$scope.hasProp($scope.autocomplete.instances[branch], uri))
				newUris.push(uri);

		if (!newUris.length) return;

		$scope.getInstances(newUris).then(instances => {
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
	/**
	 * @function
	 * @name watchGroup
	 * @memberOf appcontroller
	 * @description An angular function to watch formatId and formatEdit. If formatId changes, all classes, literals
	 * for the autocomplete and units are newly obtained via the corresponding function. If formatEdit changes all
	 * ARTypes are newly obtained.
	 */
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
	/**
	 * @function
	 * @name validateFormatData
	 * @memberOf appcontroller
	 * @description A function to validate the given formatData on the website. It uses validateInput function.
	 */
	$scope.validateFormatData = () => {
		return $scope.validateInput($scope.formatData, ['label', 'identifier', 'shortDescription', 'mimeType', 'artype']);
	}

	/**
	 * @function
	 * @name validateClassData
	 * @memberOf appcontroller
	 * @description A function to validate the given formatData on the website. It uses validateInput function.
	 */
	$scope.validateClassData = () => {
		return $scope.validateInput($scope.familyTree[0], ['label', 'identifier', 'shortDescription']);
	}

	/**
	 * @function
	 * @name validateInput
	 * @memberOf appcontroller
	 * @description A general function to validate input fields on the website. It uses a loop over the attributes
	 * and checks if some of it is undefined.
	 */
	$scope.validateInput = (obj, attrs) => {
		for (const attr of attrs)
			if (angular.isUndefined(obj[attr]))
				return false
		return true;
	}

	/**
	 * @function
	 * @name cleanJson
	 * @memberOf appcontroller
	 * @description A function to clean given JSON.
	 */
	$scope.cleanJson = angularJson => {
		return JSON.parse(angular.toJson(angularJson));
	}

	/**
	 * @function
	 * @name toCamelCase
	 * @memberOf appcontroller
	 * @description A function to convert a string to camel case.
	 */
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

	/**
	 * @function
	 * @name initFamilyTree
	 * @memberOf appcontroller
	 * @description A function to initialize a familyTree.
	 */
	$scope.initFamilyTree = () => {
		if ($scope.familyTree.length > 1)
			$scope.familyTree = $scope.familyTree.splice(0, 1);
	}

	/**
	 * @function
	 * @name convTypeUri
	 * @memberOf appcontroller
	 * @description A function to convert a uritype.
	 */
	$scope.convTypeUri = uri => {
		return $scope.uritypes[uri]?.label ?? 'N/A';
	}

	/**
	 * @function
	 * @name hasProp
	 * @memberOf appcontroller
	 * @description A function to check if an object has a given property
	 * @param {Object} obj The object to check the property on.
	 * @param {String} prop The property to be checked on the object.
	 */
	$scope.hasProp = (obj, prop) => {
		if (!obj) return false
		return Object.prototype.hasOwnProperty.call(obj, prop);
	}

	/**
	 * @function
	 * @name distinct
	 * @memberOf appcontroller
	 * @description A function to select distinct values from an array.
	 */
	$scope.distinct = (arr, comp) => {
		return arr.map(e => e[comp]).map((e, i, final) => final.indexOf(e) === i && i).filter((e) => arr[e]).map(e => arr[e]);
	}

	/**
	 * @function
	 * @name cleanJson
	 * @memberOf appcontroller
	 * @description A function to remove repeating parts and subclasses.
	 */
	$scope.removeRepeatingPartsAndSubclasses = () => {
		if (!$scope.familyTree.length) return;
		$scope.familyTree[0] = $scope.cleanJson($scope.familyTree[0]);
		$scope.familyTree[0].base = $scope.distinct($scope.familyTree[0].base, 'value');
		$scope.familyTree[0].partOf = $scope.distinct($scope.familyTree[0].partOf, 'value');
		for (const instance of $scope.familyTree[0].instances)
			instance.partOf = $scope.distinct(instance.partOf, 'value');
	}

	// ===================== Event Handlers =======================

	window.addEventListener('beforeunload', e => {
		e.preventDefault();
		e.returnValue = '';
	});

	window.addEventListener('unload', () => {
		$scope.createOrUpdateDraftClass();
	});

	/**
	 * @function
	 * @name discardDraftClassData
	 * @memberOf appcontroller
	 * @description Triggered after clicking the "Discard" button. 
	 * Asks the user whether to discard the form inputs and the corresponding model data.
	 */
	$scope.discardDraftClassData = () => {
		if (confirm('Unsaved changes are discarded. Continue?'))
			$scope.clearDraftClassData();
	}

	/**
	 * @function
	 * @name submitForm
	 * @memberOf appcontroller
	 * @description A function to submit a form. Therefore checkValidity and reportValidity funtions are used.
	 * @param {String} formId Id of the form which should be validated.
	 * @param {String} next Name of the next method to be called after submitting was done.
	 */
	$scope.submitForm = (formId, next) => {
		const form = $(formId)[0];
		if (!form.checkValidity())
			form.reportValidity();
		else
			next();
	}


	/**
	 * @function
	 * @name clearDraftClassData
	 * @memberOf appcontroller
	 * @description A function to clear draft class data stored at the clients machine.
	 * All variables are setted to undefined or false and then initClassData function is called.
	 */
	$scope.clearDraftClassData = () => {
		$scope.classId = undefined;
		$scope.activeClassUri = undefined;
		$scope.classEdit = false;
		$scope.initClassData();
	}

	/**
	 * @function
	 * @name deleteSelectedDraftClass
	 * @memberOf appcontroller
	 * @description A function to delete the actually in the UI selected draft class. It is called if the
	 * user hits the delete button. For security a confirmation is shown. If the user confirms the deletion
	 * dropDraftClass function is called for the corresponding RPC call. When this is done clearDraftClassData
	 * and getClassesForDraftFormat functions are called for consistency. All correpsonding class data are now
	 * removed from the database and the clients local store.
	 */
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

	/**
	 * @function
	 * @name publishDraftFormat
	 * @memberOf appcontroller
	 * @description A function to publish the given draft format data. A confirmation is needed to publish. Then
	 * the format is published to the Wikinormia by executing the publishDraft function. After successfull publishing
	 * the user is redirected to the wikinormia.html and all data stored by client are cleaned.
	 */
	$scope.publishDraftFormat = () => {
		if (!window.confirm('Do you really want to publish the current draft format?'))
			return;

		$scope.publishDraft($scope.formatId).then(data => {
			if(data.success)
				window.location.href = 'wikinormia.html?page=' + $scope.formatData.identifier;
		});
	}

	/**
	 * @function
	 * @name editSelectedDraftFormat
	 * @memberOf appcontroller
	 * @description A function to show that format has been edited. The variable formatEdit is set to true and formatId and
	 * formartEdit are obtained by looking at the URL.
	 */
	$scope.editSelectedDraftFormat = () => {
		$scope.formatEdit = true;
		$location.search({
			[FORMAT_ID]: $scope.formatId,
			[FORMAT_EDIT]: $scope.formatEdit
		});
	}

	/**
	 * @function
	 * @name deleteSelectedDraftFormat
	 * @memberOf appcontroller
	 * @description A function to delete a format. This function is called when the user hits the delete button for a format. dropDraftFormat
	 * function is used to delete the format via RPC call at the database. After that clearDraftFormatData is called and all related variables
	 * are setted to the now correct values.
	 */
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

	/**
	 * @function
	 * @name autoCompleteIdentfier
	 * @memberOf appcontroller
	 * @description A function to implement the autocomplete in the UI for the identifier.
	 */
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

	/**
	 * @function
	 * @name addItem
	 * @memberOf appcontroller
	 * @description A function to add an item to a collection. It is reused from wikinormia.html.
	 * @param {Object[]} collection Collection to add item to.
	 * @param {Object} item Item to add to the specified collection.
	 * @param {Integer} position Position to add item at.
	 */
	$scope.addItem = (collection, item = {}, position) => {
		if (!position) {
			collection.push(item);
		} else
			collection.splice(position, 0, item);
	}

	/**
	 * @function
	 * @name addInstanceItem
	 * @memberOf appcontroller
	 * @description A function to add an empty item to the instances.
	 */
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

	/**
	 * @function
	 * @name initInstanceMember
	 * @memberOf appcontroller
	 * @description A function to initialize a memeber of an instance. Therfore two indices are used.
	 */
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

	/**
	 * @function
	 * @name deleteItem
	 * @memberOf appcontroller
	 * @description A function to an item in a collection.
	 * @param {Object[]} collection Collection to delete item from.
	 * @param {Object} item Item to delete from specified collection.
	 */
	$scope.deleteItem = (collection, item) => {
		const index = collection.indexOf(item);
		if (index >= 0) collection.splice(index, 1);
	}

	/**
	 * @function
	 * @name deleteSubclassItem
	 * @memberOf appcontroller
	 * @description A function to delete a subclass item from a collection
	 * @param {Object[]} collection Collection to delete subclass item from.
	 * @param {Object} item Subclass item to delete from collection.
	 */
	$scope.deleteSubclassItem = (collection, item) => {
		const index = collection.indexOf(item);
		if (index >= 0) collection.splice(index, 1);
		$scope.getFamilyTreeForClasses();
	}

	/**
	 * @function
	 * @name changeClassTab
	 * @memberOf appcontroller
	 * @description The function which is used to change the class tab.
	 */
	$scope.changeClassTab = uri => {
		$scope.activeClassUri = uri;
	}

	/**
	 * @function
	 * @name instancePartOfsExists
	 * @memberOf appcontroller
	 * @description Checks whether part of instances exist inside the autocomplete object. 
	 */
	$scope.instancePartOfsExists = () => {
		const values = Object.values($scope.autocomplete.instances.partOf);
		for (const value of values)
			if (value.list.length > 0)
				return true;
		return false;
	}

	// ======================== RPC calls =========================
	/**
	 * @function
	 * @name getDraftClass
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to obtain a draft class.
	 * The used endpoint is wikinormia/getDraft.
	 */
	$scope.getDraftClass = classId => {
		return $q((resolve, reject) => {
			if (!classId) {
				reject(new Error(`[getDraftClass] classId is ${classId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'getDraft', [classId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name getDraftFormat
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to obtain a draft format.
	 * The used endpoint is format/getDraftFormat.
	 */
	$scope.getDraftFormat = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[getDraftFormat] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'getDraft', [formatId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listDraftClass
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list a draft class.
	 * The used endpoint is wikinormia/listDraftClass.
	 */
	$scope.listDraftClass = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[listDraftClass] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'listDraft', [formatId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name setDraftFormat
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to set a draft format.
	 * The used endpoint is format/setDraftFormat.
	 */
	$scope.setDraftFormat = (formatId, formatData) => {
		return $q((resolve, reject) => {
			if (!formatData) {
				reject(new Error(`[setDraftFormat] formatData is ${formatData}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'setDraft', [formatId, formatData], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name setDraftClass
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to set a draft class.
	 * The used endpoint is wikinormia/setDraftFormat.
	 */
	$scope.setDraftClass = (formatId, classId, classDataJson) => {
		return $q((resolve, reject) => {
			if (!formatId || !classDataJson) {
				reject(new Error('[setDraftClass] formatId or classDataJson is not set:', formatId, classDataJson));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'setDraft', [formatId, classId, classDataJson], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name dropDraftFormat
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to delete a draft format.
	 * The used endpoint is format/dropDraftFormat.
	 */
	$scope.dropDraftFormat = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[dropDraftFormat] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('format', 'dropDraft', [formatId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name dropDraftClass
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to delete a draft class.
	 * The used endpoint is wikinormia/dropDraft.
	 */
	$scope.dropDraftClass = classId => {
		return $q((resolve, reject) => {
			if (!classId) {
				reject(new Error(`[dropDraftClass] classId is ${classId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'dropDraft', [classId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listUnits
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list units.
	 * The used endpoint is wikinormia/listUnits.
	 */
	$scope.listUnits = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listUnits', [], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listAutocompleteTypes
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list auto completation types.
	 * The used endpoint is wikinormia/listAutocompleteTypes.
	 */
	$scope.listAutocompleteTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listAutocompleteTypes', [], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listLiteralTypes
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list literal types.
	 * The used endpoint is wikinormia/listLiteralTypes.
	 */
	$scope.listLiteralTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'listLiteralTypes', [], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listLiteralTypes
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list literal types.
	 * The used endpoint is format/listLiteralTypes.
	 */
	$scope.listFormats = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'listFormats', [], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name listARTypes
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to list ARTypes.
	 * The used endpoint is format/ARTypes.
	 */
	$scope.listARTypes = () => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('format', 'listARTypes', [], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name getFamilyTree
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to get a family tree.
	 * The used endpoint is wikinormia/getFamilytree.
	 */
	$scope.getFamilyTree = baseUris => {
		return $q(resolve => {
			$scope.sdsd.rpcCall('wikinormia', 'getFamilyTree', [baseUris], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name publishDraft
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to publish a draft.
	 * The used endpoint is wikinormia/publishDraft.
	 */
	$scope.publishDraft = formatId => {
		return $q((resolve, reject) => {
			if (!formatId) {
				reject(new Error(`[publishDraft] formatId is ${formatId}.`));
				return;
			}

			$scope.sdsd.rpcCall('wikinormia', 'publishDraft', [formatId], data => resolve(data));
		});
	}

	/**
	 * @function
	 * @name getInstances
	 * @memberOf appcontroller
	 * @description A function to encapsulate the RPC call to obtain instances.
	 * The used endpoint is wikinormia/getInstances.
	 */
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
/**
 * @function
 * @name jqueryValidity
 * @memberOf appcontroller
 * @description A function to set a customized text for validation warnings.
 */
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
