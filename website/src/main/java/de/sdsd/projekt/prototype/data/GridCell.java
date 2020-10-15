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

	/** The key. */
	public final Key key;
	
	/** The pos. */
	public final Coordinate size, pos;
	
	/** The value. */
	public final long value;

	/**
	 * Instantiates a new grid cell.
	 *
	 * @param key the key
	 * @param size the size
	 * @param pos the pos
	 * @param value the value
	 */
	public GridCell(Key key, Coordinate size, Coordinate pos, long value) {
		this.key = key;
		this.size = size;
		this.pos = pos;
		this.value = value;
	}
	
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public Key getKey() {
		return key;
	}

	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public Coordinate getSize() {
		return size;
	}

	/**
	 * Gets the pos.
	 *
	 * @return the pos
	 */
	public Coordinate getPos() {
		return pos;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public long getValue() {
		return value;
	}
	
	/**
	 * To geo json geometry.
	 *
	 * @return the JSON object
	 */
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
