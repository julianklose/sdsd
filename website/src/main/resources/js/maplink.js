/**
 * SDSD client functions for creating map links.
 * 
 * @file   Defines a class for creating map links.
 * @author Julian Klose, 48514372+julianklose@users.noreply.github.com.
 */

const ENCODERTYPES = {
	geometry:"G",
	timelog:"T",
	grid:"D"
};
class UrlEncoder {
	constructor() {
		this.layers = [];
		this.mappings = [];
		this.urlmappings = {};
	}
	
	map(id) {
		let m = this.urlmappings[id];
		if(m === undefined) {
			m = this.mappings.length;
			this.urlmappings[id] = m;
			this.mappings.push(encodeURIComponent(id).replace('~', '%7E'));
		}
		return m;
	}
	
	addLayer(type, file, id, ddi, color, active) {
		let l = active ? "" : "H";
		l += ENCODERTYPES[type] + "-";
		l += this.map(file) + "-";
		l += this.map(id) + "-";
		if(ddi) l += this.map(ddi);
		l += "-";
		if(color) l += color.slice(1);
		this.layers.push(l);
	}
	
	getMappings() {
		return this.mappings.join("~");
	}
	
	getLayers() {
		return this.layers.join("~");
	}
	
	getLink() {
		return "/map.html?mappings=" + this.getMappings() + "&layers=" + this.getLayers();
	}
}