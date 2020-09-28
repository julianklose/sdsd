package de.sdsd.projekt.prototype.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;

import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;

/**
 * Represents a grid cell from cassandra.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class GridCell {

	public final Key key;
	public final Coordinate size, pos;
	public final long value;

	public GridCell(Key key, Coordinate size, Coordinate pos, long value) {
		this.key = key;
		this.size = size;
		this.pos = pos;
		this.value = value;
	}
	
	public Key getKey() {
		return key;
	}

	public Coordinate getSize() {
		return size;
	}

	public Coordinate getPos() {
		return pos;
	}

	public long getValue() {
		return value;
	}
	
	public JSONObject toGeoJsonGeometry() {
		JSONArray ring = new JSONArray()
				.put(new JSONArray().put(pos.x).put(pos.y))
				.put(new JSONArray().put(pos.x).put(pos.y + size.y))
				.put(new JSONArray().put(pos.x + size.x).put(pos.y + size.y))
				.put(new JSONArray().put(pos.x + size.x).put(pos.y))
				.put(new JSONArray().put(pos.x).put(pos.y));
		
		return new JSONObject()
				.put("type", "Polygon")
				.put("coordinates", new JSONArray()
						.put(ring));
	}
}
