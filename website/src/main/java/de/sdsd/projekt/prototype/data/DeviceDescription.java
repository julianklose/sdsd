package de.sdsd.projekt.prototype.data;

import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import efdi.GrpcEfdi.ISO11783_TaskData;

/**
 * Represents an EFDI device description, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DeviceDescription {
	public static final String CONTEXTID = "context", DEVICEDESCRIPTION = "devicedescription", USER = "user";
	
	public static final Bson filter(String contextId) {
		return Filters.eq(CONTEXTID, contextId);
	}
	
	public static final Bson create(User user, byte[] content) {
		return Updates.combine(
				Updates.set(USER, user.getName()),
				Updates.set(DEVICEDESCRIPTION, new Binary(content)));
	}
	
	private final String contextId;
	private final String user;
	private final byte[] content;

	public DeviceDescription(Document doc) {
		this.contextId = doc.getString(CONTEXTID);
		this.user = doc.getString(USER);
		this.content = doc.get(DEVICEDESCRIPTION, Binary.class).getData();
	}
	
	public Bson filter() {
		return Filters.eq(CONTEXTID, contextId);
	}

	public String getContextId() {
		return contextId;
	}

	public String getUser() {
		return user;
	}

	public byte[] getBinaryContent() {
		return content;
	}
	
	public ISO11783_TaskData getContent() throws InvalidProtocolBufferException {
		return ISO11783_TaskData.parseFrom(content);
	}
	
	@Override
	public String toString() {
		try {
			return String.format("DeviceDescription(%s) of %s: %s", contextId, user, getContent().getDeviceList().stream()
					.map(dvc -> String.format("%s(%s)", dvc.getDeviceDesignator(), dvc.getDeviceSerialNumber()))
					.collect(Collectors.joining(", ")));
		} catch (InvalidProtocolBufferException e) {
			return String.format("DeviceDescription(%s) of %s: %s", contextId, user, e.getMessage());
		}
	}

}
