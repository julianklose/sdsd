package de.sdsd.projekt.prototype.jsonrpc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.ext.com.google.common.collect.Sets.SetView;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.Permissions;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.Service;
import de.sdsd.projekt.prototype.data.ServiceInstance;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * JSONRPC-Endpoint for service functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ServiceEndpoint extends JsonRpcEndpoint {

	public ServiceEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	public JSONObject listServices(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listServices: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = new JSONArray();
				for (Service service : application.service.listServices(Service.filterVisible(true))) {
					JSONObject obj = new JSONObject()
							.put("id", service.getId().toHexString())
							.put("name", service.getName())
							.put("author", service.getAuthor())
							.put("added", isoUTC(service.getAdded()));
					array.put(obj);
				}
				return new JSONObject().put("services", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject listMyServices(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listMyServices: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = new JSONArray();
				for (Service service : application.service.listServices(Service.filterAuthor(user))) {
					JSONObject obj = new JSONObject()
							.put("id", service.getId().toHexString())
							.put("token", service.getToken())
							.put("name", service.getName())
							.put("added", isoUTC(service.getAdded()))
							.put("visible", service.isVisible());
					array.put(obj);
				}
				return new JSONObject().put("myServices", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject listMyInstances(HttpServletRequest req, String serviceId) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listMyInstances: user(" + (user != null ? user.getName() : "none") + ") service(" + serviceId + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Optional<Service> service = application.service.getService(Service.filter(new ObjectId(serviceId)));
				if(!service.isPresent())
					throw new SDSDException("Service " + serviceId + " doesn't exist");
				JSONArray array = application.service.listInstances(ServiceInstance.filterService(service.get())).stream()
						.map(serviceInstance -> new JSONObject()
								.put("id", serviceInstance.getId().toHexString())
								.put("token", serviceInstance.getToken())
								.put("user", serviceInstance.getUser())
								.put("activated", isoUTC(serviceInstance.getActivated()))
								.put("completed", serviceInstance.getCompleted() != null ? isoUTC(serviceInstance.getCompleted()) : null)
								.put("error", serviceInstance.getError())
								.put("parameter", serviceInstance.getParameter()))
						.collect(Util.toJSONArray());
				return new JSONObject().put("name", service.get().getName()).put("list", array);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject listActiveServices(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listActiveServices: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = new JSONArray();
				Bson filter = Filters.and(ServiceInstance.filter(user), ServiceInstance.filterCompleted(false));
				for (ServiceInstance serviceInstance : application.service.listInstances(filter)) {
					JSONObject obj = new JSONObject()
							.put("id", serviceInstance.getId().toHexString())
							.put("serviceId", serviceInstance.getServiceId().toHexString())
							.put("name", serviceInstance.getServiceName())
							.put("activated", isoUTC(serviceInstance.getActivated()))
							.put("error", serviceInstance.getError());
					array.put(obj);
				}
				return new JSONObject().put("activeServices", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject listCompletedServices(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listCompletedServices: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = new JSONArray();
				Bson filter = Filters.and(ServiceInstance.filter(user), ServiceInstance.filterCompleted(true));
				for (ServiceInstance serviceInstance : application.service.listInstances(filter)) {
					Instant completed = serviceInstance.getCompleted();
					JSONObject obj = new JSONObject()
							.put("id", serviceInstance.getId().toHexString())
							.put("serviceId", serviceInstance.getServiceId().toHexString())
							.put("name", serviceInstance.getServiceName())
							.put("activated", isoUTC(serviceInstance.getActivated()))
							.put("completed", completed != null ? isoUTC(completed) : null)
							.put("error", serviceInstance.getError());
					array.put(obj);
				}
				return new JSONObject().put("completedServices", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject getInstance(HttpServletRequest req, String instanceId) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getInstance: user(" + (user != null ? user.getName() : "none") + ") instance(" + instanceId + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ServiceInstance instance = application.service.getInstance(user, instanceId);
				Service service = application.service.getService(instance);
				
				Instant completed = instance.getCompleted();
				JSONObject obj = new JSONObject()
						.put("serviceId", instance.getServiceId().toHexString())
						.put("name", instance.getServiceName())
						.put("activated", isoUTC(instance.getActivated()))
						.put("completed", completed != null ? isoUTC(completed) : null)
						.put("parameter", instance.getParameter())
						.put("error", instance.getError());
				if(instance.getResult() != null) {
					obj.put("result", new JSONObject()
							.put("value", "/")
							.put("label", instance.getResult()));
				}

				if(instance.getParameter() == null && completed == null) {
					JSONArray parameter = service.getParameter();

					for(int i = 0; i < parameter.length(); ++i) {
						JSONObject param = parameter.getJSONObject(i);
						String type = param.getString("type");
						if(type.equals("file")) {
							param.put("options", getFileOptions(application.list.files.getList(user)));
						}
						else if(type.equals("element") && param.has("uri")) {
							List<File> files = application.list.files.getList(user);
							param.put("options", getTripleOptions(files, param.getString("uri")));
						}
					}
					
					obj.put("neededParmeters", parameter);
				}
				
				if(completed == null) {
					Permissions permissions = instance.getPermissions(application);
					obj.put("permissions", permissions.getPermissions().stream()
							.map(per -> new JSONObject()
								.put("id", per.getId())
								.put("type", per.getType())
								.put("name", per.getName())
								.put("description", per.getDescription())
								.put("allow", per.isAllowed())
								.put("objs", per.getObjects()))
							.collect(Util.toJSONArray()));
					obj.put("timePermission", new JSONObject()
							.put("from", instance.getPermissionFrom() == null ? null : isoUTC(instance.getPermissionFrom()))
							.put("until", instance.getPermissionUntil() == null ? null :isoUTC(instance.getPermissionUntil())));
				}
				
				return new JSONObject().put("serviceInstance", obj);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject getPermissionOptions(HttpServletRequest req, String typeuri) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getPermissionOptions: user(" + (user != null ? user.getName() : "none") 
					+ ") typeuri(" + typeuri + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray options;
				if(Permissions.GPS.equals(typeuri)) {
					options = new JSONArray();
				}
				else if(Permissions.FILE.equals(typeuri)) {
					List<File> files = application.list.files.getList(user);
					options = getFileOptions(files);
				}
				else {
					List<File> files = application.list.files.getList(user);
					options = getTripleOptions(files, typeuri);
				}
				
				return new JSONObject().put("options", options);
			}
		} catch (JSONException | NumberFormatException | DateTimeParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	private static final Var OBJ = Var.alloc("obj"), GRAPH = Var.alloc("graph"), LABEL = Var.alloc("label"), TYPE = Var.alloc("type");
	
	private JSONArray getTripleOptions(List<File> files, String typeuri) {
		if(files.isEmpty()) return new JSONArray();
		Groups options = new Groups(files.size());
		files.sort(File.CMP_RECENT);
		for(File f : files) {
			options.addGroup(f.getURI(), f.getFilename());
		}
		options.addGroup(TripleFunctions.TBOX, "Wikinormia");
		
		SelectBuilder query = new SelectBuilder()
				.addVar(GRAPH).addVar(OBJ).addVar(LABEL).setDistinct(true)
				.fromNamed(files.stream().map(File::getURI).collect(Collectors.toSet()))
				.fromNamed(TripleFunctions.TBOX)
				.addGraph(TripleFunctions.TBOX_N, TYPE, TripleFunctions.SUBCLASS, NodeFactory.createURI(typeuri))
				.addGraph(GRAPH, new WhereBuilder()
						.addWhere(OBJ, PathFactory.pathAlt(Util.path(RDF.type), Util.path(DCTerms.isPartOf)), TYPE)
						.addOptional(OBJ, RDFS.label, LABEL))
				.addOrderBy(LABEL);
		try(QueryResult qr = application.triple.query(query.build())) {
			for(UtilQuerySolution qs : qr.iterate()) {
				options.addItem(qs.getUri(GRAPH), qs.getUri(OBJ), qs.getString(LABEL));
			}
		}
		
		return options.toJson();
	}
	
	private JSONArray getFileOptions(List<File> files) {
		if(files.isEmpty()) return new JSONArray();
		List<SDSDType> types = application.list.types.getList(null);
		Groups options = new Groups(types.size());
		types.forEach(t -> options.addGroup(t.getUri(), t.getName()));
		files.stream()
				.sorted(File.CMP_RECENT)
				.forEachOrdered(f -> options.addItem(f.getType(), f.getURI(), f.getFilename()));
		
		return options.toJson();
	}
	
	public JSONObject setInstanceParameter(HttpServletRequest req, String instanceId, JSONObject values) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("setInstanceParameter: user(" + (user != null ? user.getName() : "none") 
					+ ") serviceInstance(" + instanceId + ") parameter(" + values.length() + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ServiceInstance instance = application.service.getInstance(user, instanceId);
				Service service = application.service.getService(instance);
				JSONArray parameter = service.getParameter();
				
				if(values.length() != parameter.length())
					throw new SDSDException("Parameter count doesn't match request parameters");
				
				for(int i = 0; i < parameter.length(); ++i) {
					JSONObject param = parameter.getJSONObject(i);
					String name = param.getString("name");
					
					switch(param.getString("type")) {
					case "file":
					case "element": {
						int min = param.optInt("min", 1);
						int max = param.optInt("max", min > 1 ? Integer.MAX_VALUE : 1);
						if(min > 1 || max > 1) {
							JSONArray val = values.optJSONArray(name);
							if(min > 0 && val == null || val != null && val.length() < min)
								throw new SDSDException(String.format("Parameter %s has not enough values (at least %d)", 
										param.getString("label"), min));
							if(val != null && val.length() > max)
								throw new SDSDException(String.format("Parameter %s has too many values (at most %d)", 
										param.getString("label"), max));
						} else {
							String val = values.optString(name);
							if(min > 0 && val == null)
								throw new SDSDException(String.format("Parameter %s has no value", 
										param.getString("label")));
						}
						break;
					}
					case "number": {
						BigDecimal val = values.getBigDecimal(name);
						BigDecimal min = param.optBigDecimal("min", null);
						BigDecimal max = param.optBigDecimal("max", null);
						BigDecimal step = param.optBigDecimal("step", null);
						if(step != null && (min != null ? val.subtract(min) : val).remainder(step).compareTo(BigDecimal.ZERO) != 0)
							throw new SDSDException(String.format("Parameter %s is no multiple of %s", 
									param.getString("label"), step.toString()));
						if(min != null && val.compareTo(min) < 0 || max != null && val.compareTo(max) > 0) 
							throw new SDSDException(String.format("Parameter %s out of range [%s,%s]", 
									param.getString("label"), (min != null ? min.toString() : " "), (max != null ? max.toString() : " ")));
						break;
					}
					case "datetime": {
						Instant val = Instant.parse(values.getString(name));
						String from = param.optString("from");
						String until = param.optString("until");
						if(!from.isEmpty() && val.isBefore(Instant.parse(from)) || !until.isEmpty() && val.isAfter(Instant.parse(until)))
							throw new SDSDException(String.format("Parameter %s out of range [%s,%s]", 
									param.getString("label"), (!from.isEmpty() ? from : " "), (!until.isEmpty() ? until : " ")));
						break;
					}
					default: // "string"
					}
				}

				if(application.service.updateInstance(instance, instance.setParameter(values))) {
					if(instance.isReadyToStart())
						application.service.instanceChanged.trigger(service, instance);
					return success(true);
				}
				return success(false);
			}
		} catch (JSONException | NumberFormatException | DateTimeParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setInstancePermissions(HttpServletRequest req, String instanceId, JSONArray perms, JSONObject timePermission) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("setInstancePermissions: user(" + (user != null ? user.getName() : "none") 
					+ ") serviceInstance(" + instanceId + ") permissions(" + perms.length() + ") timePermission(" + timePermission.toString() + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ServiceInstance instance = application.service.getInstance(user, instanceId);
				Service service = application.service.getService(instance);
				Permissions permissions = instance.getPermissions(application);
				for(int i = 0; i < perms.length(); ++i) {
					JSONObject p = perms.getJSONObject(i);
					JSONArray objects = p.getJSONArray("objs");
					Set<String> objs = new HashSet<>(objects.length());
					for (int j = 0; j < objects.length(); ++j) {
						objs.add(objects.getString(j));
					}
					permissions.setPermission(p.getString("id"), p.getBoolean("allow"), objs);
				}
				
				String from = timePermission.optString("from"), until = timePermission.optString("until");
				Bson update = instance.setTimePermission(from.isEmpty() ? null : Instant.parse(from), until.isEmpty() ? null : Instant.parse(until));
				
				if(application.service.updateInstance(instance, update)) {
					if(instance.isReadyToStart())
						application.service.instanceChanged.trigger(service, instance);
					return success(true);
				}
				return success(false);
			}
		} catch (JSONException | NumberFormatException | DateTimeParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject activateService(HttpServletRequest req, String serviceId) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("activateService: user(" + (user != null ? user.getName() : "none") + ") service(" + serviceId + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Service service = application.service.getService(Service.filter(new ObjectId(serviceId)))
						.orElseThrow(() -> new SDSDException("The service " + serviceId + " doesn't exist"));
				application.service.activateService(user, service);
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject errorInstance(String token, String error) throws JsonRpcException {
		try {
			Optional<ServiceInstance> context = application.service.getInstance(ServiceInstance.filterToken(token));
			System.out.println("errorInstance: serviceInstance(" + (context.isPresent() ? context.get().getServiceId().toHexString() : "null") 
					+ ") error(" + error + ")");
			
			if (context.isPresent()) 
				return success(application.service.updateInstance(context.get(), context.get().setError(error)));
			else
				throw new SDSDException("No active context for the given token");
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject completeInstance(String token, String result) throws JsonRpcException {
		try {
			Optional<ServiceInstance> context = application.service.getInstance(ServiceInstance.filterToken(token));
			System.out.println("completeInstance: serviceInstance(" + (context.isPresent() ? context.get().getServiceId().toHexString() : "null") + ")");
			
			if (context.isPresent()) 
				return success(application.service.updateInstance(context.get(), context.get().setCompleted(result)));
			else
				throw new SDSDException("No active context for the given token");
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject deleteInstance(HttpServletRequest req, String instanceId) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("deleteInstance: user(" + (user != null ? user.getName() : "none") + ") serviceInstance(" + instanceId + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ServiceInstance instance = application.service.getInstance(user, instanceId);
				return success(application.service.deleteInstance(instance));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject createService(HttpServletRequest req, JSONObject input) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			String name = input.optString("name");
			System.out.println("createService: user(" + (user != null ? user.getName() : "none") + ") name(" + name + ")");
			
			if(name == null || name.isEmpty()) 
				throw new SDSDException("The service name must not be empty");
			
			JSONArray params = input.optJSONArray("parameter");
			if(params != null) {
				Set<String> pnames = new HashSet<String>(params.length());
				for(int i = 0; i < params.length(); ++i) {
					JSONObject param = params.getJSONObject(i);
					String pname = param.getString("name");
					if(pname.isEmpty())
						throw new JSONException("parameter names must not be empty");
					if(!pnames.add(pname))
						throw new JSONException("parameter names must be unique");
					if(param.getString("label").isEmpty())
						throw new JSONException("labels must not be empty");
					switch(param.getString("type")) {
					case "element":
						if(param.getString("uri").isEmpty())
							throw new JSONException("Element parameter needs a type uri");
					case "file":
						if(param.has("min") && param.getInt("min") < 0) 
							throw new SDSDException("'min' must be a positive integer");
						if(param.has("max") && param.getInt("max") < 0) 
							throw new SDSDException("'max' must be a positive integer");
						break;
					case "string":
						break;
					case "number":
						if(param.has("min")) param.getBigDecimal("min");
						if(param.has("max")) param.getBigDecimal("max");
						if(param.has("step") && param.getBigDecimal("step").compareTo(BigDecimal.ZERO) == 0)
							throw new SDSDException("'step' must not be zero");
						break;
					case "datetime":
						if(!param.optString("from").isEmpty()) Instant.parse(param.getString("from"));
						if(!param.optString("until").isEmpty()) Instant.parse(param.getString("until"));
						break;
					default:
						throw new JSONException("Unknown parameter type \"" + param.getString("type") + "\"");
					}
				}
			}
			else 
				params = new JSONArray();
			
			JSONArray access = input.optJSONArray("access");
			Set<String> accessList = new HashSet<>();
			if(access != null) {
				for(int i = 0; i < access.length(); ++i) {
					accessList.add(access.getJSONObject(i).getString("uri"));
				}
			}

			final String URI="?uri";
			Query query = new SelectBuilder()
					.addVar(URI)
					.from(TripleFunctions.TBOX)
					.addWhere(URI, RDFS.label, Var.ANON)
					.addWhereValueVar(URI, accessList.stream().map(NodeFactory::createURI).toArray())
					.build();
			Set<String> accessUris;
			try(QueryResult qr = application.triple.query(query)) {
				accessUris = qr.stream()
						.map(qs -> qs.getResource(URI).getURI())
						.collect(Collectors.toSet());
			}
			
			SetView<String> diff = Sets.difference(accessList, accessUris);
			if(!diff.isEmpty())
				throw new SDSDException("Unknown access URIs: " + String.join(", ", diff));
			
			if (user == null) 
				throw new NoLoginException();
			else if(application.service.getService(Service.filterName(name)).isPresent())
				throw new SDSDException("Service with the given name already exists");
			else {
				application.service.addService(user, name, params, accessUris);
				return success(true);
			}
		} catch (JSONException | DateTimeParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setServiceVisible(HttpServletRequest req, String serviceId, boolean visible) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("setServiceVisible: user(" + (user != null ? user.getName() : "none") 
					+ ") service(" + serviceId + ") visible(" + (visible ? "true" : "false") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Bson filter = Filters.and(Service.filterAuthor(user), Service.filter(new ObjectId(serviceId)));
				Service service = application.service.getService(filter)
						.orElseThrow(() -> new SDSDException("Service with the given id doesn't exist"));
				return success(application.service.updateService(service, service.setVisible(visible)));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject deleteService(HttpServletRequest req, String serviceId) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("deleteService: user(" + (user != null ? user.getName() : "none") + ") service(" + serviceId + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Bson filter = Filters.and(Service.filterAuthor(user), Service.filter(new ObjectId(serviceId)));
				Service service = application.service.getService(filter)
						.orElseThrow(() -> new SDSDException("Service with the given id doesn't exist"));
				
				return success(application.service.deleteService(service));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

}
