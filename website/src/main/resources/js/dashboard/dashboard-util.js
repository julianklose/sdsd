'use strict';

function hide(elem) {
	$(elem).hide();
}

function show(elem) {
	$(elem).show();
}

function html(elem, html) {
	$(elem).html(html.toString());
}

function getHtml(elem) {
	return $(elem).html();
}

function append(elem, html) {
	$(elem).append(html.toString());
}

function prepend(elem, html) {
	$(elem).prepend(html.toString());
}

function clear(elem) {
	$(elem).empty();
}

function switchClass(elem, oldClass, newClass) {
	$(elem).removeClass(oldClass).addClass(newClass);
}

function getAttr(elem, attr) {
	return $(elem).attr(attr);
}

function setAttr(elem, attr, val) {
	$(elem).attr(attr, val);
}

function addClass(elem, newClass) {
	$(elem).addClass(newClass);
}

function removeClass(elem, oldClass) {
	$(elem).removeClass(oldClass);
}

function emptyObj(obj) {
	return Object.keys(obj).length === 0;
}

function emptyGroupBy(obj, col) {
	return obj[0][col] === '';
}

function remove(elem) {
	$(elem).remove();
}

function move(src, dst) {
	if (emptyObj($(src)) || emptyObj($(dst))) return;
	$(src).detach().appendTo(dst);
}

function setHeight(elem, height) {
	$(elem).height(height);
}

function getHeight(elem) {
	return $(elem).height();
}

function resetHeight(elem) {
	$(elem).css('height', '');
}

function fromJson(str) {
	return JSON.parse(str);
}

function toJson(obj) {
	return JSON.stringify(obj);
}

function join(array, separator = ', ') {
	return array.join(separator);
}

function replace(str, replacements) {
	for (let searchValue in replacements)
		str = str.replace(searchValue, replacements[searchValue])
	return str;
}

function empty(array) {
	return Array.isArray(array) && !array.length;
}

function values(obj) {
	if (!obj)
		return [];
	return Object.values(obj);
}

function keys(obj) {
	return Object.keys(obj);
}

function first(array) {
	return array[0];
}

function last(array) {
	return array[array.length - 1];
}

function removeTypes(rows) {
	for (const row of rows)
		for (const col in row)
			row[col] = row[col].split('^^')[0];
}

function capitalize(string) {
	return string.charAt(0).toUpperCase() + string.slice(1);
}

function camelToName(attr) {
	attr = replace(attr, {
		'_': ''
	});
	const parts = attr.split(/(?=[A-Z])/);
	return capitalize(join(parts, ' '));
}

function showLoading(elem, add = false) {
	if (add)
		prepend(elem, new Loading());
	else
		html(elem, new Loading());
}

function removeLoading(elem) {
	remove(elem + ' .loading');
}

function isHex(hex) {
	return typeof(hex) === 'string' &&
		hex.length % 2 == 0 &&
		!isNaN(Number('0x' + hex));
}

function timeLogLink(fileId, tlgName, dlv = null) {
	fileId = replace(fileId, {
		'sdsd:': ''
	});
	const urlmap = new UrlEncoder();
	urlmap.addLayer('timelog', fileId, tlgName, dlv, null, true);
	return urlmap.getLink();
}

function dateFormat(utcStr, opts = {}) {
	const date = new Date(utcStr);
	const dateStr = date.toLocaleDateString('en-GB', opts);
	const timeStr = date.toLocaleTimeString();
	return dateStr + ' ' + timeStr;
}

function getRowLabel(elem, nth = 0) {
	return $(elem).closest('tr').find('td:eq(' + nth + ')').text();
}

function historyPushState(hashName) {
	history.pushState(null, null, hashName);
}

function historyReplaceState(hashName) {
	history.replaceState(null, null, hashName);
}

function visible(elem) {
	return $(elem).is(':visible');
}

function hasProp(obj, prop) {
	if (!obj) return false
	return Object.prototype.hasOwnProperty.call(obj, prop);
}

function ddiFromUri(ddiUri) {
	return +(ddiUri.split('_').pop());
}

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
