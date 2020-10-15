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
	
	/** The Constant EXP. */
	public static final String ACC_ID = "accountId", CONN = "connection", SUBS = "subscriptions", EXP = "expire";
	
	/**
	 * Creates the.
	 *
	 * @param accountId the account id
	 * @param onboardingResponse the onboarding response
	 * @param arconn the arconn
	 * @return the document
	 */
	public static Document create(String accountId, String onboardingResponse, ARConnection arconn) {
		return new Document()
			.append(ACC_ID, accountId)
			.append(CONN, onboardingResponse)
			.append(EXP, Date.from(arconn.getExpirationDate()));
	}
	
	/** The user. */
	private final User user;
	
	/** The account id. */
	private final String accountId;
	
	/** The onboarding info. */
	private final JSONObject onboardingInfo;
	
	/** The subscriptions. */
	private Set<ARMessageType> subscriptions;
	
	/** The expiration date. */
	private final Instant expirationDate;
	
	/**
	 * Instantiates a new AR conn.
	 *
	 * @param doc the doc
	 * @param user the user
	 */
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
	
	/**
	 * Gets the account id.
	 *
	 * @return the account id
	 */
	public String getAccountId() {
		return accountId;
	}
	
	/**
	 * Gets the own endpoint id.
	 *
	 * @return the own endpoint id
	 */
	public String getOwnEndpointId() {
		return onboardingInfo.getString("sensorAlternateId");
	}
	
	/**
	 * Gets the onboarding info.
	 *
	 * @return the onboarding info
	 */
	public String getOnboardingInfo() {
		return onboardingInfo.toString();
	}
	
	/**
	 * Conn.
	 *
	 * @return the AR connection
	 */
	public ARConnection conn() {
		if(Main.DEBUG_MODE && isMQTT())
			throw new RuntimeException("No MQTT connections in DEBUG Mode!");
		try {
			return user.manager.getARConnection(user, onboardingInfo);
		} catch (JSONException | IOException | GeneralSecurityException | MqttException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Gets the subscriptions.
	 *
	 * @return the subscriptions
	 */
	public Set<ARMessageType> getSubscriptions() {
		return subscriptions;
	}

	/**
	 * Sets the subscriptions.
	 *
	 * @param types the types
	 * @return the bson
	 */
	public Bson setSubscriptions(Set<ARMessageType> types) {
		this.subscriptions = Collections.unmodifiableSet(types);
		return Updates.set(User.AR + "." + SUBS, types.stream()
				.map(ARMessageType::technicalMessageType)
				.collect(Util.asMongoStringList()));
	}
	
	/**
	 * Gets the expiration date.
	 *
	 * @return the expiration date
	 */
	public Instant getExpirationDate() {
		return expirationDate;
	}
	
	/**
	 * Checks if is qa.
	 *
	 * @return true, if is qa
	 */
	public boolean isQA() {
		JSONObject c = onboardingInfo.getJSONObject("connectionCriteria");
		return c.has("host") ? c.getString("host").contains("dke-qa.")
				: c.getString("measures").startsWith("https://dke-qa.");
	}
	
	/**
	 * Checks if is mqtt.
	 *
	 * @return true, if is mqtt
	 */
	public boolean isMQTT() {
		return onboardingInfo.getJSONObject("connectionCriteria").has("port");
	}
}
