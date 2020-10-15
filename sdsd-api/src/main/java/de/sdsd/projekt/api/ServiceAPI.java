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
 * The Class ServiceAPI.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public class ServiceAPI implements AutoCloseable {

	/** The Constant REGEX_ILLEGAL_CHARS. */
	public static final Pattern REGEX_ILLEGAL_CHARS = Pattern.compile("[<>:;,?\"*|/\\\\]+");

	/** The Constant WIKI_URI. */
	public static final String WIKI_URI = "https://app.sdsd-projekt.de/wikinormia.html?page=";

	/** The isoxml. */
	public static WikiFormat ISOXML = Util.format("isoxml");

	/** The r dlv. */
	public static WikiType R_DLV = ISOXML.res("DLV");

	/** The p ddi. */
	public static WikiAttr P_DDI = R_DLV.prop("A");

	/** The partof. */
	public static Property PARTOF = DCTerms.isPartOf;

	/** The Constant EVENT_INSTANCE_CANCELED. */
	private static final String ENDPOINT = "api", EVENT_INSTANCE_CHANGED = "instanceChanged",
			EVENT_INSTANCE_CANCELED = "instanceCanceled";

	/** The client. */
	private WebsocketClient client;

	/**
	 * Instantiates a new service API.
	 *
	 * @param local the local
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ServiceAPI(boolean local) throws IOException {
		try {
			this.client = new WebsocketClient(local);
		} catch (DeploymentException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Sets the instance changed listener.
	 *
	 * @param serviceToken the service token
	 * @param callback     the callback
	 * @throws JsonRpcException the json rpc exception
	 */
	public void setInstanceChangedListener(String serviceToken, Consumer<ApiInstance> callback)
			throws JsonRpcException {
		client.setEventListener(new SDSDListener(ENDPOINT, EVENT_INSTANCE_CHANGED, null, serviceToken) {
			@Override
			public void accept(JSONArray t) {
				callback.accept(new ApiInstance(t.getJSONObject(0)));
			}
		});
	}

	/**
	 * Sets the instance canceled listener.
	 *
	 * @param serviceToken the service token
	 * @param callback     the callback
	 * @throws JsonRpcException the json rpc exception
	 */
	public void setInstanceCanceledListener(String serviceToken, Consumer<ApiInstance> callback)
			throws JsonRpcException {
		client.setEventListener(new SDSDListener(ENDPOINT, EVENT_INSTANCE_CANCELED, null, serviceToken) {
			@Override
			public void accept(JSONArray t) {
				callback.accept(new ApiInstance(t.getJSONObject(0)));
			}
		});
	}

	/**
	 * Clear listeners.
	 */
	public void clearListeners() {
		client.clearEventListeners(null);
	}

	/**
	 * List active instances.
	 *
	 * @param serviceToken the service token
	 * @return the list
	 * @throws JsonRpcException the json rpc exception
	 */
	public List<ApiInstance> listActiveInstances(String serviceToken) throws JsonRpcException {
		JSONArray res = execute("listActiveInstances", null, serviceToken).getJSONArray("activeInstances");
		List<ApiInstance> instances = new ArrayList<>(res.length());
		for (int i = 0; i < res.length(); ++i) {
			instances.add(new ApiInstance(res.getJSONObject(i)));
		}
		return instances;
	}

	/**
	 * Gets the wikinormia instances.
	 *
	 * @param type the type
	 * @return the wikinormia instances
	 * @throws JsonRpcException the json rpc exception
	 */
	public Map<String, WikiInstance> getWikinormiaInstances(Resource type) throws JsonRpcException {
		JSONObject result = execute("getWikinormiaInstances", null, type.getURI());
		return result(result, WikiInstance::new);
	}

	/**
	 * The Class ApiInstance.
	 */
	public class ApiInstance {

		/** The token. */
		public final String token;

		/** The activated. */
		public final Instant activated;

		/** The parameter. */
		public final JSONObject parameter;

		/**
		 * Instantiates a new api instance.
		 *
		 * @param res the res
		 */
		public ApiInstance(JSONObject res) {
			this.token = res.getString("token");
			this.activated = Instant.parse(res.getString("activated"));
			this.parameter = res.optJSONObject("parameter");
		}

		/**
		 * Checks if is params set.
		 *
		 * @return true, if is params set
		 */
		public boolean isParamsSet() {
			return parameter != null;
		}

		/**
		 * Clear listeners.
		 */
		public void clearListeners() {
			client.clearEventListeners(token);
		}

		// =================
		// methods

		/**
		 * Gets the label.
		 *
		 * @param fileUri the file uri
		 * @param uri     the uri
		 * @return the label
		 * @throws JsonRpcException the json rpc exception
		 */
		public String getLabel(@Nullable String fileUri, String uri) throws JsonRpcException {
			return execute("getLabel", token, fileUri, uri).optString("label");
		}

		/**
		 * Gets the object.
		 *
		 * @param fileUri the file uri
		 * @param uri     the uri
		 * @return the object
		 * @throws JsonRpcException the json rpc exception
		 */
		public SDSDObject getObject(@Nullable String fileUri, String uri) throws JsonRpcException {
			return new SDSDObject(execute("getObject", token, fileUri, uri));
		}

		/**
		 * Gets the source file uri.
		 *
		 * @param uri the uri
		 * @return the source file uri
		 * @throws JsonRpcException the json rpc exception
		 */
		public String getSourceFileUri(String uri) throws JsonRpcException {
			return execute("getSourceFileUri", token, uri).getString("fileUri");
		}

		/**
		 * Gets the parts.
		 *
		 * @param fileUri the file uri
		 * @param uri     the uri
		 * @param type    the type
		 * @return the parts
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<FindResult> getParts(@Nullable String fileUri, String uri, @Nullable Resource type)
				throws JsonRpcException {
			return result(
					execute("getParts", token, fileUri, uri, type != null ? type.getURI() : null).getJSONArray("parts"),
					FindResult::new);
		}

		/**
		 * Traverse.
		 *
		 * @param fileUri    the file uri
		 * @param uri        the uri
		 * @param attributes the attributes
		 * @return the list
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<Attr> traverse(@Nullable String fileUri, String uri, Property... attributes)
				throws JsonRpcException {
			JSONArray properties = new JSONArray();
			for (Property p : attributes) {
				properties.put(p.getURI());
			}
			return result(execute("traverse", token, fileUri, uri, properties).getJSONArray("values"), Attr::new);
		}

		/**
		 * Gets the value.
		 *
		 * @param fileUri   the file uri
		 * @param uri       the uri
		 * @param attribute the attribute
		 * @return the value
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<Attr> getValue(@Nullable String fileUri, String uri, Property attribute) throws JsonRpcException {
			return result(execute("getValue", token, fileUri, uri, attribute.getURI()).getJSONArray("values"),
					Attr::new);
		}

		/**
		 * Find.
		 *
		 * @return the find builder
		 */
		public FindBuilder find() {
			return new FindBuilder();
		}

		/**
		 * The Class FindBuilder.
		 */
		public class FindBuilder {

			/** The type. */
			private String file = null, type = null;

			/** The attr. */
			private JSONObject attr = new JSONObject();

			/** The uris. */
			private JSONArray uris = null;

			/**
			 * By URI.
			 *
			 * @param uris the uris
			 * @return the find builder
			 */
			public FindBuilder byURI(String... uris) {
				JSONArray arr = new JSONArray();
				for (String uri : uris) {
					if (uri != null && uri.length() > 0)
						arr.put(uri);
				}
				this.uris = arr.length() > 0 ? arr : null;
				return this;
			}

			/**
			 * By file.
			 *
			 * @param fileUri the file uri
			 * @return the find builder
			 */
			public FindBuilder byFile(String fileUri) {
				this.file = fileUri;
				return this;
			}

			/**
			 * By type.
			 *
			 * @param type the type
			 * @return the find builder
			 */
			public FindBuilder byType(Resource type) {
				this.type = type.getURI();
				return this;
			}

			/**
			 * By attribute.
			 *
			 * @param attribute the attribute
			 * @param values    the values
			 * @return the find builder
			 * @throws JSONException the JSON exception
			 */
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

			/**
			 * Exec.
			 *
			 * @return the list
			 * @throws JsonRpcException the json rpc exception
			 */
			public List<FindResult> exec() throws JsonRpcException {
				return result(execute("find", token, file, type, attr, uris).getJSONArray("instances"),
						FindResult::new);
			}
		}

		/**
		 * List time logs.
		 *
		 * @param fileUri the file uri
		 * @return the time log keys
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLogKeys<TimeLogKey> listTimeLogs(String fileUri) throws JsonRpcException {
			return new TimeLogKeys<>(execute("listTimeLogs", token, fileUri), TimeLogKey::new);
		}

		/**
		 * List devices.
		 *
		 * @return the map
		 * @throws JsonRpcException the json rpc exception
		 */
		public Map<String, Device> listDevices() throws JsonRpcException {
			return result(execute("listDevices", token), Device::new);
		}

		/**
		 * Find time logs.
		 *
		 * @param clientName the client name
		 * @return the time log keys
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLogKeys<TimeLogKeyDdi> findTimeLogs(String clientName) throws JsonRpcException {
			return new TimeLogKeys<>(execute("findTimeLogs", token, clientName), TimeLogKeyDdi::new);
		}

		/**
		 * Find totals by device.
		 *
		 * @param clientName the client name
		 * @param timeFilter the time filter
		 * @param ddis       the ddis
		 * @return the time log keys
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLogKeys<Total> findTotalsByDevice(String clientName, @Nullable TimeInterval timeFilter, int... ddis)
				throws JsonRpcException {
			return new TimeLogKeys<>(execute("findTotalsByDevice", token, clientName,
					timeFilter != null ? timeFilter.toJson() : null, new JSONArray(ddis)), Total::new);
		}

		/**
		 * Find value uris by device.
		 *
		 * @param clientName the client name
		 * @param timeFilter the time filter
		 * @param ddis       the ddis
		 * @return the time log keys
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLogKeys<TimeLogKeyDdi> findValueUrisByDevice(String clientName, @Nullable TimeInterval timeFilter,
				int... ddis) throws JsonRpcException {
			return new TimeLogKeys<>(execute("findValueUrisByDevice", token, clientName,
					timeFilter != null ? timeFilter.toJson() : null, new JSONArray(ddis)), TimeLogKeyDdi::new);
		}

		/**
		 * Gets the positions.
		 *
		 * @param fileUri    the file uri
		 * @param timelog    the timelog
		 * @param timeFilter the time filter
		 * @param limit      the limit
		 * @return the positions
		 * @throws JsonRpcException the json rpc exception
		 */
		public Positions getPositions(String fileUri, String timelog, @Nullable TimeInterval timeFilter, int limit)
				throws JsonRpcException {
			return new Positions(execute("getPositions", token, fileUri, timelog,
					timeFilter != null ? timeFilter.toJson() : null, limit));
		}

		/**
		 * Gets the time log.
		 *
		 * @param key        the key
		 * @param timeFilter the time filter
		 * @param limit      the limit
		 * @return the time log
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLog getTimeLog(TimeLogKey key, @Nullable TimeInterval timeFilter, int limit)
				throws JsonRpcException {
			return new TimeLog(execute("getTimeLog", token, key.getFileUri(), key.getName(), key.getValueUri(),
					timeFilter != null ? timeFilter.toJson() : null, limit));
		}

		/**
		 * Gets the time log.
		 *
		 * @param fileUri    the file uri
		 * @param name       the name
		 * @param valueUri   the value uri
		 * @param timeFilter the time filter
		 * @param limit      the limit
		 * @return the time log
		 * @throws JsonRpcException the json rpc exception
		 */
		public TimeLog getTimeLog(String fileUri, String name, String valueUri, @Nullable TimeInterval timeFilter,
				int limit) throws JsonRpcException {
			return new TimeLog(execute("getTimeLog", token, fileUri, name, valueUri,
					timeFilter != null ? timeFilter.toJson() : null, limit));
		}

		/**
		 * Gets the value info.
		 *
		 * @param fileUri  the file uri
		 * @param valueUri the value uri
		 * @return the value info
		 * @throws JsonRpcException the json rpc exception
		 */
		public ValueInfo getValueInfo(String fileUri, String valueUri) throws JsonRpcException {
			return new ValueInfo(execute("getValueInfo", token, fileUri, valueUri));
		}

		/**
		 * Gets the file.
		 *
		 * @param fileUri the file uri
		 * @return the file
		 * @throws JsonRpcException the json rpc exception
		 */
		public SDSDFile getFile(String fileUri) throws JsonRpcException {
			return new SDSDFile(execute("getFile", token, fileUri));
		}

		/**
		 * List files.
		 *
		 * @param sdsdtype the sdsdtype
		 * @return the list
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<SDSDFileMeta> listFiles(String sdsdtype) throws JsonRpcException {
			return result(execute("listFiles", token, sdsdtype).getJSONArray("files"), SDSDFileMeta::new);
		}

		/** The Constant EVENT_FILE_DELETED. */
		private static final String EVENT_NEW_DATA = "newData", EVENT_NEW_FILE = "newFile",
				EVENT_FILE_APPENDED = "fileAppended", EVENT_FILE_DELETED = "fileDeleted";

		/**
		 * Sets the new data listener.
		 *
		 * @param callback the new new data listener
		 * @throws JsonRpcException the json rpc exception
		 */
		public void setNewDataListener(Consumer<SDSDFileMeta> callback) throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_NEW_DATA, token, "") {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		/**
		 * Unset new data listener.
		 *
		 * @throws JsonRpcException the json rpc exception
		 */
		public void unsetNewDataListener() throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_NEW_DATA, token, "");
		}

		/**
		 * Sets the new file listener.
		 *
		 * @param callback the new new file listener
		 * @throws JsonRpcException the json rpc exception
		 */
		public void setNewFileListener(Consumer<SDSDFileMeta> callback) throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_NEW_FILE, token, "") {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		/**
		 * Unset new file listener.
		 *
		 * @throws JsonRpcException the json rpc exception
		 */
		public void unsetNewFileListener() throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_NEW_FILE, token, "");
		}

		/**
		 * Sets the file appended listener.
		 *
		 * @param callback the callback
		 * @param fileUri  the file uri
		 * @throws JsonRpcException the json rpc exception
		 */
		public void setFileAppendedListener(Consumer<SDSDFileAppended> callback, String fileUri)
				throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_FILE_APPENDED, token, fileUri) {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileAppended(t.getJSONObject(0)));
				}
			});
		}

		/**
		 * Unset file appended listener.
		 *
		 * @param fileUri the file uri
		 * @throws JsonRpcException the json rpc exception
		 */
		public void unsetFileAppendedListener(String fileUri) throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_FILE_APPENDED, token, fileUri);
		}

		/**
		 * Sets the file deleted listener.
		 *
		 * @param callback the callback
		 * @param fileUri  the file uri
		 * @throws JsonRpcException the json rpc exception
		 */
		public void setFileDeletedListener(Consumer<SDSDFileMeta> callback, String fileUri) throws JsonRpcException {
			client.setEventListener(new SDSDListener(ENDPOINT, EVENT_FILE_DELETED, token, fileUri) {
				@Override
				public void accept(JSONArray t) {
					callback.accept(new SDSDFileMeta(t.getJSONObject(0)));
				}
			});
		}

		/**
		 * Unset file deleted listener.
		 *
		 * @param fileUri the file uri
		 * @throws JsonRpcException the json rpc exception
		 */
		public void unsetFileDeletedListener(String fileUri) throws JsonRpcException {
			client.unsetEventListener(ENDPOINT, EVENT_FILE_DELETED, token, fileUri);
		}

		/**
		 * Send file.
		 *
		 * @param filename the filename
		 * @param content  the content
		 * @return true, if successful
		 * @throws JsonRpcException the json rpc exception
		 */
		public boolean sendFile(String filename, byte[] content) throws JsonRpcException {
			JSONObject result = execute("sendFile", token, REGEX_ILLEGAL_CHARS.matcher(filename).replaceAll(""),
					Base64.getEncoder().encodeToString(content));
			return result.getBoolean("success");
		}

		/**
		 * Append file.
		 *
		 * @param filename   the filename
		 * @param newContent the new content
		 * @return true, if successful
		 * @throws JsonRpcException the json rpc exception
		 */
		public boolean appendFile(String filename, byte[] newContent) throws JsonRpcException {
			JSONObject result = execute("appendFile", token, REGEX_ILLEGAL_CHARS.matcher(filename).replaceAll(""),
					Base64.getEncoder().encodeToString(newContent));
			return result.getBoolean("success");
		}

		/**
		 * Find geometry.
		 *
		 * @param fileUri     the file uri
		 * @param uri         the uri
		 * @param elementtype the elementtype
		 * @param geotype     the geotype
		 * @return the list
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<JSONObject> findGeometry(@Nullable String fileUri, @Nullable String uri,
				@Nullable ElementType elementtype, @Nullable GeoType geotype) throws JsonRpcException {
			JSONObject featureCollection = execute("findGeometry", token, fileUri, uri,
					elementtype != null ? elementtype.name() : null, geotype != null ? geotype.name() : null);
			return result(featureCollection.getJSONArray("features"), Function.identity());
		}

		/**
		 * Gets the device element properties.
		 *
		 * @param fileUri the file uri
		 * @param detUri  the det uri
		 * @return the device element properties
		 * @throws JsonRpcException the json rpc exception
		 */
		public DeviceElementProperties getDeviceElementProperties(@Nullable String fileUri, String detUri)
				throws JsonRpcException {
			return new DeviceElementProperties(execute("getDeviceElementProperties", token, fileUri, detUri));
		}

		/**
		 * Geo intersects.
		 *
		 * @param geometry    the geometry
		 * @param elementtype the elementtype
		 * @param geotype     the geotype
		 * @return the list
		 * @throws JsonRpcException the json rpc exception
		 */
		public List<JSONObject> geoIntersects(JSONObject geometry, @Nullable ElementType elementtype,
				@Nullable GeoType geotype) throws JsonRpcException {
			JSONObject featureCollection = execute("geoIntersects", token, geometry,
					elementtype != null ? elementtype.name() : null, geotype != null ? geotype.name() : null);
			return result(featureCollection.getJSONArray("features"), Function.identity());
		}

		/**
		 * Sets the error.
		 *
		 * @param error the error
		 * @return true, if successful
		 * @throws JsonRpcException the json rpc exception
		 */
		public boolean setError(@Nullable String error) throws JsonRpcException {
			return execute("setError", token, error).getBoolean("success");
		}

		/**
		 * Complete.
		 *
		 * @param url the url
		 * @return true, if successful
		 * @throws JsonRpcException the json rpc exception
		 */
		public boolean complete(String url) throws JsonRpcException {
			return execute("complete", token, url).getBoolean("success");
		}

		/**
		 * Find value uris.
		 *
		 * @param fileUri the file uri
		 * @param ddis    the ddis
		 * @return the value uri helper
		 * @throws JsonRpcException the json rpc exception
		 */
		public ValueUriHelper findValueUris(String fileUri, int... ddis) throws JsonRpcException {
			ValueUriHelper valueUriHelper = new ValueUriHelper(ddis);
			valueUriHelper.init(fileUri);
			return valueUriHelper;
		}

		/**
		 * The Class ValueUriHelper.
		 */
		public class ValueUriHelper {

			/** The ddis. */
			private final Map<Integer, List<String>> ddis;

			/** The keys. */
			private final Map<String, Set<String>> keys = new HashMap<>();

			/**
			 * Instantiates a new value uri helper.
			 *
			 * @param ddis the ddis
			 */
			public ValueUriHelper(int... ddis) {
				this.ddis = new HashMap<>(ddis.length);
				for (int ddi : ddis) {
					this.ddis.put(ddi, new ArrayList<>());
				}
			}

			/**
			 * Inits the.
			 *
			 * @param fileUri the file uri
			 * @throws JsonRpcException the json rpc exception
			 */
			public void init(String fileUri) throws JsonRpcException {
				for (FindResult res : find().byFile(fileUri).byType(R_DLV)
						.byAttribute(P_DDI, ddis.keySet().stream().map(Util.DDI::inst).toArray()).exec()) {
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

			/**
			 * Gets the value uri.
			 *
			 * @param tlName the tl name
			 * @param ddi    the ddi
			 * @return the value uri
			 */
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

	/**
	 * The Enum ElementType.
	 */
	public static enum ElementType {

		/** The Other. */
		Other,
		/** The Time log. */
		TimeLog,
		/** The Field. */
		Field,
		/** The Treatment zone. */
		TreatmentZone,
		/** The Guidance pattern. */
		GuidancePattern,
		/** The Field access. */
		FieldAccess
	}

	/**
	 * The Enum GeoType.
	 */
	public static enum GeoType {

		/** The Point. */
		Point,
		/** The Line string. */
		LineString,
		/** The Polygon. */
		Polygon,
		/** The Multi point. */
		MultiPoint,
		/** The Multi line string. */
		MultiLineString,
		/** The Multi polygon. */
		MultiPolygon,
		/** The Geometry collection. */
		GeometryCollection;
	}

	/**
	 * Geo json B box.
	 *
	 * @param minLat  the min lat
	 * @param minLong the min long
	 * @param maxLat  the max lat
	 * @param maxLong the max long
	 * @return the JSON object
	 */
	public static JSONObject geoJsonBBox(double minLat, double minLong, double maxLat, double maxLong) {
		JSONArray ring = new JSONArray().put(new JSONArray().put(minLong).put(minLat))
				.put(new JSONArray().put(minLong).put(maxLat)).put(new JSONArray().put(maxLong).put(maxLat))
				.put(new JSONArray().put(maxLong).put(minLat)).put(new JSONArray().put(minLong).put(minLat));
		return new JSONObject().put("type", GeoType.Polygon.name()).put("coordinates", new JSONArray().put(ring));
	}

	// =========
	// helper

	/**
	 * Result.
	 *
	 * @param <T>     the generic type
	 * @param res     the res
	 * @param creator the creator
	 * @return the list
	 * @throws JSONException the JSON exception
	 */
	static <T> List<T> result(JSONArray res, Function<JSONObject, T> creator) throws JSONException {
		List<T> list = new ArrayList<>(res.length());
		for (int i = 0; i < res.length(); ++i) {
			list.add(creator.apply(res.getJSONObject(i)));
		}
		return list;
	}

	/**
	 * Result.
	 *
	 * @param <T>     the generic type
	 * @param res     the res
	 * @param creator the creator
	 * @return the map
	 * @throws JSONException the JSON exception
	 */
	static <T> Map<String, T> result(JSONObject res, Function<JSONObject, T> creator) throws JSONException {
		Map<String, T> map = new HashMap<>(res.length());
		for (String key : res.keySet()) {
			map.put(key, creator.apply(res.getJSONObject(key)));
		}
		return map;
	}

	/**
	 * The Class JsonRpcException.
	 */
	public static class JsonRpcException extends Exception {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -518914703894017313L;

		/** The code. */
		private int code;

		/**
		 * Instantiates a new json rpc exception.
		 *
		 * @param error the error
		 */
		public JsonRpcException(JSONObject error) {
			super(error.optString("msg", error.optString("message", "JsonRpcException")));
			code = error.optInt("code", 0);
		}

		/**
		 * Gets the code.
		 *
		 * @return the code
		 */
		public int getCode() {
			return code;
		}
	}

	/**
	 * Execute.
	 *
	 * @param method the method
	 * @param token  the token
	 * @param params the params
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	private JSONObject execute(String method, String token, Object... params) throws JsonRpcException {
		return client.execute(ENDPOINT, method, token, params);
	}

	/**
	 * Close.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void close() throws Exception {
		client.close();
	}

}
