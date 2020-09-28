package de.sdsd.projekt.prototype.jsonrpc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilRDFVisitor;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiFormat;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiType;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * Implementation of the webserver jsonRPC interface, accessible over /sdsd/json-rpc.
 *
 * @author Markus Schr&ouml;der
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public abstract class JsonRpcEndpoint {
	
	public static final Random RANDOM = new Random();

	protected ApplicationLogic application;

	public JsonRpcEndpoint(ApplicationLogic application) {
		this.application = application;
	}
	
	/**
	 * Thrown to indicate that the user is not logged in.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class NoLoginException extends Exception {
		private static final long serialVersionUID = -8357959906500287172L;
		
		public NoLoginException() {
			super("Not logged in.");
		}
	}
	
	/**
	 * Thrown to indicate that the user has no active agrirouter onboarding.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class NoOnboardingException extends Exception {
		private static final long serialVersionUID = 527306270618037365L;
		
		public NoOnboardingException() {
			super("Not onboarded to agrirouter.");
		}
	}
	
	public static Object exceptionTransformer(Throwable e) {
		return e instanceof JsonRpcException 
				? ((JsonRpcException)e).toJsonRpcError()
				: e;
	}
	
	/**
	 * Base class for JSON-RPC exceptions.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	static class JsonRpcException extends Exception {
		private static final long serialVersionUID = 7204748671104102687L;
		
		private final int code;
		
		public JsonRpcException(int code, String message) {
			super(message);
			this.code = code;
		}
		
		public JSONObject toJsonRpcError() {
			return new JSONObject()
					.put("code", code)
					.put("message", getMessage());
		}
	}
	
	protected static final String INTERNAL_ERROR = "Internal Server Error... Please report.";
	
	protected JsonRpcException createError(User user, Throwable e) {
		if(e instanceof NoLoginException)
			return new JsonRpcException(-32000, e.getMessage());
		else if(e instanceof NoOnboardingException)
			return new JsonRpcException(-32001, e.getMessage());
		else if(e instanceof SDSDException) {
			System.err.println(e.getMessage());
			if(user != null) application.logError(user, e.getLocalizedMessage());
			return new JsonRpcException(-32002, e.getMessage());
		}
		else if(e instanceof ARException) {
			System.err.println(e.getMessage());
			if(user != null) application.logError(user, e.getLocalizedMessage());
			return new JsonRpcException(-32003, e.getMessage());
		}
		else {
			e.printStackTrace();
			if(user != null) application.logError(user, INTERNAL_ERROR);
			return new JsonRpcException(-32004, INTERNAL_ERROR);
		}
	}
	
	public static JSONObject success(boolean success) {
		return new JSONObject().put("success", success);
	}
	
	public static String isoUTC(@Nullable Instant datetime) {
		return datetime == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(datetime.atOffset(ZoneOffset.UTC));
	}
	
	public static JSONObject artype(ARMessageType type) {
		return new JSONObject()
				.put("type", type.technicalMessageType())
				.put("name", type.toString());
	}
	
	public static JSONObject labeled(String value, String label) {
		return new JSONObject()
				.put("value", value)
				.put("label", label);
	}
	
	/**
	 * Helper for creating value-label JSON-RPC output.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Res extends JSONObject {
		public Res(String value, String label) {
			setValue(value);
			setLabel(label);
		}
		
		public String getValue() {
			return getString("value");
		}
		
		public Res setValue(String value) {
			put("value", value);
			return this;
		}
		
		public String getLabel() {
			return getString("label");
		}
		
		public Res setLabel(String label) {
			put("label", label);
			return this;
		}
	}
	
	/**
	 * Helper for creating input for grouped options.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Groups {
		private final Map<String, JSONObject> groups;
		public Groups(int groupCount) {
			groups = new LinkedHashMap<>(groupCount);
		}
		
		public void addGroup(String key, @Nullable String label) {
			if(label == null) label = key;
			if(!groups.containsKey(key))
				groups.put(key, new JSONObject().put("value", key).put("label", label));
		}
		
		public void addItem(String groupKey, String value, @Nullable String label) {
			if(label == null) label = value;
			JSONObject group = groups.get(groupKey);
			JSONArray list = group.optJSONArray("list");
			if(list == null) group.put("list", list = new JSONArray());
			list.put(new Res(value, label));
		}
		
		public JSONArray toJson() {
			return groups.values().stream()
					.filter(group -> group.has("list"))
					.collect(Util.toJSONArray());
		}
	}
	
	public static final JSONObject getOrCreate(JSONObject parent, String key) {
		JSONObject obj = parent.optJSONObject(key);
		if(obj == null) parent.put(key, obj = new JSONObject());
		return obj;
	}
	public static final <K, V> V getOrCreate(Map<K, V> map, K key, Supplier<V> value) {
		V val = map.get(key);
		if(val == null) map.put(key, val = value.get());
		return val;
	}
	
	public static final WikiType T_DDI = TripleFunctions.FORMAT_UNKNOWN.res("ddi");
	public static final WikiFormat T_ISOXML = new WikiFormat("isoxml");
	
	public static final UtilRDFVisitor<Resource> DDI_RES = new UtilRDFVisitor<Resource>() {
		@Override
		public Resource visitURI(Resource r, String uri) {
			return r;
		}
		
		@Override
		public Resource visitLiteral(Literal l) {
			return T_DDI.inst(l.getString());
		}
		
		@Override
		public Resource visitBlank(Resource r, AnonId id) {
			return null;
		}
	};
	public static final Pattern DDI_REGEX = Pattern.compile(Pattern.quote(TripleFunctions.createWikiInstanceUri(T_DDI.getURI(), "")) + "(\\d+)");
	public static final UtilRDFVisitor<Integer> DDI_INT = new UtilRDFVisitor<Integer>() {
		@Override
		public Integer visitURI(Resource r, String uri) {
			Matcher matcher = DDI_REGEX.matcher(uri);
			if(matcher.matches()) return Integer.parseInt(matcher.group(1));
			else return null;
		}
		
		@Override
		public Integer visitLiteral(Literal l) {
			return l.getInt();
		}
		
		@Override
		public Integer visitBlank(Resource r, AnonId id) {
			return null;
		}
	};
 	
	protected static String getSessionId(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie c : req.getCookies()) {
				if (c.getName().equals("SDSDSESSION")) {
					return c.getValue();
				}
			}
		}
		return null;
	}
	
	protected static String getSetSessionId(HttpServletRequest req, HttpServletResponse res) {
		String sessionId = getSessionId(req);
		if(sessionId == null) {
			byte[] rand = new byte[16];
			RANDOM.nextBytes(rand);
			sessionId = Hex.encodeHexString(rand);
			Cookie c = new Cookie("SDSDSESSION", sessionId);
			c.setPath("/");
			res.addCookie(c);
		}
		return sessionId;
	}
	
}
