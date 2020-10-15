package de.sdsd.projekt.prototype.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.AROnboarding.SecureOnboardingContext;
import de.sdsd.projekt.agrirouter.request.AREndpoint;

/**
 * Represents a user of the SDSD website, stored in MongoDB.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class User implements Comparable<User> {

	/** The Constant AR. */
	public static final String ID = "_id", NAME = "username", PW = "password", MAIL = "email", AR = "agrirouter";

	/**
	 * Filter.
	 *
	 * @param username the username
	 * @return the bson
	 */
	public static Bson filter(String username) {
		return Filters.eq(NAME, username);
	}

	/**
	 * Creates the.
	 *
	 * @param username the username
	 * @param password the password
	 * @param email the email
	 * @return the document
	 */
	public static Document create(String username, String password, String email) {
		return new Document()
				.append(ID, ObjectId.get())
				.append(NAME, username)
				.append(PW, BCrypt.hashpw(password, BCrypt.gensalt(12)))
				.append(MAIL, email);
	}
	
	/**
	 * To graph uri.
	 *
	 * @param username the username
	 * @return the string
	 */
	public static String toGraphUri(String username) {
		return "user:" + Util.toCamelCase(username, false);
	}

	/** The id. */
	public final ObjectId id;
	
	/** The username. */
	public final String username;
	
	/** The manager. */
	public final UserManager manager;
	
	/** The email. */
	private String email;
	
	/** The password. */
	private String password;
	
	/** The agrirouter. */
	@Nullable
	private ARConn agrirouter;
	
	/** The secure onboarding context. */
	@Nullable
	private SecureOnboardingContext secureOnboardingContext = null;
	
	/** The pending endpoints. */
	@Nullable
	private CompletableFuture<List<AREndpoint>> pendingEndpoints = null;
	
	/**
	 * Instantiates a new user.
	 *
	 * @param userDoc the user doc
	 * @param manager the manager
	 */
	User(Document userDoc, UserManager manager) {
		this.manager = manager;
		this.id = userDoc.getObjectId(ID);
		this.username = userDoc.getString(NAME);
		updateFromDB(userDoc);
	}
	
	/**
	 * Update from DB.
	 *
	 * @param userDoc the user doc
	 */
	void updateFromDB(Document userDoc) {
		this.email = userDoc.getString(MAIL);
		this.password = userDoc.getString(PW);
		Document arDoc = userDoc.get(AR, Document.class);
		this.agrirouter = arDoc != null ? new ARConn(arDoc, this) : null;
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(NAME, username);
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return username;
	}
	
	/**
	 * Gets the graph uri.
	 *
	 * @return the graph uri
	 */
	public String getGraphUri() {
		return toGraphUri(username);
	}
	
	/**
	 * Gets the created.
	 *
	 * @return the created
	 */
	public Instant getCreated() {
		return Instant.ofEpochSecond(id.getTimestamp());
	}

	/**
	 * Gets the email.
	 *
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email.
	 *
	 * @param email the email
	 * @return the bson
	 */
	public Bson setEmail(String email) {
		this.email = email;
		return Updates.set(MAIL, email);
	}

	/**
	 * Agrirouter.
	 *
	 * @return the AR conn
	 */
	@CheckForNull
	public ARConn agrirouter() {
		return agrirouter;
	}

	/**
	 * Sets the agrirouter.
	 *
	 * @param accountId the account id
	 * @param onboardingResponse the onboarding response
	 * @return the bson
	 * @throws JSONException the JSON exception
	 * @throws GeneralSecurityException the general security exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws MqttException the mqtt exception
	 */
	public Bson setAgrirouter(String accountId, JSONObject onboardingResponse)
			throws JSONException, GeneralSecurityException, IOException, MqttException {
		ARConnection arconn = manager.initARConnection(this, onboardingResponse);
		Document doc = ARConn.create(accountId, onboardingResponse.toString(), arconn);
		this.agrirouter = new ARConn(doc, this);
		return Updates.set(AR, doc);
	}

	/**
	 * Close agrirouter conn.
	 */
	public void closeAgrirouterConn() {
		manager.closeARConnection(this);
	}
	
	/**
	 * Clear agrirouter conn.
	 *
	 * @return the bson
	 */
	public Bson clearAgrirouterConn() {
		manager.closeARConnection(this);
		this.agrirouter = null;
		return Updates.unset(AR);
	}

	/**
	 * Gets the secure onboarding context.
	 *
	 * @return the secure onboarding context
	 */
	@Nullable
	public SecureOnboardingContext getSecureOnboardingContext() {
		return secureOnboardingContext;
	}

	/**
	 * Sets the secure onboarding context.
	 *
	 * @param secureOnboardingContext the new secure onboarding context
	 */
	public void setSecureOnboardingContext(@Nullable SecureOnboardingContext secureOnboardingContext) {
		this.secureOnboardingContext = secureOnboardingContext;
	}
	
	/**
	 * Check password.
	 *
	 * @param password the password
	 * @return true, if successful
	 */
	public boolean checkPassword(String password) {
		return BCrypt.checkpw(password, this.password);
	}
	
	/**
	 * Change password.
	 *
	 * @param oldpw the oldpw
	 * @param newpw the newpw
	 * @return the bson
	 */
	public Bson changePassword(String oldpw, String newpw) {
		if(BCrypt.checkpw(oldpw, this.password))
			return resetPassword(newpw);
		return null;
	}
	
	/**
	 * Reset password.
	 *
	 * @param newpw the newpw
	 * @return the bson
	 */
	public Bson resetPassword(String newpw) {
		this.password = BCrypt.hashpw(newpw, BCrypt.gensalt(12));
		return Updates.set(PW, this.password);
	}

	/**
	 * Gets the pending endpoints.
	 *
	 * @return the pending endpoints
	 */
	@Nullable
	public synchronized CompletableFuture<List<AREndpoint>> getPendingEndpoints() {
		return pendingEndpoints;
	}
	
	/**
	 * Sets the pending endpoints.
	 *
	 * @param future the new pending endpoints
	 */
	public synchronized void setPendingEndpoints(@Nullable final CompletableFuture<List<AREndpoint>> future) {
		if(future == null) return;
		this.pendingEndpoints = future;
		future.whenComplete((l, e) -> {
			if(e != null) {
				if(e instanceof CompletionException) e = e.getCause();
				System.err.println(username + " pending Endpoints: " + e.toString());
			}
			synchronized (User.this) {
				if(future == this.pendingEndpoints)
					this.pendingEndpoints = null;
			}
		});
	}
	
	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(User o) {
		return username.compareToIgnoreCase(o.username);
	}
}
