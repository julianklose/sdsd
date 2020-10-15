package de.sdsd.projekt.api;

import java.io.IOException;
import java.util.UUID;

import javax.websocket.ClientEndpoint;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;

/**
 * The Class RestClient.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
@ClientEndpoint
public class RestClient extends Client {

	/** The Constant SDSD_REST_LOCAL. */
	private static final String SDSD_REST = "https://app.sdsd-projekt.de/api/json-rpc",
			SDSD_REST_LOCAL = "http://localhost:8081/api/json-rpc";

	/** The url. */
	private final String url;

	/** The client. */
	private final CloseableHttpClient client;

	/**
	 * Instantiates a new rest client.
	 *
	 * @param local if the SDSD website is accessible on localhost.
	 */
	public RestClient(boolean local) {
		this.url = local ? SDSD_REST_LOCAL : SDSD_REST;
		this.client = HttpClients.custom().build();
	}

	/**
	 * Execute.
	 *
	 * @param endpoint the endpoint
	 * @param method   the method
	 * @param token    the token
	 * @param params   the params
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	@Override
	public JSONObject execute(String endpoint, String method, String token, Object... params) throws JsonRpcException {
		HttpPost post = new HttpPost(url);

		if (token != null) {
			post.setHeader("token", token);
		}

		String rq = request(UUID.randomUUID().toString(), endpoint, method, params).toString();

		post.setEntity(new StringEntity(rq, "UTF-8"));

		String content;
		try (CloseableHttpResponse response = client.execute(post)) {
			content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		JSONObject resp = new JSONObject(content);

		if (resp.has("error")) {
			throw new JsonRpcException(resp.getJSONObject("error"));
		}

		if (resp.has("result")) {
			return resp.getJSONObject("result");
		}

		throw new RuntimeException("error: " + resp.toString(2));
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		client.close();
	}

}
