package de.sdsd.projekt.api;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;

/**
 * Base class for SDSD API connection clients.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public abstract class Client implements AutoCloseable {
	
	/**
	 * Executes JSON-RPC calls.
	 *
	 * @param endpoint the endpoint
	 * @param method the method
	 * @param token the token
	 * @param params the params
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public abstract JSONObject execute(String endpoint, String method, String token, Object... params) throws JsonRpcException;
	
	/**
	 * Creates the JSON-RPC call from the given parameters.
	 *
	 * @param id the id
	 * @param clazz the clazz
	 * @param method the method
	 * @param parameters the parameters
	 * @return the JSON object
	 */
	protected JSONObject request(String id, String clazz, String method, Object... parameters) {
		JSONArray params = new JSONArray();
		for(Object param : parameters)
			params.put(param);
		
		JSONObject request = new JSONObject();
		request.put("method", clazz + "." + method);
		request.put("params", params);
		request.put("id", id);
		return request;
	}
	
}
