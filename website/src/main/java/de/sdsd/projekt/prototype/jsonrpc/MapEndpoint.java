package de.sdsd.projekt.prototype.jsonrpc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TableFunctions;
import de.sdsd.projekt.prototype.applogic.TableFunctions.ElementKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.FileKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;
import de.sdsd.projekt.prototype.applogic.TableFunctions.TimeInterval;
import de.sdsd.projekt.prototype.applogic.TableFunctions.TimelogInfo;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.MinMaxValues;
import de.sdsd.projekt.prototype.data.GridCell;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.Timelog;
import de.sdsd.projekt.prototype.data.TimelogPosition;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.data.ValueInfo;

/**
 * JSONRPC-Endpoint for map functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class MapEndpoint extends JsonRpcEndpoint {
	
	/**
	 * Instantiates a new map endpoint.
	 *
	 * @param application the application
	 */
	public MapEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Lists the users files.
	 *
	 * @param req http servlet request including userdata
	 * @return the JSON object including the files "filename" and "id"
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listMapFiles(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listMapFiles: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				//TODO: filter map files
				JSONArray array = application.list.files.getList(user).stream()
						.sorted(File.CMP_RECENT)
						.map(file -> new JSONObject()
								.put("id", file.getId().toHexString())
								.put("filename", file.getFilename()))
						.collect(Util.toJSONArray());
				return new JSONObject().put("files", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/** The Constant KEY_NAME_COMP. */
	private static final Comparator<ElementKey> KEY_NAME_COMP = new Comparator<ElementKey>() {
		@Override
		public int compare(ElementKey o1, ElementKey o2) {
			return o1.name.compareTo(o2.name);
		}
	};
	
	/**
	 * Lists the chosen files content.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the chosen files id
	 * @return the JSON object includes the files containing grids (name), timelogs (name, count, max, from and until) and geometries (id, label and type) 
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listMapContent(HttpServletRequest req, String fileid) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listMapContent: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				FileKey fkey = new FileKey(user.getName(), File.toURI(fileid));

				JSONArray grids = application.table.listGridKeys(fkey).stream()
						.map(Key::getName)
						.distinct()
						.sorted()
						.map(n -> new JSONObject().put("name", n))
						.collect(Util.toJSONArray());
				
				JSONArray timelogs = application.table.listPositionKeys(fkey).stream()
						.sorted(KEY_NAME_COMP)
						.map(key -> {
							TimelogInfo info = application.table.getTimelogInfo(key);
							return new JSONObject()
									.put("name", key.name)
									.put("count", info.count)
									.put("max", TableFunctions.OUTPUT_MAX)
									.put("from", isoUTC(info.from))
									.put("until", isoUTC(info.until));
						}).collect(Util.toJSONArray());
				
				JSONArray geometries = application.geo.find(user, GeoElement.filterFile(fid)).stream()
						.map(geo -> new JSONObject()
								.put("id", geo.getId().toHexString())
								.put("label", geo.getFullLabel())
								.put("type", geo.getType().name()))
						.collect(Util.toJSONArray());

				return new JSONObject()
						.put("grids", grids)
						.put("timelogs", timelogs)
						.put("geometries", geometries);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the grids DDis.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the parent files id
	 * @param gridName the grids name
	 * @return a List of the the grids DDis
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getGridDDIs(HttpServletRequest req, String fileid, String gridName) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getGridDDIs: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") grid(" + gridName + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				ElementKey ekey = new ElementKey(user.getName(), File.toURI(fileid), gridName);
				
				List<String> valueUris = application.table.listGridValueUris(ekey);
				List<ValueInfo> infos = application.table.getValueInfos(ekey.file, valueUris);
				
				return createDdiMap(infos, findGroups(infos, ekey.file));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the timelog DDis.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the parent files id
	 * @param timelogName the timelogs name
	 * @return a List of the the timelogs DDis
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getTimelogDDIs(HttpServletRequest req, String fileid, String timelogName) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getTimelogDDIs: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") timelog(" + timelogName + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				ElementKey ekey = new ElementKey(user.getName(), File.toURI(fileid), timelogName);
				
				List<String> valueUris = application.table.listTimelogValueUris(ekey);
				List<ValueInfo> infos = application.table.getValueInfos(ekey.file, valueUris);
				
				return createDdiMap(infos, findGroups(infos, ekey.file));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the geometries.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the parent files id
	 * @param geouri geometries uri
	 * @return the geometry
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getGeometry(HttpServletRequest req, String fileid, String geouri) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getGeometry: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") geo(" + geouri + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				Bson filter = null;
				try {
					if(geouri.length() == 24)
						filter = GeoElement.filter(user, new ObjectId(geouri));
				} catch(Exception e) {}
				if(filter == null)
					filter = GeoElement.filterUri(geouri);
				
				List<GeoElement> geos = application.geo.find(user, filter);
				if(geos.isEmpty()) throw new SDSDException("Geometry not found!");
				
				JSONObject geojson = null;
				GeoElement geo = geos.get(0);
				geojson = geo.getFeatureJson();
				JSONObject props = geojson.optJSONObject("properties");
				if(props == null) 
					geojson.put("properties", props = new JSONObject());

				props.put("label", geo.getFullLabel());
				
				JSONObject out = new JSONObject()
						.put("geojson", geojson);
				
				MinMaxValues values = geo.getValues();
				if(values != null)
					out.put("relativeValues", values.toJson());
				
				return out;
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the grid.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the parent files id
	 * @param gridName the grids name
	 * @param valueUri the values uri
	 * @return the grid
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getGrid(HttpServletRequest req, String fileid, String gridName, String valueUri) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getGrid: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") gridName(" + gridName + ") valueUri(" + valueUri + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				String fileUri = File.toURI(fileid);
				Key key = new Key(user.getName(), fileUri, gridName, valueUri);
				
				ValueInfo info = application.table.getValueInfo(key.file, key.valueUri);
				JSONArray features = new JSONArray();
				List<Double> values = new ArrayList<>();
				for(GridCell cell : application.table.getGrid(key)) {
					double value = info.translateValue(cell.value);
					values.add(value);
					features.put(new JSONObject()
							.put("type", "Feature")
							.put("geometry", cell.toGeoJsonGeometry())
							.put("properties", new JSONObject()
									.put(valueUri, new JSONObject()
											.put("value", value)
											.put("label", info.formatValue(value) + ' ' + info.unit))));
				}

				return calcMinMax(values)
						.put("geojson", new JSONObject()
								.put("type", "FeatureCollection")
								.put("features", features));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the timelog.
	 *
	 * @param req http servlet request including userdata
	 * @param fileid the parent files id
	 * @param timelogName the timelogs name
	 * @param valueUri the values uri
	 * @param timeFilter filter that limits the timelogs by the given interval
	 * @param limit the limit
	 * @return the timelog
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getTimelog(HttpServletRequest req, String fileid, String timelogName, String valueUri, 
			JSONObject timeFilter, int limit) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getTimelog: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") timelogName(" + timelogName + ") valueUri(" + valueUri
					+ ") timeFilter(" + timeFilter + ") limit(" + limit + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else if(limit > 10000)
				throw new SDSDException("Limit is too big");
			else {
				String fileUri = File.toURI(fileid);
				TimeInterval timeinterval = TimeInterval.from(timeFilter);
				if(valueUri != null && valueUri.isEmpty()) valueUri = null;
				
				List<TimelogPosition> positions;
				ValueInfo info;
				List<Timelog> timelogs;
				if(valueUri != null) {
					Key key = new Key(user.getName(), fileUri, timelogName, valueUri);
					positions = application.table.getPositions(key, timeinterval, limit);
					info = application.table.getValueInfo(key.file, key.valueUri);
					timelogs = application.table.getTimelogs(key, timeinterval, limit);
				} else {
					ElementKey key = new ElementKey(user.getName(), fileUri, timelogName);
					positions = application.table.getPositions(key, timeinterval, limit);
					info = null;
					timelogs = null;
				}
				
				JSONArray features = new JSONArray();
				List<Double> valueList = new ArrayList<>();
				JSONArray lscoords = null;
				Instant last = Instant.MIN, lastPoint = Instant.MIN;
				
				JSONObject prop = null;
				int j = timelogs != null ? timelogs.size()-1 : -1;
				for(int i = positions.size()-1; i >= 0; --i) {
					TimelogPosition pos = positions.get(i);
					if(!last.isAfter(pos.time.minusSeconds(60))) { // start a new linestring if there is a pause of over 60 seconds
						lscoords = new JSONArray();
						features.put(new JSONObject()
								.put("type", "Feature")
								.put("geometry", new JSONObject()
										.put("type", "LineString")
										.put("coordinates", lscoords)));
					}
					if(pos.isValid()) {
						JSONArray coords = pos.toGeoJson();
						lscoords.put(coords);
						
						if(j >= 0) {
							Timelog tl = timelogs.get(j);
							if(!pos.time.isBefore(tl.time)) {
								double value = info.translateValue(tl.value);
								valueList.add(value);
								prop = new JSONObject()
										.put("value", value)
										.put("label", info.formatValue(value) + ' ' + info.unit);
								--j;
							}
						}
						
						if(timelogs != null || !lastPoint.isAfter(pos.time.minusSeconds(60))) { // create less points
							JSONObject props = new JSONObject().put("time", isoUTC(pos.time));
							if(valueUri != null) props.put(valueUri, prop);
							features.put(new JSONObject()
									.put("type", "Feature")
									.put("geometry", new JSONObject()
											.put("type", "Point")
											.put("coordinates", coords))
									.put("properties", props));
							lastPoint = pos.time;
						}
					}
					last = pos.time;
				}
				
				return calcMinMax(valueList)
						.put("geojson", new JSONObject()
								.put("type", "FeatureCollection")
								.put("features", features));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the label or ddi.
	 *
	 * @param info the info
	 * @return the label or ddi
	 */
	private String getLabelOrDdi(ValueInfo info) {
		String label = info.designator;
		if(label.length() <= 4) {
			try {
				int ddinum = Integer.parseUnsignedInt(label, 16);
				label = application.triple.getDDI(ddinum).getName();
			} catch (NumberFormatException e) {}
		}
		return label;
	}
	
	/**
	 * Find groups.
	 *
	 * @param infos the infos
	 * @param fileUri the file uri
	 * @return the map
	 */
	private Map<String, JSONArray> findGroups(Collection<ValueInfo> infos, String fileUri) {
		Map<String, JSONArray> groupMap = new HashMap<>(infos.size());
		Var VURI = Var.alloc("valueUri"), DVC = Var.alloc("dvc"), DET = Var.alloc("det"), DVCL = Var.alloc("dvcLabel"), DETL = Var.alloc("detLabel");
		SelectBuilder sb = new SelectBuilder()
				.addVar(VURI).addVar(DVCL).addVar(DETL)
				.from(fileUri)
				.addWhere(VURI, T_ISOXML.res("DLV").prop("C"), DET)
				.addWhere(DET, DCTerms.isPartOf, DVC)
				.addWhere(DET, T_ISOXML.res("DET").prop("D"), DETL)
				.addWhere(DVC, T_ISOXML.res("DVC").prop("B"), DVCL)
				.addValueVar(VURI, infos.stream().map(ValueInfo::valueUri).map(NodeFactory::createURI).toArray());
		try(QueryResult qr = application.triple.query(sb.build())) {
			for(UtilQuerySolution qs : qr.iterate()) {
				groupMap.put(qs.getUri(VURI), new JSONArray()
						.put(qs.getString(DVCL))
						.put(qs.getString(DETL)));
			}
		}
		return groupMap;
	}
	
	/**
	 * Creates the ddi map.
	 *
	 * @param infos the infos
	 * @param groups the groups
	 * @return the JSON object
	 */
	private JSONObject createDdiMap(Collection<ValueInfo> infos, Map<String, JSONArray> groups) {
		JSONObject ddimap = new JSONObject();
		for(ValueInfo ddi : infos) {
			JSONObject obj = new JSONObject()
					.put("label", getLabelOrDdi(ddi))
					.put("groups", groups.get(ddi.valueUri));
			ddimap.put(ddi.valueUri, obj);
		}
		return ddimap;
	}
	
	/**
	 * Calc min max.
	 *
	 * @param valueList the value list
	 * @return the JSON object
	 */
	private static JSONObject calcMinMax(List<Double> valueList) {
		JSONObject obj = new JSONObject();
		if(valueList.isEmpty()) return obj;
		
		double[] values = valueList.stream()
				.mapToDouble(Double::doubleValue)
				.sorted().distinct()
				.toArray();

		if(values.length < 8)
			obj.put("min", values[0]).put("max", values[values.length-1]);
		else {
			double q1 = values[values.length / 4];
			double q3 = values[(int) Math.ceil(values.length * 3. / 4.) - 1];
			double iqr = q3 - q1;
			double maxValue = q3 + iqr * 1.5;
			double minValue = q1 - iqr * 1.5;
			
			double min = maxValue, max = minValue;
			for(double v : values) {
				if(v >= minValue && v <= maxValue) {
					if(v < min) min = v;
					if(v > max) max = v;
				}
			}
			obj.put("min", min).put("max", max);
		}
		return obj;
	}

}
