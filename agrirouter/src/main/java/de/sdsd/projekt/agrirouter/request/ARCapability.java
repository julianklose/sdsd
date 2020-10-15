package de.sdsd.projekt.agrirouter.request;

import java.io.Serializable;

import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.Capability;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;

/**
 * Agrirouter capability that tells if an endpoint can receive and/or send a specified message type.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARCapability implements Serializable, Comparable<ARCapability> {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 2534369026520830841L;
	
	/** The type. */
	private final ARMessageType type;
	
	/** The direction. */
	private final ARDirection direction;
	
	/**
	 * Create a new agrirouter capability.
	 * 
	 * @param type agrirouter message type
	 * @param direction agrirouter message direction (send, receive, send&receive)
	 */
	public ARCapability(ARMessageType type, ARDirection direction) {
		super();
		this.type = type;
		this.direction = direction;
	}
	
	/**
	 * Create a new agrirouter capability.
	 * 
	 * @param technicalMessageType agrirouter message type, see {@link ARMessageType#technicalMessageType()}
	 * @param direction numeric agrirouter direction, see {@link ARDirection#number()}
	 */
	public ARCapability(String technicalMessageType, int direction) {
		super();
		this.type = ARMessageType.from(technicalMessageType);
		this.direction = ARDirection.from(direction);
	}
	
	/**
	 * Returns the specified agrirouter message type.
	 * @return agrirouter message type
	 */
	public ARMessageType getType() {
		return type;
	}
	
	/**
	 * Returns the specified agrirouter message direction.
	 * @return agrirouter message direction
	 */
	public ARDirection getDirection() {
		return direction;
	}
	
	/**
	 * Checks if is receive.
	 *
	 * @return whether this capabilities direction is receive or send/receive
	 */
	public boolean isReceive() {
		return direction == ARDirection.SEND_RECEIVE || direction == ARDirection.RECEIVE;
	}
	
	/**
	 * Checks if is send.
	 *
	 * @return whether this capabilities direction is send or send/receive
	 */
	public boolean isSend() {
		return direction == ARDirection.SEND_RECEIVE || direction == ARDirection.SEND;
	}
	
	/**
	 * Create and return a agrirouter protobuf capability.
	 * @return agrirouter protobuf capability
	 */
	Capability toCapability() {
		return Capability.newBuilder()
				.setTechnicalMessageType(type.technicalMessageType())
				.setDirectionValue(direction.number())
				.build();
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
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ARCapability other = (ARCapability) obj;
		if (direction != other.direction)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return new StringBuilder("Type: ")
				.append(type)
				.append("  Direction: ")
				.append(direction)
				.toString();
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(ARCapability o) {
		return type.compareTo(o.type);
	}
}
