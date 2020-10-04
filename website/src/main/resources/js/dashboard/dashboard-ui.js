'use strict';

const DASHBOARD_ID = '#dashboard';

const DASH_TAB_CLASS = '.dash-tab';
const DASH_MAP_CLASS = '.dash-map';
const DASH_POPUP_NAV_CLASS = '.dash-popup-nav';

const DASH_NAV_TABS_ID = '#dash-nav-tabs';
const DASH_TAB_FIELDS_ID = '#dash-tab-fields';
const DASH_TAB_MACHINES_ID = '#dash-tab-machines';

const DASH_MAP_ID = '#dash-map';
const DASH_MAP_FIELDLIST_ID = '#dash-map-fieldlist';
const DASH_MAP_FIELDLIST_SEARCH_ID = DASH_MAP_FIELDLIST_ID + '-search';
const DASH_MAP_FIELDLIST_FIELDS_ID = DASH_MAP_FIELDLIST_ID + '-fields';
const DASH_MAP_SMALL_ID = DASH_MAP_ID + '-small';
const DASH_MAP_LARGE_ID = DASH_MAP_ID + '-large';

const DASH_NAV_BACK_ID = '#dash-nav-back';
const DASH_TASK_NAV_CLASS = '.dash-task-nav';
const DASH_MACHINE_NAV_CLASS = '.dash-machine-nav';
const DASH_FIELD_TASK_INFO_ID = '#dash-field-task-info';
const DASH_TASKS_TABLE_ID = '#dash-tasks-table';
const DASH_RELATED_TIMELOGS_TABLE_ID = '#dash-related-timelogs-table';
const DASH_FIELD_INFO_ID = '#dash-field-info';

const DASH_DETAILS_ID = '#dash-details';
const DASH_DETAILS_TASKLABEL_ID = DASH_DETAILS_ID + '-tasklabel';
const DASH_DETAILS_FARMINFO_ID = DASH_DETAILS_ID + '-farminfo';
const DASH_DETAILS_WORKERINFO_ID = DASH_DETAILS_ID + '-workerinfo';
const DASH_DETAILS_CUSTOMERINFO_ID = DASH_DETAILS_ID + '-customerinfo';
const DASH_DETAILS_MACHINEINFO_ID = DASH_DETAILS_ID + '-machineinfo';
const DASH_DETAILS_TIMES_ID = DASH_DETAILS_ID + '-times';
const DASH_DETAILS_TASKTIMES_ID = DASH_DETAILS_ID + '-tasktimes';
const DASH_DETAILS_PRODUCTALLOCATIONINFO_ID = DASH_DETAILS_ID + '-productallocationinfo';

const DASH_MACHINES_TABLE_ID = '#dash-machines-table';
const DASH_MACHINE_DETAILS_ID = '#dash-machine-details';
const DASH_MACHINE_DETAILS_MACHINELABEL_ID = DASH_MACHINE_DETAILS_ID + '-machinelabel';
const DASH_MACHINE_DETAILS_DEVICEELEMENTINFO_ID = DASH_MACHINE_DETAILS_ID + '-deviceelementinfo';
const DASH_MACHINE_DETAILS_TIMELOGINFO_ID = DASH_MACHINE_DETAILS_ID + '-timeloginfo';
const DASH_MACHINE_DETAILS_DEVICEELEMENTPROPERTIES_ID = DASH_MACHINE_DETAILS_ID + '-devicepropertiesinfo';
const DASH_MACHINE_DETAILS_DEVICEELEMENTPROCESSDATA_ID = DASH_MACHINE_DETAILS_ID + '-deviceprocessdatainfo';

const DATA_TAB = 'data-tab';
const DATA_TARGET = 'data-target';
const DATA_TASKID = 'data-taskid';
const DATA_TASKGRAPH = 'data-taskgraph';
const DATA_MACHINERES = 'data-machineres';
const DATA_LEAFLETID = 'data-leafletid';

const HASH_HOME = '#home';
const HASH_TASKS = '#tasks';
const HASH_TASK_DETAILS = '#task-details';
const HASH_MACHINE_DETAILS = '#machine-details';

const TO_CLEAR = [
	DASH_MAP_ID,
	DASH_MAP_FIELDLIST_ID,
	DASH_TASKS_TABLE_ID,
	DASH_RELATED_TIMELOGS_TABLE_ID,
	DASH_FIELD_INFO_ID,
	DASH_DETAILS_TASKLABEL_ID,
	DASH_DETAILS_CUSTOMERINFO_ID,
	DASH_DETAILS_FARMINFO_ID,
	DASH_DETAILS_WORKERINFO_ID,
	DASH_DETAILS_MACHINEINFO_ID,
	DASH_DETAILS_TIMES_ID,
	DASH_DETAILS_TASKTIMES_ID,
	DASH_MACHINES_TABLE_ID,
	DASH_MACHINE_DETAILS_MACHINELABEL_ID,
	DASH_MACHINE_DETAILS_DEVICEELEMENTINFO_ID,
	DASH_MACHINE_DETAILS_TIMELOGINFO_ID,
	DASH_MACHINE_DETAILS_DEVICEELEMENTPROPERTIES_ID,
	DASH_MACHINE_DETAILS_DEVICEELEMENTPROCESSDATA_ID,
	DASH_DETAILS_PRODUCTALLOCATIONINFO_ID
];

const NA = 'N/A';
const ICON_PATH = '/img/bi/';
/**
 * A class which represents a row in a table.
 */
class Row {
	/**
	 * @constructor
	 * @partOf Row
	 * @description Constructor, which sets the elements and the Bootstrap class for the row.
	 * @param {Integer} columns Number of columns
	 * @param {string[]} elements Elements to be shown in the row.
	 */
	constructor({
		columns = 1,
		elements = []
	}) {
		this._col = 'col-' + Math.floor(12 / columns);
		this._elements = elements;
	}
	/**
	 * A toString method which generates HTML to use on a website.
	 * @return {String} HTML string of row
	 */
	toString() {
		let html = '<div class="row">';
		for (let element of this._elements) {
			if (element instanceof Column)
				element.class = this._col;
			else
				element = new Column({
					$class: this._col,
					element: element
				});
			html += element;
		}
		html += '</div>';
		return html;
	}
}
/**
 * A class which represents a column in a table.
 */
class Column {
	/**
	 * Constructor, which sets the id, the class and the element.
	 * @param {String} id Id to use for column.
	 * @param {String} class HTML/Bootstrap class to use for column.
	 * @param {String[]} element Elements to show in the column.
	 */
	constructor({
		id = '',
		$class = '',
		element = ''
	}) {
		this._id = replace(id, {
			'#': ''
		});
		this._class = $class;
		this._element = element;
	}
	/**
	 * A setter for the class.
	 * @param {String} Class to set.
	 */
	set class($class) {
		this._class = $class;
	}
/**
 * A toString method to generate HTML.
 * @return {String} HTML string of the column.
 */
	toString() {
		return `<div id="${this._id}" class="${this._class}">${this._element}</div>`;
	}
}

/**
 * A class which represents a card in the UI.
 */
class Card {
	/**
	 * Constructor, which sets the class, the title, the subtitle and the elements of a Bootstrap card.
	 * @param {String} class HTML/Bootstrap class to use for the card.
	 * @param {String} title Title to show within the card.
	 * @param {String} subtitle Subtitle to show within the card.
	 * @param {String[]} elements Elements to show in the card.
	 */
	constructor({
		$class = '',
		title = '',
		subtitle,
		elements = []
	}) {
		this._class = $class;
		this._title = title;
		this._subtitle = subtitle;
		this._elements = elements;
	}
	/**
	 * A toString method to generate the corresponding HTML of the card with.
	 * @return HTML string of the represented card.
	 */
	toString() {
		let html = `
			<div class="card ${this._class}">
				<div class="card-body">
		`;

		if (this._title)
			html += `<h3 class="card-title">${this._title}</h3>`;

		if (this._subtitle)
			html += `<h6 class="card-subtitle mb-2 text-muted">${this._subtitle}</h6>`;

		html += '<div class="card-text">';

		for (const elem of this._elements)
			html += elem;

		html += `</div>
				</div>
			</div>
		`;

		return html;
	}
}
/**
 * A class to represent a spinner to show loading processes with.
 */
class Spinner {
	/**
	 * Constructor, which sets the icon, the width and the heigt of the spinner.
	 * @param {String} icon The icon to use with the spinner. Default value is the icon arrow-repeat.
	 * @param {String} width Width to set the spinner to. Default value is 32.
	 * @param {String} height Height to set the spinner to. Default value is 32.
	 */
	constructor({
		icon = 'arrow-repeat',
		width = 32,
		height = 32
	} = {}) {
		this._icon = icon;
		this._width = width;
		this._height = height;
	}
	/**
	 * ToString method to generate the corresponding HTML for the spinner. Bootstrap is used to create the animation.
	 * @return HTML string of the represented spinner.
	 */
	toString() {
		return `<img src="${ICON_PATH}${this._icon}.svg" class="fa-spin" alt="arrow-repeat" width="${this._width}" height="${this._height}" />`;
	}
}
/**
 * A class to represent a loading animation. Uses the Spinner class.
 */
class Loading {
	/**
	 * Constructor, which sets the element. Default value is an Object of the spinner class.
	 * @param {Object} Object to show at loading.
	 */
	constructor({
		element = new Spinner()
	} = {}) {
		this._element = element;
	}
	/**
	 * ToString method to generate corresponding HTML. Bootstrap is used to set the location.
	 * @return HTML representation of the represented loading process.
	 */
	toString() {
		const loading = new Card({
			$class: 'loading align-middle text-center',
			elements: [this._element]
		});
		return loading.toString();
	}
}
/**
 * A class to represent HTML details function.
 */
class Details {
	/**
	 * Constructor, which sets the summary, the element and the class.
	 * @param {String} summary Summary for the corresponding details.
	 * @param {String[]} element Contains elements to show in the details.
	 * @param {String} class HTML/Bootstrap class to use for the details.
	 */
	constructor({
		summary = '',
		element = '',
		$class = ''
	}) {
		this._summary = summary;
		this._element = element;
		this._class = $class;
	}
	/**
	 * Getter for the summary.
	 * @return {String} Summary of the class.
	 */
	get summary() {
		return this._summary;
	}
	/**
	* Getter for the elements.
	* @return {String[]} Array of elements.
	*/
	get element() {
		return this._element;
	}
	/**
	 * ToString method to generate corresponding HTML. Bootstrap is used to set the location.
	 * @return HTML representation of the represented details.
	 */
	toString() {
		let html = `
			<details class="${this._class}">
		<summary>${this._summary}</summary>
		`;

		if (Array.isArray(this._element))
			for (const elem of this._element)
				html += elem;
		else
			html += this._element;

		html += `</details>`;
		return html;
	}
}
/**
 * A class to represent a HTML table.
 */
class Table {
	/**
	 * Constructor which sets the header, rows, caption, class, hover, striped and responsive.
	 * @param {String[]} header Header to be shown in the table.
	 * @param {String[]} rows Rows to be shown in the table.
	 * @param {String} caption Caption to be shown for the table.
	 * @param {String} class HTML/Bootstrap class to be shown in the table.
	 * @param {boolean} hover If this is setted true, the table line hovered will be highlighted in color.
	 * @param {boolean} striped If this is setted true, the table will be striped with different black-white colors.
	 * @param {String} responsive Sets if table should be repsonsive with Bootstrap. Standard value is 'table-responsive-lg'.
	 */
	constructor({
		header = [],
		rows = [],
		caption = '',
		$class = '',
		hover = true,
		striped = true,
		responsive = 'table-responsive-lg'
	}) {
		this._header = header;
		this._rows = rows;
		this._caption = caption;
		this._class = $class;
		this._hover = hover;
		this._striped = striped;
		this._responsive = responsive;
	}
	/**
	 * Getter for the table header.
	 * @return {String[]} Corresponding header of the table.
	 */
	get header() {
		return this._header;
	}
	/**
	 * Getter for the rows of the table.
	 * @return {String[]} Corresponding rows of the table.
	 */
	get rows() {
		return this._rows;
	}
	/**
	 * Method to add a row to the table.
	 * @param {String} row Row to be added to the table.
	 */
	addRow(row) {
		this._rows.push(row);
	}
	/**
	 * ToString method to generate corresponding HTML for the table.
	 * @return {String} HTML representation of the table.
	 */
	toString() {
		let styles = '';
		if (this._hover) styles += 'table-hover ';
		if (this._striped) styles += 'table-striped ';
		styles += this._responsive;

		let html = `<table class="table ${this._class} ${styles}">`;

		if (this._caption)
			html += `<caption>${this._caption}</caption>`;

		html += '<thead><tr>';

		for (const head of this._header)
			html += `<th scope="col">${head}</th>`;

		html += '</tr></thead><tbody>';

		for (const row of this._rows) {
			html += '<tr>';
			for (const col in row)
				html += `<td>${row[col]}</td>`;
			html += '</tr>';
		}

		html += '</tbody></table>';
		return html;
	}
}
/**
 * A class to represent a Bootstrap toast (kind of alert message).
 */
class Toast {
	/**
	 * Constructor for the toast, which sets icon, title, content and small.
	 * @param {String} icon Icon, which should be used for the toast.
	 * @param {String} title Title, which should be used for the toast.
	 * @param {String} content Content, which should be used for the toast.
	 * @param {String} small Sets little text within the toast left of the close button (See Bootstrap documentation).
	 */
	constructor({
		icon = '',
		title = '',
		content = '',
		small = ''
	}) {
		this._icon = icon;
		this._title = title;
		this._small = small;
		this._content = content;
	}
	/**
	 * ToString method to generate corresponding HTML for the toast.
	 * @return {String} HTML representation of the toast.
	 */
	toString() {
		let html = `
			<div class="toast shadow-none" data-autohide="false">
				<div class="toast-header text-dark">
					${this._icon}
					 <strong class="mr-auto">${this._title}</strong>
		`;

		if (this._small)
			html += `<small>${this._small}</small>`;

		html += `
				</div>
				<div class="toast-body" title="${this._title}">
					${this._content}
				</div>
			</div>
		`;
		return html;
	}
}
/**
 * A class to represent an icon.
 */
class Icon {
	/** Constructor which sets the name, size, class and data of the icon.
	 * @param {String} name Name of the icon.
	 * @þaram {Integer} size Size of the icon.
	 * @param {String} class HTML/Bootstrap class of the icon.
	 * @param {String} data Data which should be stored within the icon.
	 */
	constructor({
		name = '',
		size = 16,
		$class = '',
		data = ''
	}) {
		this._name = name;
		this._size = size;
		this._class = $class;
		this._data = data;
	}
	/** Setter for the data to be stored within the icon.
	 * @param {String} data Data to be stored.
	 */
	set data(data) {
		this._data = data;
	}
	/**
	 * ToString method to generate the corresponding HTML for the icon.
	 * @return {String} HTML representation of the icon.
	 */
	toString() {
		return `<img src="${ICON_PATH}${this._name}.svg" width="${this._size}" height="${this._size}" class="${this._class}" ${this._data} alt="${this._name}" />`;
	}
}
/**
 * A class to represent a link.
 */
class Link {
	/** Constructor, which sets the href, class, target, text and data.
	 * @param {String} href Link to which points the link to. As standard javascript; is used because it is used with Javascript.
	 * @param {String} class HTML/Bootstrap class of the link.
	 * @param {String} target Target for the link. Standard value is _blank.
	 * @param {String} text Text to be shown for the link.
	 * @param {String} data Data to be stored within the link.
	 */
	constructor({
		href = 'javascript:;',
		$class = '',
		target = '_blank',
		text = '',
		data = ''
	}) {
		const isJavascript = href === 'javascript:;';

		if (isJavascript) {
			this._href = href;
		} else {
			try {
				const url = new URL(href);
				this._href = url.pathname + url.search;
			} catch (e) {
				this._href = href;
			}
		}

		this._target = isJavascript ? '' : `target="${target}"`;
		this._class = $class;
		this._text = text;
		this._data = data;
	}
	/**
	 * A getter for the text of the link
	 * @return {String} Text of the represented link.
	 */
	get text() {
		return this._text;
	}
	/**
	 * ToString method to generate corresponding HTML for the link.
	 * @return {String} HTML representation of the corresponding link.
	 */
	toString() {
		return `<a href="${this._href}" ${this._target} class="${this._class}" ${this._data}>${this._text}</a>`;
	}
}
/**
 * A class to represent an input field.
 */
class Input {
	/**
	 * Constructor, which sets the type, placeholder, id and class.
	 * @param {String} type Contains the data type of the represented input field. Standard value is text.
	 * @param {String} placeholder Placeholder to be shown in the input field when nothing is typed in.
	 * @param {String} id HTML id to identify the input field.
	 * @param {String} class HTML/Bootstrap class of the input field.
	 */
	constructor({
		type = 'text',
		placeholder = '',
		id = '',
		$class = ''
	}) {
		this._type = type;
		this._placeholder = placeholder;
		this._id = id;
		this._class = $class;
	}
	/**
	 * ToString method to create the corresponding HTML string.
	 * @return {String} HTML representation of the input field.
	 */
	toString() {
		return `<input type="${this._type}" placeholder="${this._placeholder}" id="${this._id}" class="${this._class}" />`;
	}
}
/**
 * A class to represent a button.
 */
class Button {
	/**
	 * Constructor, which sets the color, class, data and element for the button.
	 * @param {String} color Color of the button. Standard value is Bootstrap class btn-primary (See Bootstrap documentation).
	 * @param {String} class HTML/Bootstrap class of the button.
	 * @param {String} data Data to be stored within the button.
	 * @param {String} element Element to show within the button.
	 */
	constructor({
		color = 'btn-primary',
		$class = '',
		data = '',
		element = ''
	}) {
		this._color = color;
		this._class = $class;
		this._data = data;
		this._element = element;
	}
	/**
	 * ToString method to generate the corresponding HTML string for the button.
	 * @return {String} HTML representation of the corresponding button object.
	 */
	toString() {
		return `<button type="button" class="btn ${this._color} ${this._class}" ${this._data}>${this._element}</button>`;
	}
}
/**
 * A class to represent the HTML span element
 */
class Span {
	/**
	 * Constructor, which sets the id, class and text.
	 * @param {String} id HTML id for the span.
	 * @param {String} HTML/Bootstrap class for the span.
	 * @param {String} Text to show within the span.
	 */
	constructor({
		id = '',
		$class = '',
		text = ''
	}) {
		this._id = id;
		this._class = $class;
		this._text = text;
	}
	/**
	 * ToString method to generate the corresponding HTML string for
	 * @return {String} HTML representation of the corresponding span object.
	 */
	toString() {
		return `<span id="${this._id}" class="${this._class}">${this._text}</span>`;
	}
}
/**
 * A class to represent a HTML list.
 */
class List {
	/**
	 * Constructor, which sets the items and id.
	 * @param {String[]} Items to be shown in the list.
	 * @param {String} HTML id for the list.
	 */
	constructor({
		items = [],
		id = ''
	}) {
		this._items = items;
		this._id = id;
	}
	/**
	 * Getter for the items.
	 * @return {String[]} Items of the list.
	 */
	get items() {
		return this._items;
	}
	/**
	 * ToString method to generate the corresponding HTML string for the list.
	 * @return {String} HTML representation of the corresponding list object.
	 */
	toString() {
		let html = `<div id="${this._id}" class="list-group list-group-flush">`;
		for (const item of this._items)
			html += item;
		html += '</div>';

		return html;
	}
}
/**
 * A class to represent a list item.
 */
class ListItem {
	/**
	 * Constructor, which sets class, element, active and data.
	 * @param {String} class HTML/Bootstrap class for the ListItem.
	 * @param {Boolean} active Sets if list element is higlighted as active.
	 */
	constructor({
		$class = '',
		element = '',
		active = false,
		data = ''
	}) {
		this._class = $class;
		this._element = element;
		this._active = active;
		this._data = data;
	}
	/**
	 * ToString method to generate the corresponding HTML string for the list item.
	 * @return {String} HTML representation of the corresponding list item object.
	 */
	toString() {
		const highlight = this._active ? 'active' : '';
		return new Link({
			href: 'javascript:;',
			$class: `list-group-item list-group-item-action ${highlight} ${this._class}`,
			data: this._data,
			text: this._element
		}).toString();
	}
}
/**
 * A class to represent HTML bold text.
 */
class Bold {
	/**
	 * Constructor, which sets the text.
	 * @param {String} text Text to show bold.
	 */
	constructor({
		text = ''
	}) {
		this._text = text;
	}
	/**
	 * ToString method to generate the corresponding HTML string for the bold text.
	 * @return {String} HTML representation of the corresponding span object.
	 */
	toString() {
		return '<b>' + this._text + '</b>';
	}
}
/**
 * A class to represent a HTML header.
 */
class Header {
	/**
	 * Constructor, which sets the h value and the text.
	 * @param {Integer} h Sets the grade of the header. For example if h=2 h2 is used. Standard value is 2.
	 * @param {String} text Text to be shown in the header.
	 */
	constructor({
		h = 2,
		text = ''
	}) {
		this._h = h;
		this._text = text;
	}
	/**
	 * ToString method to generate the corresponding HTML string for the header.
	 * @return {String} HTML representation of the corrsponding header object.
	 */
	toString() {
		return `<h${this._h}>${this._text}</h${this._h}>`;
	}
}

const NO_DATA = new Span({
	$class: 'dash-no-data text-warning',
	text: 'No data available'
});
