package de.sdsd.projekt.prototype.data;

import org.redisson.api.RedissonClient;

import agrirouter.response.payload.account.Endpoints.ListEndpointsResponse.Endpoint;
import de.sdsd.projekt.agrirouter.request.AREndpoint;

/**
 * Cache store of agrirouter endpoints.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AREndpointStore extends EntityStore<AREndpoint> {
	
	public static AREndpoint getDefault(String id) {
		return new AREndpoint(Endpoint.newBuilder()
				.setEndpointId(id)
				.setEndpointName(id)
				.build(), false);
	}

	public AREndpointStore(RedissonClient redis, User user) {
		super(redis, user, "arendpoint");
	}

	@Override
	protected String getId(AREndpoint e) {
		return e.getId();
	}

	@Override
	protected AREndpoint createDefault(String id) {
		return getDefault(id);
	}
}
