package de.sdsd.projekt.prototype.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.model.Updates;

import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.prototype.Main;

/**
 * Represents an agrirouter connection, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARConn {
	public static final String ACC_ID = "accountId", CONN = "connection", SUBS = "subscriptions", EXP = "expire";
	
	public static Document create(String accountId, String onboardingResponse, ARConnection arconn) {
		return new Document()
			.append(ACC_ID, accountId)
			.append(CONN, onboardingResponse)
			.append(EXP, Date.from(arconn.getExpirationDate()));
	}
	
	private final User user;
	private final String accountId;
	private final JSONObject onboardingInfo;
	private Set<ARMessageType> subscriptions;
	private final Instant expirationDate;
	
	public ARConn(Document doc, User user) {
		this.user = user;
		this.accountId = doc.getString(ACC_ID);
		this.onboardingInfo = new JSONObject(doc.getString(CONN));
		this.subscriptions = Collections.unmodifiableSet(Util
				.stringstreamFromMongo(doc.get(SUBS))
				.map(ARMessageType::from)
				.collect(Collectors.toSet()));
		Date date = doc.getDate(EXP);
		this.expirationDate = date != null ? date.toInstant() : Instant.now();
	}
	
	public String getAccountId() {
		return accountId;
	}
	
	public String getOwnEndpointId() {
		return onboardingInfo.getString("sensorAlternateId");
	}
	
	public String getOnboardingInfo() {
		return onboardingInfo.toString();
	}
	
	public ARConnection conn() {
		if(Main.DEBUG_MODE && isMQTT())
			throw new RuntimeException("No MQTT connections in DEBUG Mode!");
		try {
			return user.manager.getARConnection(user, onboardingInfo);
		} catch (JSONException | IOException | GeneralSecurityException | MqttException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Set<ARMessageType> getSubscriptions() {
		return subscriptions;
	}

	public Bson setSubscriptions(Set<ARMessageType> types) {
		this.subscriptions = Collections.unmodifiableSet(types);
		return Updates.set(User.AR + "." + SUBS, types.stream()
				.map(ARMessageType::technicalMessageType)
				.collect(Util.asMongoStringList()));
	}
	
	public Instant getExpirationDate() {
		return expirationDate;
	}
	
	public boolean isQA() {
		JSONObject c = onboardingInfo.getJSONObject("connectionCriteria");
		return c.has("host") ? c.getString("host").contains("dke-qa.")
				: c.getString("measures").startsWith("https://dke-qa.");
	}
	
	public boolean isMQTT() {
		return onboardingInfo.getJSONObject("connectionCriteria").has("port");
	}
}
