package de.sdsd.projekt.agrirouter.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse.Direction;
import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse.Endpoint;
import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse.MessageType;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;

/**
 * Represents an agrirouter endpoint.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AREndpoint implements Serializable, Comparable<AREndpoint> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6514636221732741587L;
	/**
	 * Agrirouter endpoint identifier.
	 */
	protected final String id;
	/**
	 * Name of the endpoint as specified in the agrirouter control center.
	 */
	protected final String name;
	/**
	 * Type of the endpoint.
	 * <table border="1"><tr><th>Type</th><th>Description</th></tr>
	 * <tr><td>application</td><td>all regular agrirouter endpoints including web apps and telemetry units</td></tr>
	 * <tr><td>pairedAccount</td><td>account of another person that is connected to your agrirouter account</td></tr>
	 * <tr><td>endpointGroup</td><td>group of endpoints, created in the control center</td></tr>
	 * </table> (this list may be incomplete)
	 */
	protected final String type;
	/**
	 * Whether the endpoint is active.
	 * Endpoints can be deactivated in the agrirouter control center.
	 */
	protected final boolean active;
	/**
	 * List of which data types the endpoint can send or receive.
	 */
	protected final List<ARCapability> capabilities;
	/**
	 * Unique id of the endpoint created by the external device.
	 */
	protected final String externalId;
	
	/**
	 * Creates a new agrirouter endpoint instante from the protobuf endpoint.
	 * 
	 * @param ep agrirouter protobuf endpoint
	 * @param invertCapabilities set true to invert the capabilities because it originates from routes
	 */
	public AREndpoint(Endpoint ep, boolean invertCapabilities) {
		this.id = ep.getEndpointId();
		this.name = ep.getEndpointName();
		this.type = ep.getEndpointType();
		this.active = "active".equalsIgnoreCase(ep.getStatus());
		this.capabilities = new ArrayList<>(ep.getMessageTypesCount());
		for(MessageType mt : ep.getMessageTypesList()) {
			ARDirection dir;
			if(mt.getDirection() == Direction.RECEIVE) dir = invertCapabilities ? ARDirection.SEND : ARDirection.RECEIVE;
			else if(mt.getDirection() == Direction.SEND) dir = invertCapabilities ? ARDirection.RECEIVE : ARDirection.SEND;
			else dir = ARDirection.SEND_RECEIVE;
			capabilities.add(new ARCapability(ARMessageType.from(mt.getTechnicalMessageType()), dir));
		}
		this.externalId = ep.getExternalId();
	}
	
	/**
	 * Creates a shallow copy.
	 * 
	 * @param ep arendpoint to copy.
	 */
	public AREndpoint(AREndpoint ep) {
		this.id = ep.id;
		this.name = ep.name;
		this.type = ep.type;
		this.active = ep.active;
		this.capabilities = ep.capabilities;
		this.externalId = ep.externalId;
	}

	/**
	 * Returns the agrirouter endpoint identifier.
	 * @return agrirouter endpoint identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the name of the endpoint as specified in the agrirouter control center.
	 * @return specified name of the endpoint
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the type of the endpoint.
	 * 
	 * @return <table border="1"><tr><th>Type</th><th>Description</th></tr>
	 * <tr><td>application</td><td>all regular agrirouter endpoints including web apps and telemetry units</td></tr>
	 * <tr><td>pairedAccount</td><td>account of another person that is connected to your agrirouter account</td></tr>
	 * <tr><td>endpointGroup</td><td>group of endpoints, created in the control center</td></tr>
	 * <tr><td>machine</td><td>virtual endpoint of a machine, created by the agrirouter after a CU sent an EFDI device description</td></tr>
	 * <tr><td>virtualCU</td><td>CU, created by a telemetry platform</td></tr>
	 * </table> (this list may be incomplete)
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns whether the endpoint is active.
	 * Endpoints can be deactivated in the agrirouter control center.
	 * @return true for active
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * Returns the unique id of the endpoint created by the external device.
	 * @return Unique endpoint id
	 */
	public String getExternalId() {
		return externalId;
	}
	
	/**
	 * Returns a list of which data types the endpoint can send or receive.
	 * @return endpoint capabilities
	 */
	public List<ARCapability> getCapabilities() {
		return capabilities;
	}
	
	/**
	 * Can this endpoint send messages of the specified type?.
	 *
	 * @param type agrirouter message type
	 * @return true if it can send those messages
	 */
	public boolean canSend(ARMessageType type) {
		for (ARCapability cap : capabilities) {
			if(cap.getDirection() != ARDirection.RECEIVE 
					&& cap.getType() == type) 
				return true;
		}
		return false;
	}
	
	/**
	 * Can this endpoint send messages?.
	 *
	 * @return true if it can send those messages
	 */
	public boolean canSend() {
		for (ARCapability cap : capabilities) {
			if(cap.getDirection() != ARDirection.RECEIVE) 
				return true;
		}
		return false;
	}
	
	/**
	 * Can this endpoint receive messages of the specified type?.
	 *
	 * @param type agrirouter message type
	 * @return true if it can receive those messages
	 */
	public boolean canReceive(ARMessageType type) {
		for (ARCapability cap : capabilities) {
			if(cap.getDirection() != ARDirection.SEND 
					&& cap.getType() == type) 
				return true;
		}
		return false;
	}
	
	/**
	 * Can this endpoint receive messages?.
	 *
	 * @return true if it can receive those messages
	 */
	public boolean canReceive() {
		for (ARCapability cap : capabilities) {
			if(cap.getDirection() != ARDirection.SEND) 
				return true;
		}
		return false;
	}
	
	/**
	 * Clears the capabilities of this endpoint.
	 */
	public void clearCapabilities() {
		capabilities.clear();
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		sb.append("\n  ID: ").append(id);
		sb.append("\n  ExternalId: ").append(externalId);
		sb.append("\n  Name: ").append(name);
		sb.append("\n  Type: ").append(type);
		sb.append("\n  active: ").append(active);
		sb.append("\n  capabilities: [");
		capabilities.forEach(c -> sb.append("\n    ").append(c.toString()));
		sb.append("\n  ]\n}");
		return sb.toString();
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		AREndpoint other = (AREndpoint) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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
	public int compareTo(AREndpoint o) {
		if(active != o.active) return active ? -1 : 1;
		int cmptype = type.compareToIgnoreCase(o.type);
		if(cmptype != 0) return cmptype;
		return name.compareToIgnoreCase(o.name);
	}

}
