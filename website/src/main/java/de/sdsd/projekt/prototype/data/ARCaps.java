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
	public static final String USER = "user", CAPS = "caps", PUSH = "push";
	
	public static Bson filter(String user) {
		return Filters.eq(USER, user);
	}
	
	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}
	
	private static Document capsToDoc(Map<ARMessageType, ARDirection> caps) {
		Document doc = new Document();
		for(Entry<ARMessageType, ARDirection> cap : caps.entrySet()) {
			doc.append(cap.getKey().technicalMessageType(), cap.getValue().number());
		}
		return doc;
	}
	
	public static Document create(User user, Map<ARMessageType, ARDirection> capabilities, PushNotification pushNotifications) {
		return new Document()
				.append(USER, user.getName())
				.append(CAPS, capsToDoc(capabilities))
				.append(PUSH, pushNotifications.getNumber());
	}
	
	public static ARCaps getDefault(User user, ARConfig config) {
		return new ARCaps(user, config);
	}
	
	private final String user;
	private Map<ARMessageType, ARDirection> capabilities;
	private PushNotification pushNotification;
	
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
	
	protected ARCaps(User user, ARConfig config) {
		this.user = user.getName();
		this.capabilities = new HashMap<>();
		for(ARCapability cap : config.getCapabilities()) {
			capabilities.put(cap.getType(), cap.getDirection());
		}
		this.pushNotification = config.getPushNotifications();
	}
	
	public Bson filter() {
		return Filters.eq(USER, user);
	}
	
	public String getUser() {
		return user;
	}
	
	@CheckForNull
	public ARDirection getCapability(ARMessageType type) {
		return capabilities.get(type);
	}
	
	public List<ARCapability> getCapabilities() {
		List<ARCapability> out = new ArrayList<>(capabilities.size());
		for(Entry<ARMessageType, ARDirection> e : capabilities.entrySet()) {
			out.add(new ARCapability(e.getKey(), e.getValue()));
		}
		return out;
	}
	
	public Map<ARMessageType, ARDirection> getCapabilitieMap() {
		return capabilities;
	}
	
	public Bson setCapabilities(Map<ARMessageType, ARDirection> capabilities) {
		this.capabilities = capabilities;
		return Updates.set(CAPS, capsToDoc(capabilities));
	}

	public PushNotification getPushNotification() {
		return pushNotification;
	}

	public Bson setPushNotification(PushNotification pushNotification) {
		this.pushNotification = pushNotification;
		return Updates.set(PUSH, pushNotification.getNumber());
	}
	
	public ARCapabilities toARCapabilities(ARConfig config) {
		ARCapabilities ar = new ARCapabilities(config.getApplicationId(), config.getVersionId());
		ar.setPushNotifications(pushNotification);
		capabilities.entrySet().stream()
				.map(e -> new ARCapability(e.getKey(), e.getValue()))
				.forEach(ar::addCapability);
		return ar;
	}

}
