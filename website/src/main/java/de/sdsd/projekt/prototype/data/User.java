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

	public static final String ID = "_id", NAME = "username", PW = "password", MAIL = "email", AR = "agrirouter";

	public static Bson filter(String username) {
		return Filters.eq(NAME, username);
	}

	public static Document create(String username, String password, String email) {
		return new Document()
				.append(ID, ObjectId.get())
				.append(NAME, username)
				.append(PW, BCrypt.hashpw(password, BCrypt.gensalt(12)))
				.append(MAIL, email);
	}
	
	public static String toGraphUri(String username) {
		return "user:" + Util.toCamelCase(username, false);
	}

	public final ObjectId id;
	public final String username;
	public final UserManager manager;
	private String email;
	private String password;
	@Nullable
	private ARConn agrirouter;
	
	@Nullable
	private SecureOnboardingContext secureOnboardingContext = null;
	@Nullable
	private CompletableFuture<List<AREndpoint>> pendingEndpoints = null;
	
	User(Document userDoc, UserManager manager) {
		this.manager = manager;
		this.id = userDoc.getObjectId(ID);
		this.username = userDoc.getString(NAME);
		updateFromDB(userDoc);
	}
	
	void updateFromDB(Document userDoc) {
		this.email = userDoc.getString(MAIL);
		this.password = userDoc.getString(PW);
		Document arDoc = userDoc.get(AR, Document.class);
		this.agrirouter = arDoc != null ? new ARConn(arDoc, this) : null;
	}
	
	public Bson filter() {
		return Filters.eq(NAME, username);
	}

	public String getName() {
		return username;
	}
	
	public String getGraphUri() {
		return toGraphUri(username);
	}
	
	public Instant getCreated() {
		return Instant.ofEpochSecond(id.getTimestamp());
	}

	public String getEmail() {
		return email;
	}

	public Bson setEmail(String email) {
		this.email = email;
		return Updates.set(MAIL, email);
	}

	@CheckForNull
	public ARConn agrirouter() {
		return agrirouter;
	}

	public Bson setAgrirouter(String accountId, JSONObject onboardingResponse)
			throws JSONException, GeneralSecurityException, IOException, MqttException {
		ARConnection arconn = manager.initARConnection(this, onboardingResponse);
		Document doc = ARConn.create(accountId, onboardingResponse.toString(), arconn);
		this.agrirouter = new ARConn(doc, this);
		return Updates.set(AR, doc);
	}

	public void closeAgrirouterConn() {
		manager.closeARConnection(this);
	}
	
	public Bson clearAgrirouterConn() {
		manager.closeARConnection(this);
		this.agrirouter = null;
		return Updates.unset(AR);
	}

	@Nullable
	public SecureOnboardingContext getSecureOnboardingContext() {
		return secureOnboardingContext;
	}

	public void setSecureOnboardingContext(@Nullable SecureOnboardingContext secureOnboardingContext) {
		this.secureOnboardingContext = secureOnboardingContext;
	}
	
	public boolean checkPassword(String password) {
		return BCrypt.checkpw(password, this.password);
	}
	
	public Bson changePassword(String oldpw, String newpw) {
		if(BCrypt.checkpw(oldpw, this.password))
			return resetPassword(newpw);
		return null;
	}
	
	public Bson resetPassword(String newpw) {
		this.password = BCrypt.hashpw(newpw, BCrypt.gensalt(12));
		return Updates.set(PW, this.password);
	}

	@Nullable
	public synchronized CompletableFuture<List<AREndpoint>> getPendingEndpoints() {
		return pendingEndpoints;
	}
	
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

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

	@Override
	public int compareTo(User o) {
		return username.compareToIgnoreCase(o.username);
	}
}
