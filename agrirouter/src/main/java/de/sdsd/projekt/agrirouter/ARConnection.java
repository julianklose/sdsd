package de.sdsd.projekt.agrirouter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.protobuf.InvalidProtocolBufferException;

import de.sdsd.projekt.agrirouter.ARMessage.Response;
import de.sdsd.projekt.agrirouter.request.ARCapabilities;
import de.sdsd.projekt.agrirouter.request.feed.ARConfirmMessage;
import de.sdsd.projekt.agrirouter.request.feed.ARPushNotificationReceiver;

/**
 * Agrirouter connection. This class represents an endpoint specific connection
 * to the agrirouter. You need this for every agrirouter function except of
 * onboarding.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public abstract class ARConnection implements AutoCloseable {

	/**
	 * Construct and open a new connection to the agrirouter. Make sure to
	 * {@link #close()} the connection when you don't need it anymore.
	 * 
	 * @param onboardingResponse json response from the
	 *                           {@link AROnboarding.SecureOnboardingContext#onboard(String)}
	 * @param executor           executor service with thread pool to use for
	 *                           receiving responses from the agrirouter. Doesn't
	 *                           need more than one thread.
	 * @throws JSONException            when there is an error in the json
	 *                                  onboarding response.
	 * @throws GeneralSecurityException error during creation of the keys for client
	 *                                  ssl communication
	 * @throws IOException              error during loading of the ssl keys
	 * @throws MqttException            for MQTT communication errors
	 */
	public static ARConnection create(JSONObject onboardingResponse, ScheduledExecutorService executor)
			throws JSONException, GeneralSecurityException, IOException, MqttException {
		JSONObject hosts = onboardingResponse.getJSONObject("connectionCriteria");
		switch (hosts.getInt("gatewayId")) {
		case 2:
			return new MqttConnection(onboardingResponse, executor);
		case 3:
			return new RestConnection(onboardingResponse, executor);
		default:
			throw new JSONException("connectionCriteria.gatewayId must be either 2 or 3");
		}
	}

	protected ARSender requestSender;

	protected final String deviceAlternateId, capabilityAlternateId, endpointId;
	protected final String urlRequest, urlResult;
	protected final X509Certificate certificate;
	protected final SSLContext sslContext;

	protected ARPushNotificationReceiver pushReceiver = null;

	/**
	 * Construct and open a new connection to the agrirouter. Make sure to
	 * {@link #close()} the connection when you don't need it anymore. Set a
	 * {@link #requestSender}.
	 * 
	 * @param onboardingResponse json response from the
	 *                           {@link AROnboarding.SecureOnboardingContext#onboard(String)}
	 * @throws JSONException            when there is an error in the json
	 *                                  onboarding response.
	 * @throws GeneralSecurityException error during creation of the keys for client
	 *                                  ssl communication
	 * @throws IOException              error during loading of the ssl keys
	 */
	ARConnection(JSONObject onboardingResponse) throws JSONException, GeneralSecurityException, IOException {
		this.deviceAlternateId = onboardingResponse.getString("deviceAlternateId");
		this.capabilityAlternateId = onboardingResponse.getString("capabilityAlternateId");
		this.endpointId = onboardingResponse.getString("sensorAlternateId");

		JSONObject hosts = onboardingResponse.getJSONObject("connectionCriteria");
		this.urlRequest = hosts.getString("measures");
		this.urlResult = hosts.getString("commands");

		JSONObject auth = onboardingResponse.getJSONObject("authentication");
		char[] secret = auth.getString("secret").toCharArray();
		ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(auth.getString("certificate")));

		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(in, secret);
		this.certificate = (X509Certificate) keyStore.getCertificate(keyStore.aliases().nextElement());

		this.sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, secret).build();
	}

	/**
	 * Returns the point in time when the certificate is no longer valid.
	 * 
	 * @return expiration date instant
	 */
	public Instant getExpirationDate() {
		return certificate.getNotAfter().toInstant();
	}

	/**
	 * Checks that the certificate is currently valid. It is if the current date and
	 * time are within the validity period given in the certificate.
	 * 
	 * @throws CertificateExpiredException     if the certificate has expired.
	 * @throws CertificateNotYetValidException if the certificate is not yet valid.
	 */
	public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
		certificate.checkValidity();
	}

	/**
	 * Returns the address for sending requests.
	 * 
	 * @return request url
	 */
	public String getUrlRequest() {
		return urlRequest;
	}

	/**
	 * Returns the address for optaining results.
	 * 
	 * @return result url
	 */
	public String getUrlResult() {
		return urlResult;
	}

	/**
	 * Returns the agrirouter device alternate ID.
	 * 
	 * @return device alternate ID
	 */
	public String getDeviceAlternateId() {
		return deviceAlternateId;
	}

	/**
	 * Returns the agrirouter capability alternate ID.
	 * 
	 * @return capability alternate ID
	 */
	public String getCapabilityAlternateId() {
		return capabilityAlternateId;
	}

	/**
	 * Returns the ID of this endpoint.
	 * 
	 * @return sensor alternate ID
	 */
	public String getEndpointId() {
		return endpointId;
	}

	/**
	 * Set this to receive push notifications from the agrirouter. Make sure push
	 * notifications were enabled in {@link ARCapabilities}.
	 * 
	 * @param receiver push notification callback
	 * @return this object for method chaining
	 * @see ARCapabilities#enablePushNotifications(boolean)
	 */
	public ARConnection setPushNotificationReceiver(@Nullable ARPushNotificationReceiver receiver) {
		this.pushReceiver = receiver;
		return this;
	}

	void pushNotification(Response resp) {
		if (pushReceiver != null) {
			ARConfirmMessage confirm = pushReceiver.readResponse(resp.header, resp.payload);
			if (confirm.count() > 0) {
				try {
					sendRequestAsync(confirm, 3, TimeUnit.SECONDS);
				} catch (Exception e) {
					pushReceiver.onError(e);
				}
			}
		}
	}

	/**
	 * Send the a request to the agrirouter asynchronously.
	 * 
	 * @param request the agrirouter request to send
	 * @param timeout the time from now to wait before aborting, 0 for infinite
	 *                waiting
	 * @param unit    the time unit of the delay parameter
	 * @return the future object to obtain results or exceptions
	 * @throws IOException error while processing the request
	 * @throws ARException error while sending the request to the agrirouter
	 * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
	 */
	public <T> CompletableFuture<T> sendRequestAsync(ARRequest<T> request, long timeout, TimeUnit unit)
			throws IOException, ARException {
		return requestSender.send(request.build(), timeout, unit);
	}

	/**
	 * Send the a request to the agrirouter synchrounously.
	 * 
	 * @param request the agrirouter request to send
	 * @param timeout the time from now to wait before aborting, 0 for infinite
	 *                waiting
	 * @param unit    the time unit of the delay parameter
	 * @return the result of the request
	 * @throws IOException          error while processing the request
	 * @throws InterruptedException if the current thread was interrupted while
	 *                              waiting
	 * @throws ARException          agrirouter error
	 */
	public <T> T sendRequest(ARRequest<T> request, long timeout, TimeUnit unit)
			throws IOException, InterruptedException, ARException {
		try {
			return requestSender.send(request.build(), timeout, unit).get();
		} catch (ExecutionException ee) {
			try {
				throw ee.getCause();
			} catch (ARException | InvalidProtocolBufferException e) {
				throw e;
			} catch (Throwable e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public void close() throws Exception {
		requestSender.close();
	}

	public static final class MqttConnection extends ARConnection {
		private final String host, clientId;
		private final int port;
		private final IMqttClient client;

		private static final String MQTT_URL_TEMPLATE = "ssl://%s:%d";

		/**
		 * Construct and open a new MQTT connection to the agrirouter. Make sure to
		 * {@link #close()} the connection when you don't need it anymore.
		 * 
		 * @param onboardingResponse json response from the
		 *                           {@link AROnboarding.SecureOnboardingContext#onboard(String)}
		 * @param executor           executor service with thread pool to use for
		 *                           receiving responses from the agrirouter. Doesn't
		 *                           need more than one thread.
		 * @throws JSONException            when there is an error in the json
		 *                                  onboarding response.
		 * @throws GeneralSecurityException error during creation of the keys for client
		 *                                  ssl communication
		 * @throws IOException              error during loading of the ssl keys
		 * @throws MqttException            for communication errors
		 * 
		 * @see IMqttClient
		 */
		public MqttConnection(JSONObject onboardingResponse, ScheduledExecutorService executor)
				throws JSONException, GeneralSecurityException, IOException, MqttException {
			super(onboardingResponse);

			JSONObject hosts = onboardingResponse.getJSONObject("connectionCriteria");
			this.port = hosts.getInt("port");
			this.host = hosts.getString("host");
			this.clientId = hosts.getString("clientId");

			this.requestSender = new ARMqttSender(executor, this);

			MqttConnectOptions options = new MqttConnectOptions();
			options.setSocketFactory(sslContext.getSocketFactory());
			options.setKeepAliveInterval(60);
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);

			this.client = new MqttClient(String.format(MQTT_URL_TEMPLATE, host, port), this.getClientId());
			client.setCallback((MqttCallback) requestSender); // set Callback function
			client.connect(options);
			client.subscribe(this.getUrlResult()); // subscribe to Commands Topic
		}

		/**
		 * Returns the agrirouter MQTT client ID.
		 * 
		 * @return client ID
		 */
		public String getClientId() {
			return clientId;
		}

		/**
		 * Returns the agrirouter MQTT host.
		 * 
		 * @return host
		 */
		public String getHost() {
			return host;
		}

		/**
		 * Returns the agrirouter MQTT port.
		 * 
		 * @return port
		 */
		public int getPort() {
			return port;
		}

		/**
		 * Returns the agrirouter MQTT server URI.
		 * 
		 * @return server URI
		 */
		public String getServerURI() {
			return client.getServerURI();
		}

		/**
		 * Send a custom request using the client ssl certificate of this endpoint.
		 * 
		 * @param request a custom request to execute
		 * @throws MqttException            if the client is not connected or any other
		 *                                  problem with the connection
		 * @throws MqttPersistenceException when a problem with storing the message
		 * @see IMqttClient#publish(String, MqttMessage)
		 */
		public void send(MqttMessage request) throws MqttPersistenceException, MqttException {
			client.publish(urlRequest, request);
		}

		@Override
		public void close() throws Exception {
			super.close();
			client.disconnectForcibly(100, 100);
			client.close();
		}
	}

	public static final class RestConnection extends ARConnection {
		public static final int CONNECTION_TIMEOUT = 30 * 60 * 1000;
		public static final int CONNECTION_CHECK_INTERVAL = 10 * 60 * 1000;
		public static final int CONNECTION_FORCE_RECONNECT = 24 * 60 * 60 * 1000 - CONNECTION_CHECK_INTERVAL;

		private final CloseableHttpClient client;

		private final long reconnect;
		private long timeout;

		/**
		 * Construct and open a new rest connection to the agrirouter. Make sure to
		 * {@link #close()} the connection when you don't need it anymore.
		 * 
		 * @param onboardingResponse json response from the
		 *                           {@link AROnboarding.SecureOnboardingContext#onboard(String)}
		 * @param executor           executor service with thread pool to use for
		 *                           receiving responses from the agrirouter. Doesn't
		 *                           need more than one thread.
		 * @throws JSONException            when there is an error in the json
		 *                                  onboarding response.
		 * @throws GeneralSecurityException error during creation of the keys for client
		 *                                  ssl communication
		 * @throws IOException              error during loading of the ssl keys
		 * 
		 * @see CloseableHttpClient
		 */
		public RestConnection(JSONObject onboardingResponse, ScheduledExecutorService executor)
				throws JSONException, GeneralSecurityException, IOException {
			super(onboardingResponse);

			this.client = HttpClients.custom().setSSLContext(sslContext).build();
			this.requestSender = new ARRestSender(executor, this);

			this.reconnect = System.currentTimeMillis() + CONNECTION_FORCE_RECONNECT;
			resetTimeout();
		}

		public long getTimeout() {
			return timeout;
		}

		private void resetTimeout() {
			timeout = Math.min(reconnect, System.currentTimeMillis() + CONNECTION_TIMEOUT);
		}

		/**
		 * Send a custom request using the client ssl certificate of this endpoint.
		 * 
		 * @param request a custom request to execute
		 * @return the response to the request
		 * @throws ClientProtocolException in case of an http protocol error
		 * @throws IOException             in case of a problem or the connection was
		 *                                 aborted
		 * @throws ARException             in case the agrirouter connection has expired
		 * @see CloseableHttpClient#execute(HttpUriRequest)
		 */
		public CloseableHttpResponse send(HttpUriRequest request)
				throws ClientProtocolException, IOException, ARException {
			try {
				resetTimeout();
				return client.execute(request);
			} catch (SSLHandshakeException e) {
				throw new ARException("The agrirouter connection has expired, please reonboard");
			}
		}

		@Override
		public void close() throws Exception {
			super.close();
			client.close();
		}

	}
}
