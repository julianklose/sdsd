package de.sdsd.projekt.prototype.jsonrpc;

import static de.sdsd.projekt.prototype.data.Permissions.ACCESS_DENIED;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TableFunctions;
import de.sdsd.projekt.prototype.applogic.TableFunctions.ElementKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.FileKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;
import de.sdsd.projekt.prototype.applogic.TableFunctions.TimeInterval;
import de.sdsd.projekt.prototype.applogic.TableFunctions.TimelogInfo;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.DeduplicatedResource;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiType;
import de.sdsd.projekt.prototype.applogic.WikinormiaFunctions.Sorting;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.GeoElement.GeoType;
import de.sdsd.projekt.prototype.data.Permissions;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.Service;
import de.sdsd.projekt.prototype.data.ServiceInstance;
import de.sdsd.projekt.prototype.data.Timelog;
import de.sdsd.projekt.prototype.data.TimelogPosition;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.data.ValueInfo;
import de.sdsd.projekt.prototype.data.WikiEntry;
import de.sdsd.projekt.prototype.websocket.WebsocketConnection;


/**
 * JSONRPC-Endpoint for service API functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ApiEndpoint extends JsonRpcEndpoint {
	
	/**
	 * Instantiates a new api endpoint.
	 *
	 * @param application the application
	 */
	public ApiEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Write instance.
	 *
	 * @param inst the inst
	 * @return the JSON object
	 */
	public static JSONObject writeInstance(ServiceInstance inst) {
		return new JSONObject()
				.put("token", inst.getToken())
				.put("activated", isoUTC(inst.getActivated()))
				.put("parameter", inst.getParameter());
	}
	
	/**
	 * List active instances.
	 *
	 * @param serviceToken the service token
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listActiveInstances(String serviceToken) throws JsonRpcException {
		try {
			Optional<Service> service = application.service.getService(Service.filterToken(serviceToken));
			if(!service.isPresent())
				throw new SDSDException("Service with the given token does not exist");
			JSONArray list = application.service.listInstances(ServiceInstance.filterReady(service.get())).stream()
					.map(ApiEndpoint::writeInstance)
					.collect(Util.toJSONArray());
			return new JSONObject().put("activeInstances", list);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the instance changed listener.
	 *
	 * @param conn the conn
	 * @param serviceToken the service token
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setInstanceChangedListener(WebsocketConnection conn, String serviceToken) throws JsonRpcException {
		try {
			Optional<Service> service = application.service.getService(Service.filterToken(serviceToken));
			System.out.format("setInstanceChangedListener: connection(%s) service(%s)\n", 
					conn.getId(), service.isPresent() ? service.get().getName() : "unknown");
			if(!service.isPresent())
				throw new SDSDException("Service with the given token does not exist");
			application.service.instanceStopped.setListener(service.get(), conn);
			application.service.instanceChanged.setListener(service.get(), 
					conn.listener("api", "instanceChanged", null, serviceToken, ApiEndpoint::writeInstance));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset instance changed listener.
	 *
	 * @param conn the conn
	 * @param serviceToken the service token
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetInstanceChangedListener(WebsocketConnection conn, String serviceToken) throws JsonRpcException {
		try {
			Optional<Service> service = application.service.getService(Service.filterToken(serviceToken));
			System.out.format("unsetInstanceChangedListener: connection(%s) service(%s)\n", 
					conn.getId(), service.isPresent() ? service.get().getName() : "unknown");
			if(!service.isPresent())
				throw new SDSDException("Service with the given token does not exist");
			application.service.instanceChanged.unsetListener(service.get(), conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the instance canceled listener.
	 *
	 * @param conn the conn
	 * @param serviceToken the service token
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setInstanceCanceledListener(WebsocketConnection conn, String serviceToken) throws JsonRpcException {
		try {
			Optional<Service> service = application.service.getService(Service.filterToken(serviceToken));
			System.out.format("setInstanceCanceledListener: connection(%s) service(%s)", 
					conn.getId(), service.isPresent() ? service.get().getName() : "unknown");
			if(!service.isPresent())
				throw new SDSDException("Service with the given token does not exist");
			application.service.instanceCanceled.setListener(service.get(), 
					conn.listener("api", "instanceCanceled", null, serviceToken, ApiEndpoint::writeInstance));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset instance cancelled listener.
	 *
	 * @param conn the conn
	 * @param serviceToken the service token
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetInstanceCancelledListener(WebsocketConnection conn, String serviceToken) throws JsonRpcException {
		try {
			Optional<Service> service = application.service.getService(Service.filterToken(serviceToken));
			System.out.format("unsetInstanceCanceledListener: connection(%s) service(%s)", 
					conn.getId(), service.isPresent() ? service.get().getName() : "unknown");
			if(!service.isPresent())
				throw new SDSDException("Service with the given token does not exist");
			application.service.instanceCanceled.unsetListener(service.get(), conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	

	/**
	 * Gets the context.
	 *
	 * @param req the req
	 * @return the context
	 * @throws SDSDException the SDSD exception
	 */
	private ServiceInstance getContext(HttpServletRequest req) throws SDSDException {
		String token = req.getHeader("token");
		if (token == null) {
			throw new SDSDException("Required header value (token) missing");
		}

		Bson filter = Filters.and(ServiceInstance.filterToken(token), ServiceInstance.filterCompleted(false));
		Optional<ServiceInstance> context = application.service.getInstance(filter);
		if(context.isPresent()) return context.get();
		else throw new SDSDException("No active context for the given token");
	}
	
	/**
	 * Complete.
	 *
	 * @param req the req
	 * @param result the result
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject complete(HttpServletRequest req, String result) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("complete: user(%s) service(%s)\n", 
					context.getUser(), context.getServiceName());
			return success(application.service.completeInstance(context, result));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the error.
	 *
	 * @param req the req
	 * @param error the error
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setError(HttpServletRequest req, String error) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("setError: user(%s) service(%s) error(%s)\n", 
					context.getUser(), context.getServiceName(), error);
			return success(application.service.updateInstance(context, context.setError(error)));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the label.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @return the label
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getLabel(HttpServletRequest req, @Nullable String fileUri, String uri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getLabel: user(%s) service(%s) fileUri(%s) uri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, uri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(uri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			Var LABEL = Var.alloc("label");
			SelectBuilder sb = new SelectBuilder()
					.addVar(LABEL)
					.addWhere(accessUri, RDFS.label, LABEL);
			if(fileUri != null) 
				sb.from(fileUri);
			else 
				sb.from(graphs);
			
			try (QueryResult qr = application.triple.query(sb.build())) {
				Optional<UtilQuerySolution> qs = qr.first();
				return qs.isPresent() ? new JSONObject().put("label", qs.get().getString(LABEL)) : new JSONObject();
			}
		} catch (Throwable e) {
			throw createError(null, e);
		} 
	}

	/**
	 * Gets the object.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @return the object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getObject(HttpServletRequest req, @Nullable String fileUri, String uri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getObject: user(%s) service(%s) fileUri(%s) uri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, uri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(uri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			Triple triple = new Triple(accessUri, Var.alloc("p"), Var.alloc("o"));
			ConstructBuilder query = new ConstructBuilder()
					.addConstruct(triple)
					.addWhere(triple);
			if(fileUri != null) query.from(fileUri);
			else query.from(graphs);
			
			try (QueryResult qr = application.triple.query(query.build())) {
				Model m = qr.construct();
				
				StringWriter jsonldStringWriter = new StringWriter();
				m.write(jsonldStringWriter, "JSON-LD");

				return new JSONObject(jsonldStringWriter.toString());
			}
		} catch (Throwable e) {
			throw createError(null, e);
		} 
	}
	
	/**
	 * Gets the parts.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @param type the type
	 * @return the parts
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getParts(HttpServletRequest req, @Nullable String fileUri, String uri, @Nullable String type) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getParts: user(%s) service(%s) fileUri(%s) uri(%s) type(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, uri, type);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(uri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			Var P = Var.alloc("part"), T = Var.alloc("type");
			SelectBuilder sb = new SelectBuilder()
					.addVar(P).addVar(T)
					.addWhere(P, DCTerms.isPartOf, accessUri);
			if(fileUri != null) 
				sb.from(fileUri);
			else 
				sb.from(graphs);
			if(type != null)
				sb.addWhere(P, RDF.type, T).addWhereValueVar(T, NodeFactory.createURI(type));
			else
				sb.addOptional(P, RDF.type, T);
			
			JSONArray parts = new JSONArray();
			try(QueryResult qr = application.triple.query(sb.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					JSONObject part = new JSONObject()
							.put("uri", qs.getUri(P));
					if(qs.contains(T))
						part.put("type", qs.getUri(T));
					parts.put(part);
				}
			}
			return new JSONObject().put("parts", parts);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Traverse.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @param properties the properties
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject traverse(HttpServletRequest req, @Nullable String fileUri, String uri, JSONArray properties) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getValue: user(%s) service(%s) fileUri(%s) uri(%s) properties%s\n", 
					context.getUser(), context.getServiceName(), fileUri, uri, properties.toString());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(uri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			if(properties.length() == 0)
				throw new SDSDException("No properties set");
			
			StringBuilder pb = new StringBuilder();
			pb.append('<').append(properties.getString(0));
			for(int i = 1; i < properties.length(); ++i) {
				pb.append(">/<").append(properties.getString(i));
			}
			pb.append('>');
			
			Var V = Var.alloc("value"), T = Var.alloc("type");
			SelectBuilder sb = new SelectBuilder()
					.addVar(V).addVar(T)
					.addWhere(accessUri, pb.toString(), V)
					.addOptional(V, RDF.type, T);
			if(fileUri != null) 
				sb.from(fileUri);
			else 
				sb.from(graphs);
			
			JSONArray values = new JSONArray();
			try(QueryResult qr = application.triple.query(sb.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					values.put(new JSONObject()
							.put("value", qs.get(V).toString())
							.put("type", qs.getUri(T)));
				}
			}
			return new JSONObject().put("values", values);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the value.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @param attributeUri the attribute uri
	 * @return the value
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getValue(HttpServletRequest req, @Nullable String fileUri, String uri, String attributeUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getValue: user(%s) service(%s) fileUri(%s) uri(%s) attributeUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, uri, attributeUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(uri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			Var V = Var.alloc("value"), T = Var.alloc("type");
			SelectBuilder sb = new SelectBuilder()
					.addVar(V).addVar(T)
					.addWhere(accessUri, NodeFactory.createURI(attributeUri), V)
					.addOptional(V, RDF.type, T);
			if(fileUri != null) 
				sb.from(fileUri);
			else 
				sb.from(graphs);
			
			JSONArray values = new JSONArray();
			try(QueryResult qr = application.triple.query(sb.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					values.put(new JSONObject()
							.put("value", qs.get(V).toString())
							.put("type", qs.getUri(T)));
				}
			}
			return new JSONObject().put("values", values);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}

	/**
	 * Gets the source file uri.
	 *
	 * @param req the req
	 * @param uri the uri
	 * @return the source file uri
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getSourceFileUri(HttpServletRequest req, String uri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getSourceFileUri: user(%s) service(%s) uri(%s)\n", 
					context.getUser(), context.getServiceName(), uri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(null);
			
			final Var G = Var.alloc("g");
			Query query = new SelectBuilder()
					.addVar(G)
					.fromNamed(graphs)
					.addGraph(G, NodeFactory.createURI(uri), RDF.type, Var.ANON)
					.build();
			
			try(QueryResult qe = application.triple.query(query)) {
				Optional<UtilQuerySolution> graph = qe.first();
				if(graph.isPresent()) {
					String graphuri = graph.get().getUri(G);
					return new JSONObject().put("fileUri", graphuri);
				}
				else
					throw new SDSDException("The given URI was not found or you have no access to it ");
			}

		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Find.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param type the type
	 * @param attr the attr
	 * @param uris the uris
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject find(HttpServletRequest req, @Nullable String fileUri, @Nullable String type, 
			@Nullable JSONObject attr, @Nullable JSONArray uris) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("find: user(%s) service(%s) fileUri(%s) type(%s) attr(%s) uris(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, type, attr, uris);

			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			if(type == null && (attr == null || attr.length() == 0))
				throw new SDSDException("No filter specified");
			
			Map<String, Var> varMap = new HashMap<>();
			
			final Var S = Var.alloc("s"), T = Var.alloc("t");
			SelectBuilder sb = new SelectBuilder()
					.setDistinct(true)
					.addVar(S).addVar(T);
			ExprFactory expr = sb.getExprFactory();
			if(fileUri != null)
				sb.from(fileUri);
			else
				sb.from(graphs);
			if(type != null)
				sb.addWhere(S, RDF.type, T).addFilter(expr.eq(T, NodeFactory.createURI(type)));
			else
				sb.addOptional(S, RDF.type, T);
			if(attr != null) {
				for(String pUri : attr.keySet()) {
					Object obj = attr.get(pUri);
					Var V = Var.alloc("v" + varMap.size());
					sb.addVar(V);
					varMap.put(pUri, V);
					
					if(JSONObject.NULL.equals(obj))
						sb.addOptional(S, NodeFactory.createURI(pUri), V);
					else
						sb.addWhere(S, NodeFactory.createURI(pUri), V);
					
					if(obj instanceof JSONArray) {
						JSONArray list = (JSONArray) obj;
						Object[] arr = new Object[list.length()];
						for(int i = 0; i < list.length(); ++i) {
							Object o = list.get(i);
							if(o instanceof String) {
								String ostr = (String)o;
								if(ostr.startsWith(TripleFunctions.NS_INTERN) || ostr.startsWith(TripleFunctions.NS_WIKI))
									o = "<" + ostr + ">";
							}
							arr[i] = o;
						}
						sb.addFilter(expr.in(V, arr));
					}
					else if(!JSONObject.NULL.equals(obj)) {
						if(obj instanceof String) {
							String ostr = (String)obj;
							if(ostr.startsWith(TripleFunctions.NS_INTERN) || ostr.startsWith(TripleFunctions.NS_WIKI))
								obj = "<" + ostr + ">";
						}
						sb.addFilter(expr.eq(V, obj));
					}
				}
			}
			if(uris != null) {
				Object[] arr = new Object[uris.length()];
				for(int i = 0; i < uris.length(); ++i) {
					String uri = uris.getString(i);
					if(uri.startsWith(TripleFunctions.NS_INTERN) || uri.startsWith(TripleFunctions.NS_WIKI))
						uri = "<" + uri + ">";
					arr[i] = uri;
				}
				sb.addFilter(expr.in(S, arr));
			}
			
			JSONObject resp = new JSONObject();
			try(QueryResult qr = application.triple.query(sb.build())) {
				List<Node> accessUris = new ArrayList<>();
				JSONArray instances = new JSONArray();
				for(UtilQuerySolution qs : qr.iterate()) {
					accessUris.add(qs.getResource(S).asNode());
					JSONObject res = new JSONObject()
							.put("uri", qs.getUri(S));
					if(qs.contains(T))
						res.put("type", qs.getUri(T));
					for(Entry<String, Var> e : varMap.entrySet()) {
						RDFNode v = qs.get(e.getValue());
						if(v instanceof Literal)
							res.put(e.getKey(), v.asLiteral().getValue());
						else if(v instanceof Resource)
							res.put(e.getKey(), v.asResource().getURI());
					}
					instances.put(res);
				}
				
				if(accessUris.size() > 0) {
					Set<Node> filterAllowed = permissions.filterAllowed(graphs, accessUris);
					if(filterAllowed.isEmpty())
						throw ACCESS_DENIED;
					for(int i = instances.length()-1; i >= 0; --i) {
						if(!filterAllowed.contains(accessUris.get(i)))
							instances.remove(i);
					}
				}
				
				resp.put("instances", instances);
			}
			
			return resp;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * List devices.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listDevices(HttpServletRequest req) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("listDevices: user(%s) service(%s)\n",
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			WikiType tdlv = T_ISOXML.res("DLV"), tdvc = T_ISOXML.res("DVC"), tdet = T_ISOXML.res("DET");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isAllowed(tdvc.getURI()) || !permissions.isAllowed(tdlv.getURI()))
				throw ACCESS_DENIED;
			
			Var DVC=Var.alloc("dvc"), CL=Var.alloc("cl"), DVCLABEL=Var.alloc("dvclabel"), SN=Var.alloc("sn"),
					DET=Var.alloc("det"), DETNO=Var.alloc("detNo"), DETLABEL=Var.alloc("detlabel"), DETTYPE=Var.alloc("dettype"), TYPE=Var.alloc("type"),
					DLV=Var.alloc("dlv"), DDI=Var.alloc("ddi");
			SelectBuilder query = new SelectBuilder()
					.setDistinct(true)
					.addVar(CL).addVar(DVCLABEL).addVar(SN)
					.addVar(DETNO).addVar(DETLABEL).addVar(DETTYPE).addVar(TYPE)
					.addVar(DDI)
					.from(permissions.getFileGraphs(null))
					.addWhere(DVC, tdvc.prop("D"), CL)
					.addOptional(DVC, tdvc.prop("B"), DVCLABEL)
					.addOptional(DVC, tdvc.prop("E"), SN)
					.addWhere(DET, DCTerms.isPartOf, DVC)
					.addWhere(DET, tdet.prop("E"), DETNO)
					.addOptional(DET, tdet.prop("D"), DETLABEL)
					.addWhere(DET, tdet.prop("C"), DETTYPE)
					.addOptional(DETTYPE, RDFS.label, TYPE)
					.addWhere(DLV, tdlv.prop("C"), DET)
					.addWhere(DLV, tdlv.prop("A"), DDI);
			
			JSONObject out = new JSONObject();
			try(QueryResult qr = application.triple.query(query.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					String clientname = Hex.encodeHexString((byte[]) qs.getLiteral(CL).getValue());
					JSONObject dvc = out.optJSONObject(clientname);
					if(dvc == null) out.put(clientname, dvc = new JSONObject());
					if(qs.contains(DVCLABEL)) dvc.put("designator", qs.getString(DVCLABEL));
					if(qs.contains(SN)) dvc.put("serial", qs.getString(SN));
					
					JSONObject dets = dvc.optJSONObject("elements");
					if(dets == null) dvc.put("elements", dets = new JSONObject());
					JSONObject det = dets.optJSONObject(qs.getString(DETNO));
					if(det == null) dets.put(qs.getString(DETNO), det = new JSONObject());
					if(qs.contains(DETLABEL)) det.put("designator", qs.getString(DETLABEL));
					det.put("type", qs.contains(TYPE) ? qs.getString(TYPE) : qs.getString(DETTYPE));
					
					det.append("ddis", qs.get(DDI, DDI_INT));
				}
			}

			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Find time logs.
	 *
	 * @param req the req
	 * @param clientName the client name
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject findTimeLogs(HttpServletRequest req, String clientName) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("findTimeLogs: user(%s) service(%s) clientname(%s)\n",
					context.getUser(), context.getServiceName(), clientName);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			WikiType device = T_ISOXML.res("DVC"), timelogvalue = T_ISOXML.res("DLV"), timelog = T_ISOXML.res("TLG");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isAllowed(device.getURI()) || !permissions.isAllowed(timelogvalue.getURI()) 
					|| !permissions.isAllowed(timelog.getURI()))
				throw ACCESS_DENIED;
			
			Literal dvcID = ResourceFactory.createTypedLiteral(clientName, XSDDatatype.XSDhexBinary);
			Var G = Var.alloc("g"), DVC = Var.alloc("dvc"), 
					DLV = Var.alloc("dlv"), DDI = Var.alloc("ddi"),
					TLG = Var.alloc("tlg"), TLGNAME = Var.alloc("tlgname");
			SelectBuilder query = new SelectBuilder()
					.setDistinct(true)
					.addVar(G).addVar(TLGNAME).addVar(DLV).addVar(DDI)
					.fromNamed(permissions.getFileGraphs(null))
					.addGraph(G, new WhereBuilder()
							.addWhere(DVC, device.prop("D"), dvcID)
							.addWhere(DLV, PathFactory.pathSeq(Util.path(timelogvalue.prop("C")), Util.path(DCTerms.isPartOf)), DVC)
							.addWhere(DLV, timelogvalue.prop("A"), DDI)
							.addWhere(DLV, DCTerms.isPartOf, TLG)
							.addWhere(TLG, TripleFunctions.TIMELOG.prop("name"), TLGNAME));
			
			JSONObject out = new JSONObject();
			try(QueryResult qr = application.triple.query(query.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					Key key = new Key(user.getName(), qs.getUri(G), qs.getString(TLGNAME), qs.getUri(DLV));
					ElementKey tlgKey = key.toElementKey();
					
					JSONObject tlg = getOrCreate(getOrCreate(out, key.file), key.name);
					if(tlg.length() == 0) {
						TimelogInfo tlgInfo = application.table.getTimelogInfo(tlgKey);
						tlg.put("count", tlgInfo.count)
								.put("from", isoUTC(tlgInfo.from))
								.put("until", isoUTC(tlgInfo.until));
					}
					
					tlg.put(key.valueUri, new JSONObject().put("ddi", qs.get(DDI, DDI_INT)));
				}
			}
			
			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Find totals by device.
	 *
	 * @param req the req
	 * @param clientName the client name
	 * @param timeFilter the time filter
	 * @param ddis the ddis
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject findTotalsByDevice(HttpServletRequest req, String clientName, JSONObject timeFilter, JSONArray ddis) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("findTotalsByDevice: user(%s) service(%s) clientname(%s) timeFilter(%s) ddis(%d)\n", 
					context.getUser(), context.getServiceName(), clientName, timeFilter, ddis.length());

			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getFileGraphs(null);
			
			WikiType device = T_ISOXML.res("DVC"), timelogvalue = T_ISOXML.res("DLV"), time = T_ISOXML.res("TIM");
			
			JSONObject out = new JSONObject();
			Object[] ddivalues = new Object[ddis.length()];
			for (int i = 0; i < ddis.length(); ++i) {
				ddivalues[i] = T_DDI.inst(Integer.toString(ddis.getInt(i)));
			}
			
			Var GRAPH = Var.alloc("g"), DDI = Var.alloc("ddi"), DLV = Var.alloc("dlv"), TLG = Var.alloc("tlg"),
					DET = Var.alloc("det"), DVC = Var.alloc("dvc"), TIM = Var.alloc("tim"),
					VAL = Var.alloc("v"), START = Var.alloc("start"), STOP = Var.alloc("stop"), DUR = Var.alloc("dur");
			Literal dvcID = ResourceFactory.createTypedLiteral(clientName, XSDDatatype.XSDhexBinary);

			SelectBuilder query = new SelectBuilder()
					.addVar(GRAPH).addVar(DLV).addVar(DDI).addVar(VAL).addVar(START).addVar(STOP).addVar(DUR).addVar(TLG)
					.fromNamed(graphs)
					.addGraph(GRAPH, new WhereBuilder()
							.addWhere(DVC, device.prop("D"), dvcID)
							.addWhere(TIM, time.prop("A"), START)
							.addOptional(TIM, time.prop("B"), STOP)
							.addOptional(TIM, time.prop("C"), DUR)
							.addWhere(DLV, DCTerms.isPartOf, TIM)
							.addWhere(DLV, timelogvalue.prop("C"), DET)
							.addWhere(DET, DCTerms.isPartOf, DVC)
							.addWhere(DLV, timelogvalue.prop("A"), DDI)
							.addWhere(DLV, timelogvalue.prop("B"), VAL)
							.addOptional(DLV, PathFactory.pathSeq(Util.path(DCTerms.isPartOf), Util.path(TripleFunctions.TIMELOG.prop("name"))), TLG))
					.addValueVar(DDI, ddivalues);
			TimeInterval filter = TimeInterval.from(timeFilter);
			try(QueryResult qr = application.triple.query(query.build())) {
				for(UtilQuerySolution qs : qr.iterate()) {
					String fileUri = qs.getUri(GRAPH);
					String tlgname = qs.getString(TLG);
					JSONObject vuris = getOrCreate(getOrCreate(out, fileUri), tlgname != null ? tlgname : "");
					
					Instant start = qs.getDateTime(START);
					Instant stop = qs.getDateTime(STOP);
					if(stop == null) {
						Literal dur = qs.getLiteral(DUR);
						if(dur != null)
							stop = start.plusSeconds(dur.getLong());
					}
					if(filter != null) {
						if(filter.isFrom() && stop.isBefore(filter.from())) continue;
						if(filter.isUntil() && start.isAfter(filter.until())) continue;
					}
					
					vuris.put(qs.getUri(DLV), new JSONObject()
							.put("start", isoUTC(start))
							.put("stop", isoUTC(stop))
							.put("value", qs.getLiteralValue(VAL))
							.put("ddi", qs.get(DDI).visitWith(DDI_INT)));
				}
			}
			
			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Find value uris by device.
	 *
	 * @param req the req
	 * @param clientName the client name
	 * @param timeFilter the time filter
	 * @param ddis the ddis
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject findValueUrisByDevice(HttpServletRequest req, String clientName, JSONObject timeFilter, JSONArray ddis) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("findValueUrisByDevice: user(%s) service(%s) clientname(%s) timeFilter(%s) ddis(%d)\n", 
					context.getUser(), context.getServiceName(), clientName, timeFilter, ddis.length());

			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getFileGraphs(null);
			
			WikiType device = T_ISOXML.res("DVC"), timelogvalue = T_ISOXML.res("DLV");
			
			Map<String, Map<String, Integer>> valueUris = new HashMap<>(ddis.length());
			Object[] ddivalues = new Object[ddis.length()];
			for (int i = 0; i < ddis.length(); ++i) {
				ddivalues[i] = T_DDI.inst(Integer.toString(ddis.getInt(i)));
			}
			
			Var GRAPH = Var.alloc("g"), DDI = Var.alloc("ddi"), DLV = Var.alloc("dlv"), DET = Var.alloc("det"), DVC = Var.alloc("dvc");
			Literal dvcID = ResourceFactory.createTypedLiteral(clientName, XSDDatatype.XSDhexBinary);

			Query query = new SelectBuilder()
					.addVar(GRAPH).addVar(DLV).addVar(DDI)
					.fromNamed(graphs)
					.addGraph(GRAPH, new WhereBuilder()
							.addWhere(DVC, device.prop("D"), dvcID)
							.addWhere(DLV, timelogvalue.prop("C"), DET)
							.addWhere(DET, DCTerms.isPartOf, DVC)
							.addWhere(DLV, timelogvalue.prop("A"), DDI))
					.addValueVar(DDI, ddivalues)
					.build();
			try(QueryResult qr = application.triple.query(query)) {
				for(UtilQuerySolution qs : qr.iterate()) {
					String fileUri = qs.getUri(GRAPH);
					Map<String, Integer> vuris = getOrCreate(valueUris, fileUri, () -> new HashMap<>(ddivalues.length));
					vuris.put(qs.getUri(DLV), (Integer)qs.get(DDI).visitWith(DDI_INT));
				}
			}
			
			TimeInterval filter = TimeInterval.from(timeFilter);
			JSONObject out = new JSONObject();
			for(Entry<String, Map<String, Integer>> e : valueUris.entrySet()) {
				JSONObject fileKey = getOrCreate(out, e.getKey());
				Map<String, TimelogInfo> tlInfos = filter != null ? new HashMap<>() : null;
				for(Key k : application.table.listTimelogKeys(new FileKey(context.getUser(), e.getKey()))) {
					Integer ddi = e.getValue().get(k.valueUri);
					if(ddi != null) {
						if(filter != null) {
							TimelogInfo info = tlInfos.get(k.name);
							if(info == null) tlInfos.put(k.name, info = application.table.getTimelogInfo(k));
							if(filter.isFrom() && info.until.isBefore(filter.from())) continue;
							if(filter.isUntil() && info.from.isAfter(filter.until())) continue;
						}
						getOrCreate(fileKey, k.name).put(k.valueUri, new JSONObject().put("ddi", ddi));
					}
				}
				if(fileKey.length() == 0)
					out.remove(e.getKey());
			}
			
			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * List time logs.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listTimeLogs(HttpServletRequest req, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("listTimeLogs: user(%s) service(%s) fileUri(%s)\n",
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			permissions.getGraphs(fileUri);
			
			JSONObject fileKey = new JSONObject();
			for(Key key : application.table.listTimelogKeys(new FileKey(context.getUser(), fileUri))) {
				getOrCreate(fileKey, key.name).put(key.valueUri, new JSONObject());
			}
			return new JSONObject().put(fileUri, fileKey.length() > 0 ? fileKey : null);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the time log.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param name the name
	 * @param valueUri the value uri
	 * @param timeFilter the time filter
	 * @param limit the limit
	 * @return the time log
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getTimeLog(HttpServletRequest req, String fileUri, String name, String valueUri, 
			JSONObject timeFilter, int limit) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getTimeLogs: user(%s) service(%s) fileUri(%s) name(%s) valueUri(%s) timeFilter(%s) limit(%d)\n",
					context.getUser(), context.getServiceName(), fileUri, name, valueUri, timeFilter, limit);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(valueUri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			TableFunctions.Key timelogKey = new TableFunctions.Key(context.getUser(), fileUri, name, valueUri);
			
			List<Timelog> timelogs = application.table.getTimelogs(timelogKey, TimeInterval.from(timeFilter), limit);

			JSONArray timelog = new JSONArray();
			for (Timelog tl : timelogs) {
				timelog.put(new JSONObject()
					.put("time", JsonRpcEndpoint.isoUTC(tl.time))
					.put("value", tl.value));
			}

			return new JSONObject().put("timelog", timelog);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the positions.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param timelog the timelog
	 * @param timeFilter the time filter
	 * @param limit the limit
	 * @return the positions
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getPositions(HttpServletRequest req, String fileUri, String timelog, JSONObject timeFilter, int limit) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getPositions: user(%s) service(%s) fileUri(%s) timelog(%s) timeFilter(%s) limit(%d)\n",
					context.getUser(), context.getServiceName(), fileUri, timelog, timeFilter, limit);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			permissions.getGraphs(fileUri);
			if(permissions.isGPSAllowed())
				throw ACCESS_DENIED;
			
			ElementKey positionKey = new ElementKey(context.getUser(), fileUri, timelog);

			JSONArray positions = new JSONArray();
			for (TimelogPosition pos : application.table.getPositions(positionKey, TimeInterval.from(timeFilter), limit)) {
				positions.put(new JSONObject()
					.put("time", JsonRpcEndpoint.isoUTC(pos.time))
					.put("latitude", pos.pos.y)
					.put("longitude", pos.pos.x)
					.put("altitude", Double.isNaN(pos.pos.getZ()) ? null : pos.pos.getZ()));
			}

			return new JSONObject().put("positions", positions);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the value info.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param valueUri the value uri
	 * @return the value info
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getValueInfo(HttpServletRequest req, String fileUri, String valueUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getValueInfo: user(%s) service(%s) fileUri(%s) valueUri(%s)\n",
					context.getUser(), context.getServiceName(), fileUri, valueUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node accessUri = NodeFactory.createURI(valueUri);
			if(permissions.filterAllowed(graphs, accessUri).isEmpty())
				throw ACCESS_DENIED;
			
			ValueInfo info = application.table.getValueInfo(fileUri, valueUri);
			JSONObject out = new JSONObject().put("valueUri", valueUri);
			if(!info.designator.isEmpty()) out.put("designator", info.designator);
			if(info.offset != 0) out.put("offset", info.offset);
			if(info.scale != 1.) out.put("scale", info.scale);
			if(info.numberOfDecimals != 0) out.put("numberOfDecimals", info.numberOfDecimals);
			if(!info.unit.isEmpty()) out.put("unit", info.unit);
			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Prints the file.
	 *
	 * @param file the file
	 * @return the JSON object
	 */
	private static JSONObject printFile(File file) {
		return new JSONObject()
				.put("uri", file.getURI())
				.put("filename", file.getFilename())
				.put("source", file.getSource())
				.put("created", file.getCreated() != null ? isoUTC(file.getCreated()) : null)
				.put("lastmodified", file.getModified() != null ? isoUTC(file.getModified()) : null)
				.put("leveraged", file.getLeveraged() != null ? isoUTC(file.getLeveraged()) : null)
				.put("expires", file.getExpires() != null ? isoUTC(file.getExpires()) : null)
				.put("size", file.getSize())
				.put("type", file.getType());
	}
	
	/**
	 * List files.
	 *
	 * @param req the req
	 * @param sdsdtype the sdsdtype
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listFiles(HttpServletRequest req, String sdsdtype) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("listFiles: user(%s) service(%s) sdsdtype(%s)\n",
					context.getUser(), context.getServiceName(), sdsdtype);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;

			List<File> files = permissions.getFiles(File.filterType(sdsdtype));
			return new JSONObject().put("files", files.stream()
					.map(ApiEndpoint::printFile)
					.collect(Util.toJSONArray()));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the file.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @return the file
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getFile(HttpServletRequest req, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getFile: user(%s) service(%s) fileUri(%s)\n",
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;

			
			List<File> files = permissions.getFiles(fileUri);
			if(files.size() == 0)
				throw new SDSDException("File not found: " + fileUri);
			File file = files.get(0);

			byte[] content = application.file.downloadFile(user, file);
			
			return printFile(file).put("content", Base64.getEncoder().encodeToString(content));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the new data listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param identifier the identifier
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setNewDataListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("setNewDataListener: user(%s) service(%s)\n", 
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;
			
			application.file.dataAdded.setListener(user, 
					conn.listener("api", "newData", context.getToken(), identifier, ApiEndpoint::printFile));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset new data listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param identifier the identifier
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetNewDataListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("unsetNewDataListener: user(%s) service(%s)\n", 
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			application.file.dataAdded.unsetListener(user, conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the new file listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param identifier the identifier
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setNewFileListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("setNewFileListener: user(%s) service(%s)\n", 
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;
			
			application.list.files.setListener(user, 
					conn.listener("api", "newFile", context.getToken(), identifier, ApiEndpoint::printFile));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset new file listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param identifier the identifier
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetNewFileListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("unsetNewFileListener: user(%s) service(%s)\n", 
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			application.list.files.unsetListener(user, conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the file appended listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param fileUri the file uri
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setFileAppendedListener(HttpServletRequest req, WebsocketConnection conn, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("setFileAppendedListener: user(%s) service(%s) fileUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;
			
			List<File> files = permissions.getFiles(fileUri);
			if(files.size() == 0)
				throw new SDSDException("File not found: " + fileUri);
			File file = files.get(0);
			
			application.file.fileAppended.setListener(file, conn.listener("api", "fileAppended", context.getToken(), fileUri, 
					content -> printFile(file).put("appended", Base64.getEncoder().encodeToString(content))));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset file appended listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param fileUri the file uri
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetFileAppendedListener(HttpServletRequest req, WebsocketConnection conn, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("unsetFileAppendedListener: user(%s) service(%s) fileUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			File file = application.list.files.get(user, File.toID(fileUri));
			if(file.getSize() == 0)
				throw new SDSDException("File not found: " + fileUri);
			
			application.file.fileAppended.unsetListener(file, conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Sets the file deleted listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param fileUri the file uri
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setFileDeletedListener(HttpServletRequest req, WebsocketConnection conn, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("setFileDeletedListener: user(%s) service(%s) fileUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			if(!permissions.isFileAllowed())
				throw ACCESS_DENIED;
			
			List<File> files = permissions.getFiles(fileUri);
			if(files.size() == 0)
				throw new SDSDException("File not found: " + fileUri);
			File file = files.get(0);
			
			application.file.fileDeleted.setListener(user, 
					conn.listener("api", "fileDeleted", context.getToken(), file.getURI(), ApiEndpoint::printFile));
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Unset file deleted listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param fileUri the file uri
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unsetFileDeletedListener(HttpServletRequest req, WebsocketConnection conn, String fileUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("unsetFileDeletedListener: user(%s) service(%s) fileUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");
			
			File file = application.list.files.get(user, File.toID(fileUri));
			if(file.getSize() == 0)
				throw new SDSDException("File not found: " + fileUri);
			
			application.file.fileDeleted.unsetListener(user, conn.observerId());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Send file.
	 *
	 * @param req the req
	 * @param filename the filename
	 * @param content the content
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject sendFile(HttpServletRequest req, String filename, String content) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("sendFile: user(%s) service(%s) filename(%s)\n",
					context.getUser(), context.getServiceName(), filename);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			File file = application.file.storeFile(user, filename, Base64.getDecoder().decode(content), 
					Instant.now(), context.getServiceName(), null);
			if(file == null)
				throw new SDSDException(filename + ": No storage task for this resolt from service " + context.getServiceName());
			application.logInfo(user, "Service result \"%s\" (%s) from %s", file.getFilename(), 
					FileUtils.byteCountToDisplaySize(file.getSize()), context.getServiceName());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Append file.
	 *
	 * @param req the req
	 * @param filename the filename
	 * @param newContent the new content
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject appendFile(HttpServletRequest req, String filename, String newContent) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("appendFile: user(%s) service(%s) filename(%s)\n",
					context.getUser(), context.getServiceName(), filename);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			List<File> files = application.list.files.get(user, 
					Filters.and(File.filterSource(context.getServiceName()), File.filter(filename)));
			File file;
			if(files.isEmpty()) {
				file = application.file.storeFile(user, filename, Base64.getDecoder().decode(newContent), 
						Instant.now(), context.getServiceName(), null);
				if(file == null)
					throw new SDSDException(filename + ": No storage task for this resolt from service " + context.getServiceName());
			} else {
				file = files.stream().max((f1, f2) -> f1.getModified().compareTo(f2.getModified())).get(); 
				if(!application.file.appendFile(user, file, Base64.getDecoder().decode(newContent)))
					throw new SDSDException(filename + ": Couldn't write to file");
			}
			
			application.logInfo(user, "Service result \"%s\" (%s) from %s appended", file.getFilename(), 
					FileUtils.byteCountToDisplaySize(file.getSize()), context.getServiceName());
			
			return success(true);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Find geometry.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param uri the uri
	 * @param elementtype the elementtype
	 * @param geotype the geotype
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject findGeometry(HttpServletRequest req, 
			@Nullable String fileUri, @Nullable String uri, @Nullable String elementtype, @Nullable String geotype) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("findGeometry: user(%s) service(%s) fileUri(%s) uri(%s) elementtype(%s) geotype(%s)\n",
					context.getUser(), context.getServiceName(), fileUri, uri, elementtype, geotype);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<File> files = permissions.getFiles(fileUri);
			
			if(uri != null) {
				Node accessUri = NodeFactory.createURI(uri);
				List<String> graphs = new ArrayList<>(files.size() + 2);
				graphs.add(user.getGraphUri());
				graphs.add(TripleFunctions.TBOX);
				for(File f : files) {
					graphs.add(f.getURI());
				}
				if(permissions.filterAllowed(graphs, accessUri).isEmpty())
					throw ACCESS_DENIED;
			}
			//TODO: check geo access

			ArrayList<Bson> filter = new ArrayList<>(files.size() + 3);
			files.forEach(f -> filter.add(GeoElement.filterFile(f.getId())));
			if(uri != null)
				filter.add(GeoElement.filterUri(uri));
			if(elementtype != null)
				filter.add(GeoElement.filterType(ElementType.valueOf(elementtype)));
			if(geotype != null)
				filter.add(GeoElement.filterType(GeoType.valueOf(geotype)));
			
			List<GeoElement> geoElements = application.geo.find(user, filter.isEmpty() ? null : Filters.and(filter));
			
			return new JSONObject()
					.put("type", "FeatureCollection")
					.put("features", geoElements.stream()
							.map(GeoElement::getFeatureJson)
							.collect(Util.toJSONArray()));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Geo intersects.
	 *
	 * @param req the req
	 * @param geometry the geometry
	 * @param elementtype the elementtype
	 * @param geotype the geotype
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject geoIntersects(HttpServletRequest req, JSONObject geometry, @Nullable String elementtype, @Nullable String geotype) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("geoIntersects: user(%s) service(%s) geometry(%s)\n",
					context.getUser(), context.getServiceName(), geometry.getString("type"));
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<File> files = permissions.getFiles();
			
			//TODO: check geo access
			
			ArrayList<Bson> filter = new ArrayList<>(files.size() + 3);
			files.forEach(f -> filter.add(GeoElement.filterFile(f.getId())));
			filter.add(GeoElement.filterIntersects(user, geometry));
			try {
				if(elementtype != null)
					filter.add(GeoElement.filterType(ElementType.valueOf(elementtype)));
			} catch (IllegalArgumentException e) {
				throw new SDSDException("unknown elementtype " + elementtype);
			}
			try {
				if(geotype != null)
					filter.add(GeoElement.filterType(GeoType.valueOf(geotype)));
			} catch (IllegalArgumentException e) {
				throw new SDSDException("unknown geotype " + geotype);
			}

			JSONArray features = new JSONArray();
			for(GeoElement geo : application.geo.find(user, Filters.and(filter))) {
				features.put(geo.getFeatureJson().put("file", geo.getFile().toHexString()).put("uri", geo.getUri()));
			}
			
			return new JSONObject()
					.put("type", "FeatureCollection")
					.put("features", features);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the device element properties.
	 *
	 * @param req the req
	 * @param fileUri the file uri
	 * @param detUri the det uri
	 * @return the device element properties
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getDeviceElementProperties(HttpServletRequest req, @Nullable String fileUri, String detUri) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getObject: user(%s) service(%s) fileUri(%s) detUri(%s)\n", 
					context.getUser(), context.getServiceName(), fileUri, detUri);
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Permissions permissions = context.getPermissions(application);
			List<String> graphs = permissions.getGraphs(fileUri);
			
			Node DET = NodeFactory.createURI(detUri);
			if(permissions.filterAllowed(graphs, DET).isEmpty())
				throw ACCESS_DENIED;
			
			WikiType T_DPT = T_ISOXML.res("DPT"), T_DVP = T_ISOXML.res("DVP");
					
			
			Var DOR=Var.alloc("dor"), DPT=Var.alloc("dpt"), DVP=Var.alloc("dvp"),
					DDI=Var.alloc("ddi"), LABEL=Var.alloc("label"), VALUE=Var.alloc("value"),
					UNIT=Var.alloc("unit"), OFFSET=Var.alloc("offset"), SCALE=Var.alloc("scale"), DECIMALS=Var.alloc("decimals");
			SelectBuilder sb = new SelectBuilder()
					.addVar(DDI).addVar(LABEL).addVar(VALUE).addVar(DPT)
					.addVar(UNIT).addVar(OFFSET).addVar(SCALE).addVar(DECIMALS)
					.addWhere(DET, RDF.type, T_ISOXML.res("DET"))
					.addWhere(DOR, DCTerms.isPartOf, DET)
					.addWhere(DOR, T_ISOXML.res("DOR").prop("A"), DPT)
					.addWhere(DPT, RDF.type, T_DPT)
					.addWhere(DPT, T_DPT.prop("B"), DDI)
					.addWhere(DPT, RDFS.label, LABEL)
					.addWhere(DPT, T_DPT.prop("C"), VALUE)
					.addOptional(new WhereBuilder()
							.addWhere(DPT, T_DPT.prop("E"), DVP)
							.addOptional(DVP, T_DVP.prop("B"), OFFSET)
							.addOptional(DVP, T_DVP.prop("C"), SCALE)
							.addOptional(DVP, T_DVP.prop("D"), DECIMALS)
							.addOptional(DVP, T_DVP.prop("E"), UNIT));
			if(fileUri != null) sb.from(fileUri);
			else sb.from(graphs);

			try (QueryResult qr = application.triple.query(sb.build())) {
				JSONObject out = new JSONObject();
				for(UtilQuerySolution qs : qr.iterate()) {
					int ddi = qs.get(DDI, DDI_INT);
					out.put(String.format("%04X", ddi), new JSONObject()
							.put("ddi", ddi)
							.put("value", qs.getLiteral(VALUE).getInt())
							.put("valueUri", qs.getUri(DPT))
							.put("designator", qs.getString(LABEL))
							.put("offset", qs.getLiteralValue(OFFSET))
							.put("scale", qs.getLiteralValue(SCALE))
							.put("numberOfDecimals", qs.getLiteralValue(DECIMALS))
							.put("unit", qs.getString(UNIT)));
				}
				return out;
			}
		} catch (Throwable e) {
			throw createError(null, e);
		} 
	}
	
	/**
	 * Gets the all fields.
	 *
	 * @param req the req
	 * @return the all fields
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getAllFields(HttpServletRequest req) throws JsonRpcException {
		try {
			ServiceInstance context = getContext(req);
			System.out.format("getAllFields: user(%s) service(%s)\n",
					context.getUser(), context.getServiceName());
			
			User user = application.user.getUser(context.getUser());
			if(user == null) throw new SDSDException("Service instance belongs to an unknown user");

			Resource accessType = TripleFunctions.FORMAT_UNKNOWN.res("field");
			Permissions permissions = context.getPermissions(application);
			if(!permissions.isAllowed(accessType.getURI()))
				throw ACCESS_DENIED;

			List<String> graphs = permissions.getFiles().stream()
					.sorted(File.CMP_RECENT_CORE)
					.map(File::getURI)
					.collect(Collectors.toList());
			
			Map<String, DeduplicatedResource> fields = application.triple.getAll(user, accessType, graphs, 
					permissions.getPermissionObjects(accessType.getURI()));
			
			List<GeoElement> geos = application.geo.find(user, Filters.and(
					Filters.in(GeoElement.URI, fields.keySet()), 
					GeoElement.filterType(ElementType.Field)));
			JSONArray list = new JSONArray();
			for(GeoElement geo : geos) {
				DeduplicatedResource dRes = fields.get(geo.getUri());
				if(dRes == null) continue;
				Set<String> labels = dRes.getLabels();
				if(geo.getLabel() != null && !geo.getLabel().isEmpty())
					labels.add(geo.getLabel());
				
				JSONObject props = new JSONObject()
						.put("uri", dRes.getUris())
						.put("graph", dRes.getGraphs())
						.put("label", labels)
						.put("area", Double.isFinite(geo.getArea()) ? geo.getArea() : null);
				
				list.put(geo.getFeatureJson()
						.put("properties", props));
			}

			return new JSONObject().put("fields", list);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Gets the wikinormia instances.
	 *
	 * @param type the type
	 * @return the wikinormia instances
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getWikinormiaInstances(String type) throws JsonRpcException {
		try {
			System.out.format("getWikinormiaInstances: type(%s)\n", type);
			
			List<WikiEntry> instances = application.wiki.getInstances(ResourceFactory.createResource(type), true, Sorting.NONE, 0, 0);
			JSONObject out = new JSONObject();
			for(WikiEntry inst : instances) {
				out.put(inst.getIdentifier(), new JSONObject()
						.put("uri", inst.getUri())
						.put("label", inst.getLabel()));
			}
			return out;
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}

}
