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

class Row {
	constructor({
		columns = 1,
		elements = []
	}) {
		this._col = 'col-' + Math.floor(12 / columns);
		this._elements = elements;
	}

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

class Column {
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

	set class($class) {
		this._class = $class;
	}

	toString() {
		return `<div id="${this._id}" class="${this._class}">${this._element}</div>`;
	}
}

class Card {
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

class Spinner {
	constructor({
		icon = 'arrow-repeat',
		width = 32,
		height = 32
	} = {}) {
		this._icon = icon;
		this._width = width;
		this._height = height;
	}

	toString() {
		return `<img src="${ICON_PATH}${this._icon}.svg" class="fa-spin" alt="arrow-repeat" width="${this._width}" height="${this._height}" />`;
	}
}

class Loading {
	constructor({
		element = new Spinner()
	} = {}) {
		this._element = element;
	}

	toString() {
		const loading = new Card({
			$class: 'loading align-middle text-center',
			elements: [this._element]
		});
		return loading.toString();
	}
}

class Details {
	constructor({
		summary = '',
		element = '',
		$class = ''
	}) {
		this._summary = summary;
		this._element = element;
		this._class = $class;
	}

	get summary() {
		return this._summary;
	}

	get element() {
		return this._element;
	}

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

class Table {
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

	get header() {
		return this._header;
	}

	get rows() {
		return this._rows;
	}

	addRow(row) {
		this._rows.push(row);
	}

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

class Toast {
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

class Icon {
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

	set data(data) {
		this._data = data;
	}

	toString() {
		return `<img src="${ICON_PATH}${this._name}.svg" width="${this._size}" height="${this._size}" class="${this._class}" ${this._data} alt="${this._name}" />`;
	}
}

class Link {
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

	get text() {
		return this._text;
	}

	toString() {
		return `<a href="${this._href}" ${this._target} class="${this._class}" ${this._data}>${this._text}</a>`;
	}
}

class Input {
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

	toString() {
		return `<input type="${this._type}" placeholder="${this._placeholder}" id="${this._id}" class="${this._class}" />`;
	}
}

class Button {
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

	toString() {
		return `<button type="button" class="btn ${this._color} ${this._class}" ${this._data}>${this._element}</button>`;
	}
}

class Span {
	constructor({
		id = '',
		$class = '',
		text = ''
	}) {
		this._id = id;
		this._class = $class;
		this._text = text;
	}

	toString() {
		return `<span id="${this._id}" class="${this._class}">${this._text}</span>`;
	}
}

class List {
	constructor({
    items = [],
    id = ''
	}) {
    this._items = items;
    this._id = id;
	}

	get items() {
		return this._items;
	}

	toString() {
		let html = `<div id="${this._id}" class="list-group list-group-flush">`;
		for (const item of this._items)
			html += item;
		html += '</div>';

		return html;
	}
}

class ListItem {
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

class Bold {
	constructor({
		text = ''
	}) {
		this._text = text;
	}

	toString() {
		return '<b>' + this._text + '</b>';
	}
}

class Header {
	constructor({
		h = 2,
		text = ''
	}) {
		this._h = h;
		this._text = text;
	}

	toString() {
		return `<h${this._h}>${this._text}</h${this._h}>`;
	}
}

const NO_DATA = new Span({
	$class: 'dash-no-data text-warning',
	text: 'No data available'
});
