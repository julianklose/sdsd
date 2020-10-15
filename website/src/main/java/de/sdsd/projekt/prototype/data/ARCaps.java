package de.sdsd.projekt.prototype.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.CheckForNull;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import de.sdsd.projekt.agrirouter.ARConfig;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.request.ARCapabilities;
import de.sdsd.projekt.agrirouter.request.ARCapability;

/**
 * Represents agrirouter capabilities, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARCaps {
	
	/** The Constant PUSH. */
	public static final String USER = "user", CAPS = "caps", PUSH = "push";
	
	/**
	 * Filter.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filter(String user) {
		return Filters.eq(USER, user);
	}
	
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
	 * Caps to doc.
	 *
	 * @param caps the caps
	 * @return the document
	 */
	private static Document capsToDoc(Map<ARMessageType, ARDirection> caps) {
		Document doc = new Document();
		for(Entry<ARMessageType, ARDirection> cap : caps.entrySet()) {
			doc.append(cap.getKey().technicalMessageType(), cap.getValue().number());
		}
		return doc;
	}
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param capabilities the capabilities
	 * @param pushNotifications the push notifications
	 * @return the document
	 */
	public static Document create(User user, Map<ARMessageType, ARDirection> capabilities, PushNotification pushNotifications) {
		return new Document()
				.append(USER, user.getName())
				.append(CAPS, capsToDoc(capabilities))
				.append(PUSH, pushNotifications.getNumber());
	}
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param config the config
	 * @return the default
	 */
	public static ARCaps getDefault(User user, ARConfig config) {
		return new ARCaps(user, config);
	}
	
	/** The user. */
	private final String user;
	
	/** The capabilities. */
	private Map<ARMessageType, ARDirection> capabilities;
	
	/** The push notification. */
	private PushNotification pushNotification;
	
	/**
	 * Instantiates a new AR caps.
	 *
	 * @param doc the doc
	 */
	public ARCaps(Document doc) {
		this.user = doc.getString(USER);
		Document caps = doc.get(CAPS, Document.class);
		this.capabilities = new HashMap<>(caps.size());
		for(Entry<String, Object> entry : caps.entrySet()) {
			capabilities.put(ARMessageType.from(entry.getKey()), 
					ARDirection.from((Integer)entry.getValue()));
		}
		this.pushNotification = PushNotification.forNumber(doc.getInteger(PUSH));
	}
	
	/**
	 * Instantiates a new AR caps.
	 *
	 * @param user the user
	 * @param config the config
	 */
	protected ARCaps(User user, ARConfig config) {
		this.user = user.getName();
		this.capabilities = new HashMap<>();
		for(ARCapability cap : config.getCapabilities()) {
			capabilities.put(cap.getType(), cap.getDirection());
		}
		this.pushNotification = config.getPushNotifications();
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(USER, user);
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
	 * Gets the capability.
	 *
	 * @param type the type
	 * @return the capability
	 */
	@CheckForNull
	public ARDirection getCapability(ARMessageType type) {
		return capabilities.get(type);
	}
	
	/**
	 * Gets the capabilities.
	 *
	 * @return the capabilities
	 */
	public List<ARCapability> getCapabilities() {
		List<ARCapability> out = new ArrayList<>(capabilities.size());
		for(Entry<ARMessageType, ARDirection> e : capabilities.entrySet()) {
			out.add(new ARCapability(e.getKey(), e.getValue()));
		}
		return out;
	}
	
	/**
	 * Gets the capabilitie map.
	 *
	 * @return the capabilitie map
	 */
	public Map<ARMessageType, ARDirection> getCapabilitieMap() {
		return capabilities;
	}
	
	/**
	 * Sets the capabilities.
	 *
	 * @param capabilities the capabilities
	 * @return the bson
	 */
	public Bson setCapabilities(Map<ARMessageType, ARDirection> capabilities) {
		this.capabilities = capabilities;
		return Updates.set(CAPS, capsToDoc(capabilities));
	}

	/**
	 * Gets the push notification.
	 *
	 * @return the push notification
	 */
	public PushNotification getPushNotification() {
		return pushNotification;
	}

	/**
	 * Sets the push notification.
	 *
	 * @param pushNotification the push notification
	 * @return the bson
	 */
	public Bson setPushNotification(PushNotification pushNotification) {
		this.pushNotification = pushNotification;
		return Updates.set(PUSH, pushNotification.getNumber());
	}
	
	/**
	 * To AR capabilities.
	 *
	 * @param config the config
	 * @return the AR capabilities
	 */
	public ARCapabilities toARCapabilities(ARConfig config) {
		ARCapabilities ar = new ARCapabilities(config.getApplicationId(), config.getVersionId());
		ar.setPushNotifications(pushNotification);
		capabilities.entrySet().stream()
				.map(e -> new ARCapability(e.getKey(), e.getValue()))
				.forEach(ar::addCapability);
		return ar;
	}

}
