/**
 * @author Noah Grosse Starmann
 * @author Andreas Schliebitz
 */
'use strict';
/**
 * A function to hide an element.
 * @param {String} elem The element name of the element to hide.
 */
function hide(elem) {
	$(elem).hide();
}
/**
 * A function to show an hidden element.
 * @param {String} elem The element name of the element to be shown again.
 */
function show(elem) {
	$(elem).show();
}
/**
 * A function to change the HTML via JQuery.
 * @param {String} elem The element name of the element to change.
 * @param {String} html HTML which should be used by the changed element.
 */
function html(elem, html) {
	$(elem).html(html.toString());
}
/**
 * A function to get the HTML of an element.
 * @param {String} elem The element name of the element to get the HTML of.
 * @return {String} The HTML representation of the specified element.
 */
function getHtml(elem) {
	return $(elem).html();
}
/**
 * A function to append specified HTML to an element via JQuery.
 * @param {String} elem The element name of the element to append the HTML to.
 * @param {String} html The HTML to be appended to the element.
 */
function append(elem, html) {
	$(elem).append(html.toString());
}
/**
 * A function to prepend specified HTML to an element via JQuery.
 * @param {String} elem The element name of the element to prepend the HTML to.
 * @param {String} html The HTML to be prepended to the element.
 */
function prepend(elem, html) {
	$(elem).prepend(html.toString());
}
/**
 * A function to clear an element via JQuery function empty.
 * @param {String} elem The element name of the element to clear.
 */
function clear(elem) {
	$(elem).empty();
}
/**
 * A function to switch the the HTML class of an element to a new class and remove the old one.
 * @param {String} elem The element name of the element to change the class from.
 * @param {String} oldClass Name of the old class to remove it.
 * @param {String} newClass Name of the new class to switch it.
 */
function switchClass(elem, oldClass, newClass) {
	$(elem).removeClass(oldClass).addClass(newClass);
}
/**
 * A function to get the attribute of an element.
 * @param {String} elem The element name of the element to get the attribute from.
 * @param {String} attr The attribute name of the element to get.
 * @return {String} The attribute to get.
 */
function getAttr(elem, attr) {
	return $(elem).attr(attr);
}
/**
 * A function to set the attribute of an element to a specified value.
 * @param {String} elem The element name of the element to set the attribute of.
 * @param {String} attr The name of the attribute to set the value.
 * @param {String} val The value to set the attribute to.
 */
function setAttr(elem, attr, val) {
	$(elem).attr(attr, val);
}
/**
 * A function to add a new class to an element.
 * @param {String} elem The element name of the element to add a new class to.
 * @param {String} newClass The new class to be added to an element.
 */
function addClass(elem, newClass) {
	$(elem).addClass(newClass);
}
/**
 * A function to remove a class of an element.
 * @param {String} elem The element name of the element to add a new class to.
 * @param {String} oldClass The old class to be removed of an element.
 */
function removeClass(elem, oldClass) {
	$(elem).removeClass(oldClass);
}
/**
 * A function to check if an object is empty.
 * @param {Object} object The object which should be empty.
 * @return {Boolean} Shows if an object is empty or not.
 */
function emptyObj(obj) {
	return Object.keys(obj).length === 0;
}
/**
 * A function to check if a group by is empty.
 * @param {Object} object The object to empty.
 * @param {String} col The column to empty.
 * @return {Boolean} Shows if a groupby is empty or not.
 */
function emptyGroupBy(obj, col) {
	return obj[0][col] === '';
}
/**
 * A function to remove an element from the DOM via JQuery.
 * @param {String} elem The element name of the element to remove from the DOM.
 */
function remove(elem) {
	$(elem).remove();
}
/**
 * A function to move an element in the DOM.
 * @param {Object} src Source element to move to dst.
 * @param {Object} dst Target to move the source element to.
 */
function move(src, dst) {
	if (emptyObj($(src)) || emptyObj($(dst))) return;
	$(src).detach().appendTo(dst);
}
/**
 * Setter for the height of an element.
 * @param {String} elem The element name of the element to remove from the DOM.
 * @param {Integer} height Targeted height for the element.
 */
function setHeight(elem, height) {
	$(elem).height(height);
}
/**
 * Getter for the height of an element.
 * @param {String} elem The element name of the element to get the height for.
 * @return {Integer} The requested height of an element.
 */
function getHeight(elem) {
	return $(elem).height();
}
/**
 * A function to reset the height for an element.
 * @param {String} elem The element name of the element to  reset the height of.
 */
function resetHeight(elem) {
	$(elem).css('height', '');
}
/**
 * A function to create an object from a JSON string.
 * @param {String} str The JSON string to create an object of.
 * @return {Object} The object which was parsed of the JSON string.
 */
function fromJson(str) {
	return JSON.parse(str);
}
/**
 * A function to create a JSON string of an object.
 * @param {String} str The string to create a JSON of.
 * @return {String} The string representation of the object.
 */
function toJson(obj) {
	return JSON.stringify(obj);
}
/**
 * A function to join an array.
 * @param {String[]} array The array to join.
 * @param {String} separator The separator to join the array with. Standard value is a comma.
 * @return {String[]} The joined array.
 */
function join(array, separator = ', ') {
	return array.join(separator);
}
/**
 * A function to replace in a string.
 * @param {String} str String to replace with.
 * @param {String[]} replacements The replacements to use.
 * @return {String} The string with replacements.
 */
function replace(str, replacements) {
	for (let searchValue in replacements)
		str = str.replace(searchValue, replacements[searchValue])
	return str;
}
/**
 * A function to check if an array is empty.
 * @param {Object[]} The array which should be checked.
 * @return {Boolean} Shows, if array is empty or not.
 */
function empty(array) {
	return Array.isArray(array) && !array.length;
}
/**
 * A function to obtain the values of an object.
 * @param {Object} object The object to get the values of.
 * @return {String[]} An array of the values of the object.
 */
function values(obj) {
	if (!obj)
		return [];
	return Object.values(obj);
}
/**
 * A function to obtain the keys of an object.
 * @param {Object} object The object to get the keys of.
 * @return {String[]} An array of the keys of the object.
 */
function keys(obj) {
	return Object.keys(obj);
}
/**
 * A function to get the first value of an array.
 * @param {Object[]} array The array to get the value from.
 * @param {String/Object} The first value of the array.
 */
function first(array) {
	return array[0];
}
/**
 * A function to get the last value of an array.
 * @param {Object[]} array The array to get the value from.
 * @param {String/Object} The last value of the array.
 */
function last(array) {
	return array[array.length - 1];
}
/**
 * A function to remove types from a row.
 * @param {String[]} The rows to remove types of.
 */
function removeTypes(rows) {
	for (const row of rows)
		for (const col in row)
			row[col] = row[col].split('^^')[0];
}
/**
 * A function to capitalize the letters of a string.
 * @param {String} string The string to be capitalized.
 * @return {String} The capitalized string.
 */
function capitalize(string) {
	return string.charAt(0).toUpperCase() + string.slice(1);
}
/**
 * A function to convert an attribute to name.
 * @param {String} attribute The attribute to convert.
 * @return {String} The converted string.
 */
function camelToName(attr) {
	attr = replace(attr, {
		'_': ''
	});
	const parts = attr.split(/(?=[A-Z])/);
	return capitalize(join(parts, ' '));
}
/**
 * A function to show a loading circle.
 * @param {String} elem The name of the element to add the loading circle to.
 * @add {Boolean} add A boolean parameter, which is true if the loading circle should be prepended using prepend function. Otherwise html function will be used.
 */
function showLoading(elem, add = false) {
	if (add)
		prepend(elem, new Loading());
	else
		html(elem, new Loading());
}
/**
 * A function to remove the loading circle of an element.
 * @param {String} elem The element name of the element to  remove the loading of.
 */
function removeLoading(elem) {
	remove(elem + ' .loading');
}
/**
 * A function which checks, if the given string is hex format or not.
 * @param {String} hex The string to check.
 * @return {Boolean} Boolean which signals, if string is hex or not.
 */
function isHex(hex) {
	return typeof(hex) === 'string' &&
		hex.length % 2 == 0 &&
		!isNaN(Number('0x' + hex));
}
/**
 * A function to create a timelog link.
 * @param {String} fileId The fileId to create a timelog link with.
 * @param {String} tlgName The name of the timelog to create a link with.
 * @param {String} dlv Optional parameter for a data log value.
 * @return {String} created timelog link.
 */
function timeLogLink(fileId, tlgName, dlv = null) {
	fileId = replace(fileId, {
		'sdsd:': ''
	});
	const urlmap = new UrlEncoder();
	urlmap.addLayer('timelog', fileId, tlgName, dlv, null, true);
	return urlmap.getLink();
}
/**
 * A function to format a date.
 * @param {String} utcStr The UTC date string to format.
 * @param {Object} opts Optional parameters for the formatting.
 * @return {String} The formatted date string.
 */
function dateFormat(utcStr, opts = {}) {
	const date = new Date(utcStr);
	const dateStr = date.toLocaleDateString('en-GB', opts);
	const timeStr = date.toLocaleTimeString();
	return dateStr + ' ' + timeStr;
}
/**
 * A function to get the label of a row.
 * @param {String} elem The element name of the element to get the row label of.
 * @param {Integer} nth Column number to get the row label of.
 * @return {String} The label of the specified row.
 */
function getRowLabel(elem, nth = 0) {
	return $(elem).closest('tr').find('td:eq(' + nth + ')').text();
}
/**
 * A function to push a state to the browser history.
 * @param {String} hashName The value to add to the browser history.
 */
function historyPushState(hashName) {
	history.pushState(null, null, hashName);
}
/**
 * A function to the actual state in the browser history.
 * @param {String} hashName The value to replace the actual value in the browser history with.
 */
function historyReplaceState(hashName) {
	history.replaceState(null, null, hashName);
}
/**
 * A function to make a DOM element visible.
 * @return {String} The now visible element.
 */
function visible(elem) {
	return $(elem).is(':visible');
}
/**
 * A function to check if an object has a specified property.
 *  @param {Object} obj The object to check the property at.
 *  @return {Boolean} Boolean which shows, if the object has the property or not.
 */
function hasProp(obj, prop) {
	if (!obj) return false
	return Object.prototype.hasOwnProperty.call(obj, prop);
}
/**
 * A function to get a DDI from an uri.
 * @param {String} ddiUri The ddiUri to get the DDI of.
 */
function ddiFromUri(ddiUri) {
	return +(ddiUri.split('_').pop());
}
/**
 * A function to search a value in an array of elements.
 * @param {String} needleVal The value to search in haystack.
 * @param {String[]} haystack The array to be searched.
 */
function search(needleVal, haystack) {
	$.each($(haystack), (i, elem) => {
		const haystackVal = $(elem).text();
		if (haystackVal.toUpperCase().indexOf(needleVal.toUpperCase()) > -1) {
			$(elem).show();
		} else {
			$(elem).hide();
		}
	});
}
