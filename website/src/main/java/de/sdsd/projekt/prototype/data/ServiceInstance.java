package de.sdsd.projekt.prototype.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;

/**
 * Represents a service instance, stored in MongoDB.
 * 
 * @see Service
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ServiceInstance implements Comparable<ServiceInstance> {
	public static final String MISSING_PARAMETER_ERROR = "Missing parameter",
			MISSING_PERMISSION_ERROR = "Missing permission",
			MISSING_BOTH_ERROR = "Missing parameter and permission";
	
	public static final String ID = "_id", TOKEN = "token", SERVICE = "service", SERVICENAME = "servicename", 
			USER = "user", ACTIVATED = "activated", COMPLETED = "completed", PERM = "hasPermissions", 
			PERM_FROM = "permFrom", PERM_UNTIL = "permUntil",
			PARAMETER = "parameter", RESULT = "result", ERROR = "error";

	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static Bson filterService(Service service) {
		return Filters.eq(SERVICE, service.getId());
	}
	
	public static Bson filterToken(String token) {
		return Filters.eq(TOKEN, token);
	}
	
	public static Bson filterCompleted(boolean completed) {
		return Filters.exists(COMPLETED, completed);
	}
	
	public static Bson filterReady(Service service) {
		return Filters.and(filterService(service), filterCompleted(false), Filters.exists(PARAMETER), Filters.eq(PERM, true));
	}

	public static Document create(User user, Service service) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(TOKEN, Util.createSecureToken())
				.append(USER, user.getName())
				.append(SERVICE, service.getId())
				.append(SERVICENAME, service.getName())
				.append(ACTIVATED, Date.from(Instant.now()));
		if(service.getAccess().size() > 0 && service.getParameter().length() > 0)
			doc.append(ERROR, MISSING_BOTH_ERROR);
		else {
			if(service.getAccess().size() > 0)
				doc.append(ERROR, MISSING_PERMISSION_ERROR);
			else
				doc.append(PERM, true);
			if(service.getParameter().length() > 0)
				doc.append(ERROR, MISSING_PARAMETER_ERROR);
			else
				doc.append(PARAMETER, "{}");
		}
		return doc;
	}
	
	public static ServiceInstance getDefault(User user, ObjectId id) {
		return new ServiceInstance(id, user);
	}

	private final ObjectId id;
	private final String token;
	private final ObjectId serviceId;
	private final String serviceName;
	private final String user;
	private final Instant activated;
	private Instant completed;
	private JSONObject parameter;
	private boolean hasPermissions;
	private Instant permissionFrom, permissionUntil;
	private String error, result;

	public ServiceInstance(Document doc) {
		this.id = doc.getObjectId(ID);
		this.token = doc.getString(TOKEN);
		this.serviceId = doc.getObjectId(SERVICE);
		this.serviceName = doc.getString(SERVICENAME);
		this.user = doc.getString(USER);
		this.activated = doc.getDate(ACTIVATED).toInstant();
		Date date = doc.getDate(COMPLETED);
		this.completed = date != null ? date.toInstant() : null;
		String param = doc.getString(PARAMETER);
		this.parameter = param != null ? new JSONObject(param) : null;
		this.hasPermissions = doc.getBoolean(PERM, false);
		date = doc.getDate(PERM_FROM);
		this.permissionFrom = date != null ? date.toInstant() : null;
		date = doc.getDate(PERM_UNTIL);
		this.permissionUntil = date != null ? date.toInstant() : null;
		this.error = doc.getString(ERROR);
		this.result = doc.getString(RESULT);
	}
	
	protected ServiceInstance(ObjectId id, User user) {
		this.id = id;
		this.token = "";
		this.user = user.getName();
		this.serviceId = id;
		this.serviceName = "Unknown Service";
		this.activated = Instant.now();
		this.completed = null;
		this.parameter = null;
		this.hasPermissions = false;
		this.permissionFrom = null;
		this.permissionUntil = null;
		this.error = null;
		this.result = null;
	}
	
	public Bson filter() {
		return Filters.eq(id);
	}

	public ObjectId getId() {
		return id;
	}
	
	public String getToken() {
		return token;
	}

	public ObjectId getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public String getUser() {
		return user;
	}
	
	public Instant getActivated() {
		return activated;
	}

	@CheckForNull
	public Instant getCompleted() {
		return completed;
	}

	public Bson setCompleted(String result) {
		this.completed = Instant.now();
		this.result = result;
		return Updates.combine(
				Updates.set(COMPLETED, Date.from(completed)), 
				Updates.set(RESULT, result));
	}
	
	@CheckForNull
	public JSONObject getParameter() {
		return parameter;
	}
	
	public Bson setParameter(JSONObject parameter) {
		this.parameter = parameter;
		Bson set = Updates.set(PARAMETER, parameter.toString());
		Bson err = setError(hasPermissions ? null : MISSING_PERMISSION_ERROR);
		return Updates.combine(set, err);
	}
	
	public boolean hasPermissions() {
		return hasPermissions;
	}
	
	public Bson setHasPermissions(boolean hasPermissions) {
		this.hasPermissions = hasPermissions;
		Bson set = Updates.set(PERM, hasPermissions);
		Bson err = setError(parameter != null ? null : MISSING_PARAMETER_ERROR);
		return Updates.combine(set, err);
	}
	
	public boolean isReadyToStart() {
		return parameter != null && hasPermissions;
	}
	
	public boolean hasTimePermission() {
		return permissionFrom != null || permissionUntil != null;
	}
	
	@CheckForNull
	public Instant getPermissionFrom() {
		return permissionFrom;
	}
	
	@CheckForNull
	public Instant getPermissionUntil() {
		return permissionUntil;
	}
	
	public Bson setTimePermission(@Nullable Instant from, @Nullable Instant until) {
		List<Bson> updates = new ArrayList<>(3);
		updates.add(setHasPermissions(true));
		updates.add(from != null ? Updates.set(PERM_FROM, Date.from(from)) : Updates.unset(PERM_FROM));
		updates.add(until != null ? Updates.set(PERM_UNTIL, Date.from(until)) : Updates.unset(PERM_UNTIL));
		return Updates.combine(updates);
	}
	
	public Permissions getPermissions(ApplicationLogic app) {
		return new Permissions(app, this);
	}

	@CheckForNull
	public String getError() {
		return error;
	}

	public Bson setError(@Nullable String error) {
		if(error != null && error.isEmpty()) error = null;
		this.error = error;
		return error != null ? Updates.set(ERROR, error) : Updates.unset(ERROR);
	}

	@CheckForNull
	public String getResult() {
		return result;
	}

	@Override
	public int compareTo(ServiceInstance o) {
		if(completed != null)
			return o.completed != null ? -completed.compareTo(o.completed) : 1;
		else
			return -activated.compareTo(o.activated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ServiceInstance))
			return false;
		ServiceInstance other = (ServiceInstance) obj;
		return Objects.equals(id, other.id);
	}

}
