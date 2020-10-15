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
	
	/** The Constant USER. */
	public static final String CONTEXTID = "context", DEVICEDESCRIPTION = "devicedescription", USER = "user";
	
	/**
	 * Filter.
	 *
	 * @param contextId the context id
	 * @return the bson
	 */
	public static final Bson filter(String contextId) {
		return Filters.eq(CONTEXTID, contextId);
	}
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param content the content
	 * @return the bson
	 */
	public static final Bson create(User user, byte[] content) {
		return Updates.combine(
				Updates.set(USER, user.getName()),
				Updates.set(DEVICEDESCRIPTION, new Binary(content)));
	}
	
	/** The context id. */
	private final String contextId;
	
	/** The user. */
	private final String user;
	
	/** The content. */
	private final byte[] content;

	/**
	 * Instantiates a new device description.
	 *
	 * @param doc the doc
	 */
	public DeviceDescription(Document doc) {
		this.contextId = doc.getString(CONTEXTID);
		this.user = doc.getString(USER);
		this.content = doc.get(DEVICEDESCRIPTION, Binary.class).getData();
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(CONTEXTID, contextId);
	}

	/**
	 * Gets the context id.
	 *
	 * @return the context id
	 */
	public String getContextId() {
		return contextId;
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
	 * Gets the binary content.
	 *
	 * @return the binary content
	 */
	public byte[] getBinaryContent() {
		return content;
	}
	
	/**
	 * Gets the content.
	 *
	 * @return the content
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
	public ISO11783_TaskData getContent() throws InvalidProtocolBufferException {
		return ISO11783_TaskData.parseFrom(content);
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
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
