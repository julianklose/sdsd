package de.sdsd.projekt.api;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.websocket.DeploymentException;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceResult.Attr;
import de.sdsd.projekt.api.ServiceResult.Device;
import de.sdsd.projekt.api.ServiceResult.DeviceElementProperties;
import de.sdsd.projekt.api.ServiceResult.FindResult;
import de.sdsd.projekt.api.ServiceResult.Positions;
import de.sdsd.projekt.api.ServiceResult.SDSDFile;
import de.sdsd.projekt.api.ServiceResult.SDSDFileAppended;
import de.sdsd.projekt.api.ServiceResult.SDSDFileMeta;
import de.sdsd.projekt.api.ServiceResult.SDSDObject;
import de.sdsd.projekt.api.ServiceResult.TimeInterval;
import de.sdsd.projekt.api.ServiceResult.TimeLog;
import de.sdsd.projekt.api.ServiceResult.TimeLogKey;
import de.sdsd.projekt.api.ServiceResult.TimeLogKeyDdi;
import de.sdsd.projekt.api.ServiceResult.TimeLogKeys;
import de.sdsd.projekt.api.ServiceResult.Total;
import de.sdsd.projekt.api.ServiceResult.ValueInfo;
import de.sdsd.projekt.api.ServiceResult.WikiInstance;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;
import de.sdsd.projekt.api.WebsocketClient.SDSDListener;

/**
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ServiceAPI implements AutoCloseable {

	public static final Pattern REGEX_ILLEGAL_CHARS = Pattern.compile("[<>:;,?\"*|/\\\\]+");
	public static final String WIKI_URI = "https://app.sdsd-projekt.de/wikinormia.html?page=";
	public static WikiFormat ISOXML = Util.format("isoxml");
	public static WikiType R_DLV = ISOXML.res("DLV");
	public static WikiAttr P_DDI = R_DLV.prop("A");
	public static Property PARTOF = DCTerms.isPartOf;

	private static final String ENDPOINT = "api", EVENT_INSTANCE_CHANGED = "instanceChanged",
			EVENT_INSTANCE_CANCELED = "instanceCanceled";

	private WebsocketClient client;

	public ServiceAPI(boolean local) throws IOException {
		try {
			this.client = new WebsocketClient(local);
		} catch (DeploymentException e) {
			throw new IOException(e);
		}
	}

	public void setInstanceChangedListener(String serviceToken, Consumer<ApiInstance> callback)
			throws JsonRpcException {
		client.setEventListener(new SDSDListener(ENDPOINT, EVENT_INSTANCE_CHANGED, null, serviceToken) {
			@Override
			public void accept(JSONArray t) {
				callback.accept(new ApiInstance(t.getJSONObject(0)));
			}
		});
	}

	public void setInstanceCanceledListener(String serviceToken, Consumer<ApiInstance> callback)
			throws JsonRpcException {
		client.setEventListener(new SDSDListener(ENDPOINT, EVENT_INSTANCE_CANCELED, null, serviceToken) {
			@Override
			public void accept(JSONArray t) {
				callback.accept(new ApiInstance(t.getJSONObject(0)));
			}
		});
	}
	
	public void clearListeners() {
		client.clearEventListeners(null);
	}

	public List<ApiInstance> listActiveInstances(String serviceToken) throws JsonRpcException {
		JSONArray res = execute("listActiveInstances", null, serviceToken).getJSONArray("activeInstances");
		List<ApiInstance> instances = new ArrayList<>(res.length());
		for (int i = 0; i < res.length(); ++i) {
			instances.add(new ApiInstance(res.getJSONObject(i)));
		}
		return instances;
	}

	public Map<String, WikiInstance> getWikinormiaInstances(Resource type) throws JsonRpcException {
		JSONObject result = execute("getWikinormiaInstances", null, type.getURI());
		return result(result, WikiInstance::new);
	}

	public class ApiInstance {

		public final String token;
		public final Instant activated;
		public final JSONObject parameter;

		public ApiInstance(JSONObject res) {
			this.token = res.getString("token");
			this.activated = Instant.parse(res.getString("activated"));
			this.parameter = res.optJSONObject("parameter");
		}

		public boolean isParamsSet() {
			return parameter != null;
		}
		
		public void clearListeners() {
			client.clearEventListeners(token);
		}

		// =================
		// methods

		public String getLabel(@Nullable String fileUri, String uri) throws JsonRpcException {
			return execute("getLabel", token, fileUri, uri).optString("label");
		}

		public SDSDObject getObject(@Nullable String fileUri, String uri) throws JsonRpcException {
			return new SDSDObject(execute("getObject", token, fileUri, uri));
		}

		public String getSourceFileUri(String uri) throws JsonRpcException {
			return execute("getSourceFileUri", token, uri).getString("fileUri");
		}

		public List<FindResult> getParts(@Nullable String fileUri, String uri, @Nullable Resource type)
				throws JsonRpcException {
			return result(
					execute("getParts", token, fileUri, uri, type != null ? type.getURI() : null).getJSONArray("parts"),
					FindResult::new);
		}

		public List<Attr> traverse(@Nullable String fileUri, String uri, Property... attributes)
				throws JsonRpcException {
			JSONArray properties = new JSONArray();
			for (Property p : attributes) {
				properties.put(p.getURI());
			}
			return result(execute("traverse", token, fileUri, uri, properties).getJSONArray("values"), Attr::new);
		}

		public List<Attr> getValue(@Nullable String fileUri, String uri, Property attribute) throws JsonRpcException {
			return result(execute("getValue", token, fileUri, uri, attribute.getURI()).getJSONArray("values"),
					Attr::new);
		}

		public FindBuilder find() {
			return new FindBuilder();
		}

		public class FindBuilder {
			private String file = null, type = null;
			private JSONObject attr = new JSONObject();
			private JSONArray uris = null;

			public FindBuilder byURI(String... uris) {
				JSONArray arr = new JSONArray();
				for (String uri : uris) {
					if (uri != null && uri.length() > 0)
						arr.put(uri);
				}
				this.uris = arr.length() > 0 ? arr : null;
				return this;
			}

			public FindBuilder byFile(String fileUri) {
				this.file = fileUri;
				return this;
			}

			public FindBuilder byType(Resource type) {
				this.type = type.getURI();
				return this;
			}

			public FindBuilder byAttribute(Property attribute, Object... values) throws JSONException {
				for (int i = 0; i < values.length; ++i) {
					if (values[i] instanceof Resource)
						values[i] = "<" + ((Resource) values[i]).getURI() + ">";
				}
				if (values.length == 0)
					attr.put(attribute.getURI(), JSONObject.NULL);
				else if (values.length == 1)
					attr.put(attribute.getURI(), values[0] != null ? values[0] : JSONObject.NULL);
				else if (values.length > 1) {
					JSONArray arr = new JSONArray();
					for (Object val : values) {
						if (val != null)
							arr.put(val);
					}
					attr.put(attribute.getURI(), arr);
				}
				return this;
			}

			public List<FindResult> exec() throws JsonRpcException {
				return result(execute("find", token, file, type, attr, uris).getJSONArray("instances"),
						FindResult::new);
			}
		}

		public TimeLogKeys<TimeLogKey> listTimeLogs(String fileUri) throws JsonRpcException {
			return new TimeLogKeys<>(execute("listTimeLogs", token, fileUri), TimeLogKey::new);
		}
		
		public Map<String, Device> listDevices() throws JsonRpcException {
			return result(execute("listDevices", token), Device::new);
		}
		
		public TimeLogKeys<TimeLogKeyDdi> findTimeLogs(String clientName) throws JsonRpcException {
			return new TimeLogKeys<>(execute("findTimeLogs", token, clientName), TimeLogKeyDdi::new);
		}
		
		public TimeLogKeys<Total> findTotalsByDevice(String clientName, @Nullable TimeInterval timeFilter, int... ddis) throws JsonRpcException {
			return new TimeLogKeys<>(execute("findTotalsByDevice", token, clientName, 
					timeFilter != null ? timeFilter.toJson() : null, new JSONArray(ddis)), Total::new);
		}
		
		public TimeLogKeys<TimeLogKeyDdi> findValueUrisByDevice(String clientName, @Nullable TimeInterval timeFilter, int... ddis) throws JsonRpcException {
			return new TimeLogKeys<>(execute("findValueUrisByDevice", token, clientName, 
					timeFilter != null ? timeFilter.toJson() : null, new JSONArray(ddis)), TimeLogKeyDdi::new);
		}

		public Positions getPositions(String fileUri, String timelog, @Nullable TimeInterval timeFilter, int limit)
				throws JsonRpcException {
			return new Positions(execute("getPositions", token, fileUri, timelog, timeFilter != null ? timeFilter.toJson() : null, limit));
		}

		public TimeLog getTimeLog(TimeLogKey key, @Nullable TimeInterval timeFilter, int limit) throws JsonRpcException {
			return new TimeLog(execute("getTimeLog", token, key.getFileUri(), key.getName(), key.getValueUri(), timeFilter != null ? timeFilter.toJson() : null, limit));
		}

		public TimeLog getTimeLog(String fileUri, String name, String valueUri, @Nullable TimeInterval timeFilter,
				int limit) throws JsonRpcException {
			return new TimeLog(execute("getTimeLog", token, fileUri, name, valueUri, timeFilter != null ? timeFilter.toJson() : null, limit));
		}
		
		public ValueInfo getValueInfo(String fileUri, String valueUri) throws JsonRpcException {
			return new ValueInfo(execute("getValueInfo", token, fileUri, valueUri));
		}

		public SDSDFile getFile(String fileUri) throws JsonRpcException {
			return new SDSDFile(execute("getFile", token, fileUri));
		}

		public List<SDSDFileMeta> listFiles(String sdsdtype) throws JsonRpcException {
			return result(execute("listFiles", token, sdsdtype).getJSONArray("files"), SDSDFileMeta::new);
		}

		private static final String EVENT_NEW_DATA = "newData", EVENT_NEW_FILE = "newFile",
				EVENT_FILE_APPENDED = "fileAppended", EVENT_FILE_DELETED = "fileDeleted";

		public void setNewDataListener(Consumer<SDSDFileMeta> callback) throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_NEW_DATA, token, "") {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		public void unsetNewDataListener() throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_NEW_DATA, token, "");
		}

		public void setNewFileListener(Consumer<SDSDFileMeta> callback) throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_NEW_FILE, token, "") {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		public void unsetNewFileListener() throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_NEW_FILE, token, "");
		}

		public void setFileAppendedListener(Consumer<SDSDFileAppended> callback, String fileUri)
				throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_FILE_APPENDED, token, fileUri) {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileAppended(t.getJSONObject(0)));
				}
			});
		}

		public void unsetFileAppendedListener(String fileUri) throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_FILE_APPENDED, token, fileUri);
		}
		
		public void setFileDeletedListener(Consumer<SDSDFileMeta> callback, String fileUri)
				throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_FILE_DELETED, token, fileUri) {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		public void unsetFileDeletedListener(String fileUri) throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_FILE_DELETED, token, fileUri);
		}

		public boolean sendFile(String filename, byte[] content) throws JsonRpcException {
			JSONObject result = execute("sendFile", token, REGEX_ILLEGAL_CHARS.matcher(filename).replaceAll(""),
					Base64.getEncoder().encodeToString(content));
			return result.getBoolean("success");
		}

		public boolean appendFile(String filename, byte[] newContent) throws JsonRpcException {
			JSONObject result = execute("appendFile", token, REGEX_ILLEGAL_CHARS.matcher(filename).replaceAll(""),
					Base64.getEncoder().encodeToString(newContent));
			return result.getBoolean("success");
		}

		public List<JSONObject> findGeometry(@Nullable String fileUri, @Nullable String uri,
				@Nullable ElementType elementtype, @Nullable GeoType geotype) throws JsonRpcException {
			JSONObject featureCollection = execute("findGeometry", token, fileUri, uri,
					elementtype != null ? elementtype.name() : null, geotype != null ? geotype.name() : null);
			return result(featureCollection.getJSONArray("features"), Function.identity());
		}

		public DeviceElementProperties getDeviceElementProperties(@Nullable String fileUri, String detUri)
				throws JsonRpcException {
			return new DeviceElementProperties(execute("getDeviceElementProperties", token, fileUri, detUri));
		}

		public List<JSONObject> geoIntersects(JSONObject geometry, @Nullable ElementType elementtype,
				@Nullable GeoType geotype) throws JsonRpcException {
			JSONObject featureCollection = execute("geoIntersects", token, geometry,
					elementtype != null ? elementtype.name() : null, geotype != null ? geotype.name() : null);
			return result(featureCollection.getJSONArray("features"), Function.identity());
		}

		public boolean setError(@Nullable String error) throws JsonRpcException {
			return execute("setError", token, error).getBoolean("success");
		}

		public boolean complete(String url) throws JsonRpcException {
			return execute("complete", token, url).getBoolean("success");
		}

		public ValueUriHelper findValueUris(String fileUri, int... ddis) throws JsonRpcException {
			ValueUriHelper valueUriHelper = new ValueUriHelper(ddis);
			valueUriHelper.init(fileUri);
			return valueUriHelper;
		}

		public class ValueUriHelper {
			private final Map<Integer, List<String>> ddis;
			private final Map<String, Set<String>> keys = new HashMap<>();

			public ValueUriHelper(int... ddis) {
				this.ddis = new HashMap<>(ddis.length);
				for (int ddi : ddis) {
					this.ddis.put(ddi, new ArrayList<>());
				}
			}

			public void init(String fileUri) throws JsonRpcException {
				for (FindResult res : find()
						.byFile(fileUri)
						.byType(R_DLV)
						.byAttribute(P_DDI, ddis.keySet().stream()
								.map(Util.DDI::inst)
								.toArray()).exec()) {
					Optional<String> ddi = res.getAttribute(P_DDI);
					if (ddi.isPresent()) {
						List<String> list = ddis.get(Util.ddi(ddi.get()));
						if (list != null)
							list.add(res.uri);
					}
				}

				listTimeLogs(fileUri).streamAll().forEach(key -> {
					Set<String> values = keys.get(key.getName());
					if (values == null)
						keys.put(key.getName(), values = new HashSet<>());
					values.add(key.valueUri);
				});
			}

			public Optional<String> getValueUri(String tlName, int ddi) {
				List<String> valueUris = ddis.get(ddi);
				if (valueUris == null)
					throw new IllegalArgumentException("Unknown DDI " + ddi);
				Set<String> tlValues = keys.get(tlName);
				if (tlValues == null)
					return Optional.empty();
				for (String vu : valueUris) {
					if (tlValues.contains(vu))
						return Optional.of(vu);
				}
				return Optional.empty();
			}

		}
	}

	public static enum ElementType {
		Other, TimeLog, Field, TreatmentZone, GuidancePattern, FieldAccess
	}

	public static enum GeoType {
		Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection;
	}

	public static JSONObject geoJsonBBox(double minLat, double minLong, double maxLat, double maxLong) {
		JSONArray ring = new JSONArray().put(new JSONArray().put(minLong).put(minLat))
				.put(new JSONArray().put(minLong).put(maxLat)).put(new JSONArray().put(maxLong).put(maxLat))
				.put(new JSONArray().put(maxLong).put(minLat)).put(new JSONArray().put(minLong).put(minLat));
		return new JSONObject().put("type", GeoType.Polygon.name()).put("coordinates", new JSONArray().put(ring));
	}

	// =========
	// helper

	static <T> List<T> result(JSONArray res, Function<JSONObject, T> creator) throws JSONException {
		List<T> list = new ArrayList<>(res.length());
		for (int i = 0; i < res.length(); ++i) {
			list.add(creator.apply(res.getJSONObject(i)));
		}
		return list;
	}

	static <T> Map<String, T> result(JSONObject res, Function<JSONObject, T> creator) throws JSONException {
		Map<String, T> map = new HashMap<>(res.length());
		for (String key : res.keySet()) {
			map.put(key, creator.apply(res.getJSONObject(key)));
		}
		return map;
	}

	public static class JsonRpcException extends Exception {
		private static final long serialVersionUID = -518914703894017313L;
		private int code;

		public JsonRpcException(JSONObject error) {
			super(error.optString("msg", error.optString("message", "JsonRpcException")));
			code = error.optInt("code", 0);
		}

		public int getCode() {
			return code;
		}
	}

	private JSONObject execute(String method, String token, Object... params) throws JsonRpcException {
		return client.execute(ENDPOINT, method, token, params);
	}

	@Override
	public void close() throws Exception {
		client.close();
	}

}
