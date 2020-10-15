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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see Service
 */
public class ServiceInstance implements Comparable<ServiceInstance> {
	
	/** The Constant MISSING_BOTH_ERROR. */
	public static final String MISSING_PARAMETER_ERROR = "Missing parameter",
			MISSING_PERMISSION_ERROR = "Missing permission",
			MISSING_BOTH_ERROR = "Missing parameter and permission";
	
	/** The Constant ERROR. */
	public static final String ID = "_id", TOKEN = "token", SERVICE = "service", SERVICENAME = "servicename", 
			USER = "user", ACTIVATED = "activated", COMPLETED = "completed", PERM = "hasPermissions", 
			PERM_FROM = "permFrom", PERM_UNTIL = "permUntil",
			PARAMETER = "parameter", RESULT = "result", ERROR = "error";

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	/**
	 * Filter service.
	 *
	 * @param service the service
	 * @return the bson
	 */
	public static Bson filterService(Service service) {
		return Filters.eq(SERVICE, service.getId());
	}
	
	/**
	 * Filter token.
	 *
	 * @param token the token
	 * @return the bson
	 */
	public static Bson filterToken(String token) {
		return Filters.eq(TOKEN, token);
	}
	
	/**
	 * Filter completed.
	 *
	 * @param completed the completed
	 * @return the bson
	 */
	public static Bson filterCompleted(boolean completed) {
		return Filters.exists(COMPLETED, completed);
	}
	
	/**
	 * Filter ready.
	 *
	 * @param service the service
	 * @return the bson
	 */
	public static Bson filterReady(Service service) {
		return Filters.and(filterService(service), filterCompleted(false), Filters.exists(PARAMETER), Filters.eq(PERM, true));
	}

	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param service the service
	 * @return the document
	 */
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
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the default
	 */
	public static ServiceInstance getDefault(User user, ObjectId id) {
		return new ServiceInstance(id, user);
	}

	/** The id. */
	private final ObjectId id;
	
	/** The token. */
	private final String token;
	
	/** The service id. */
	private final ObjectId serviceId;
	
	/** The service name. */
	private final String serviceName;
	
	/** The user. */
	private final String user;
	
	/** The activated. */
	private final Instant activated;
	
	/** The completed. */
	private Instant completed;
	
	/** The parameter. */
	private JSONObject parameter;
	
	/** The has permissions. */
	private boolean hasPermissions;
	
	/** The permission until. */
	private Instant permissionFrom, permissionUntil;
	
	/** The result. */
	private String error, result;

	/**
	 * Instantiates a new service instance.
	 *
	 * @param doc the doc
	 */
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
	
	/**
	 * Instantiates a new service instance.
	 *
	 * @param id the id
	 * @param user the user
	 */
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
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(id);
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public ObjectId getId() {
		return id;
	}
	
	/**
	 * Gets the token.
	 *
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Gets the service id.
	 *
	 * @return the service id
	 */
	public ObjectId getServiceId() {
		return serviceId;
	}
	
	/**
	 * Gets the service name.
	 *
	 * @return the service name
	 */
	public String getServiceName() {
		return serviceName;
	}
	
	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * Gets the activated.
	 *
	 * @return the activated
	 */
	public Instant getActivated() {
		return activated;
	}

	/**
	 * Gets the completed.
	 *
	 * @return the completed
	 */
	@CheckForNull
	public Instant getCompleted() {
		return completed;
	}

	/**
	 * Sets the completed.
	 *
	 * @param result the result
	 * @return the bson
	 */
	public Bson setCompleted(String result) {
		this.completed = Instant.now();
		this.result = result;
		return Updates.combine(
				Updates.set(COMPLETED, Date.from(completed)), 
				Updates.set(RESULT, result));
	}
	
	/**
	 * Gets the parameter.
	 *
	 * @return the parameter
	 */
	@CheckForNull
	public JSONObject getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the parameter.
	 *
	 * @param parameter the parameter
	 * @return the bson
	 */
	public Bson setParameter(JSONObject parameter) {
		this.parameter = parameter;
		Bson set = Updates.set(PARAMETER, parameter.toString());
		Bson err = setError(hasPermissions ? null : MISSING_PERMISSION_ERROR);
		return Updates.combine(set, err);
	}
	
	/**
	 * Checks for permissions.
	 *
	 * @return true, if successful
	 */
	public boolean hasPermissions() {
		return hasPermissions;
	}
	
	/**
	 * Sets the has permissions.
	 *
	 * @param hasPermissions the has permissions
	 * @return the bson
	 */
	public Bson setHasPermissions(boolean hasPermissions) {
		this.hasPermissions = hasPermissions;
		Bson set = Updates.set(PERM, hasPermissions);
		Bson err = setError(parameter != null ? null : MISSING_PARAMETER_ERROR);
		return Updates.combine(set, err);
	}
	
	/**
	 * Checks if is ready to start.
	 *
	 * @return true, if is ready to start
	 */
	public boolean isReadyToStart() {
		return parameter != null && hasPermissions;
	}
	
	/**
	 * Checks for time permission.
	 *
	 * @return true, if successful
	 */
	public boolean hasTimePermission() {
		return permissionFrom != null || permissionUntil != null;
	}
	
	/**
	 * Gets the permission from.
	 *
	 * @return the permission from
	 */
	@CheckForNull
	public Instant getPermissionFrom() {
		return permissionFrom;
	}
	
	/**
	 * Gets the permission until.
	 *
	 * @return the permission until
	 */
	@CheckForNull
	public Instant getPermissionUntil() {
		return permissionUntil;
	}
	
	/**
	 * Sets the time permission.
	 *
	 * @param from the from
	 * @param until the until
	 * @return the bson
	 */
	public Bson setTimePermission(@Nullable Instant from, @Nullable Instant until) {
		List<Bson> updates = new ArrayList<>(3);
		updates.add(setHasPermissions(true));
		updates.add(from != null ? Updates.set(PERM_FROM, Date.from(from)) : Updates.unset(PERM_FROM));
		updates.add(until != null ? Updates.set(PERM_UNTIL, Date.from(until)) : Updates.unset(PERM_UNTIL));
		return Updates.combine(updates);
	}
	
	/**
	 * Gets the permissions.
	 *
	 * @param app the app
	 * @return the permissions
	 */
	public Permissions getPermissions(ApplicationLogic app) {
		return new Permissions(app, this);
	}

	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	@CheckForNull
	public String getError() {
		return error;
	}

	/**
	 * Sets the error.
	 *
	 * @param error the error
	 * @return the bson
	 */
	public Bson setError(@Nullable String error) {
		if(error != null && error.isEmpty()) error = null;
		this.error = error;
		return error != null ? Updates.set(ERROR, error) : Updates.unset(ERROR);
	}

	/**
	 * Gets the result.
	 *
	 * @return the result
	 */
	@CheckForNull
	public String getResult() {
		return result;
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(ServiceInstance o) {
		if(completed != null)
			return o.completed != null ? -completed.compareTo(o.completed) : 1;
		else
			return -activated.compareTo(o.activated);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
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
