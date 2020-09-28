package de.sdsd.projekt.agrirouter.request;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import agrirouter.request.payload.account.Endpoints.ListEndpointsQuery;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse;
import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse.Endpoint;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.ARRequest.ARSingleRequest;

/**
 * Request a list of connected endpoints, including the own endpoint.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARListEndpointsRequest extends ARSingleRequest<List<AREndpoint>> {

	private static final String TYPE_UNFILTERED = "dke:list_endpoints_unfiltered";
	private static final String TYPE_FILTERED = "dke:list_endpoints";

	private final ListEndpointsQuery.Builder mPayload;
	private boolean considerRoutes = false;

	/**
	 * Creates a new unfiltered endpoint request.
	 */
	public ARListEndpointsRequest() {
		super(TYPE_UNFILTERED);
		mPayload = ListEndpointsQuery.newBuilder();
		setDirection(ARDirection.SEND_RECEIVE);
	}
	
	/**
	 * Specify whether to show only endpoints with an active routing to or from this endpoint.
	 * 
	 * @param filtered true to show only endpoints you can send messages to or receive messages from.
	 * @return this object for method chaining
	 */
	public ARListEndpointsRequest considerRoutingRules(boolean filtered) {
		this.considerRoutes = filtered;
		req.setTechnicalMessageType(filtered ? TYPE_FILTERED : TYPE_UNFILTERED);
		return this;
	}
	
	/**
	 * Set the direction whether the shown endpoints can send or receive. 
	 * {@link ARDirection#SEND_RECEIVE} shows endpoints which can send or receive. This is set by default.
	 * This filter is inactive unless {@link #considerRoutingRules(boolean)} is set to true.
	 * 
	 * @param direction agrirouter message direction
	 * @return this object for method chaining
	 */
	public ARListEndpointsRequest setDirection(ARDirection direction) {
		mPayload.setDirectionValue(direction.number());
		return this;
	}
	
	/**
	 * Set a filter to only return endpoints that can send and/or receive the specified message type.
	 * This filter is inactive unless {@link #considerRoutingRules(boolean)} is set to true.
	 * 
	 * @param type agrirouter message type
	 * @return this object for method chaining
	 */
	public ARListEndpointsRequest setMessageTypeFilter(ARMessageType type) {
		mPayload.setTechnicalMessageType(type.technicalMessageType());
		return this;
	}

	/**
	 * Remove the message type filter.
	 * @return this object for method chaining
	 */
	public ARListEndpointsRequest removeMessageTypeFilter() {
		mPayload.clearTechnicalMessageType();
		return this;
	}

	@Override
	protected Message getParams() {
		return mPayload.build();
	}

	@Override
	protected List<AREndpoint> parseResponse(ResponseEnvelope header, Any payload) throws InvalidProtocolBufferException {
		ListEndpointsResponse response = payload.unpack(ListEndpointsResponse.class);
		if(DEBUG_MODE) System.out.println(response.toString());
		ArrayList<AREndpoint> list = new ArrayList<>(response.getEndpointsCount());
		for(Endpoint ep : response.getEndpointsList()) {
			list.add(new AREndpoint(ep, considerRoutes));
		}
		return list;
	}

}
