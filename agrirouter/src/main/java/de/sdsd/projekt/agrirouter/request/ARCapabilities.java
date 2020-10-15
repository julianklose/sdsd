package de.sdsd.projekt.agrirouter.request;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification;
import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import agrirouter.response.Response.ResponseEnvelope;
import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.AROnboarding;
import de.sdsd.projekt.agrirouter.ARRequest.ARSingleRequest;
import de.sdsd.projekt.agrirouter.request.feed.ARPushNotificationReceiver;

/**
 * Agrirouter message to set the app capabilities.
 * This tells the agrirouter which data types this endpoint can send or receive.
 * Use {@link AROnboarding#getCapabilitieDeclaration()} to get a preset instance.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARCapabilities extends ARSingleRequest<Boolean> {

	/** The m payload. */
	private final CapabilitySpecification.Builder mPayload;

	/**
	 * Creates a new capability confirmation message.
	 * Use {@link AROnboarding#getCapabilitieDeclaration()} to get a preset instance.
	 * 
	 * @param applicationId agrirouter applicationId
	 * @param versionId agrirouter versionId
	 */
	public ARCapabilities(String applicationId, String versionId) {
		super("dke:capabilities");
		mPayload = CapabilitySpecification.newBuilder()
			.setAppCertificationId(applicationId)
			.setAppCertificationVersionId(versionId);
	}

	/**
	 * Add a capability to the list.
	 * 
	 * @param cap agrirouter message type and direction
	 * @return this object for method chaining
	 */
	public ARCapabilities addCapability(ARCapability cap) {
		mPayload.addCapabilities(cap.toCapability());
		return this;
	}
	
	/**
	 * Enable/Disable push notifications to receive messages automatically without request.
	 * Set a {@link ARPushNotificationReceiver} to receive the messages.
	 * 
	 * @param pushNotification push notification state
	 * @return this object for method chaining
	 * @see ARConnection#setPushNotificationReceiver(ARPushNotificationReceiver)
	 */
	public ARCapabilities setPushNotifications(PushNotification pushNotification) {
		mPayload.setEnablePushNotifications(pushNotification);
		return this;
	}

	/**
	 * Gets the params.
	 *
	 * @return the params
	 */
	@Override
	protected Message getParams() {
		return mPayload.build();
	}

	/**
	 * Parses the response.
	 *
	 * @param header the header
	 * @param payload the payload
	 * @return the boolean
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
	@Override
	protected Boolean parseResponse(ResponseEnvelope header, Any payload) throws InvalidProtocolBufferException {
		return header.getResponseCode() == 201;
	}

}
