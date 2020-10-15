package de.sdsd.projekt.agrirouter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import de.sdsd.projekt.agrirouter.request.ARCapability;
import de.sdsd.projekt.agrirouter.request.feed.ARPushNotificationReceiver;

/**
 * Contains all necessary configuration for the agrirouter library to work.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public class ARConfig {
	/**
	 * If detailed information about sent requests and received results is shown.
	 */
	public static final boolean DEBUG_MODE = "true".equalsIgnoreCase(System.getProperty("detailedDebugMode"));
	/**
	 * If the raw sent and received messages are shown.
	 */
	public static final boolean DEBUG_MODE_RAW = "true".equalsIgnoreCase(System.getProperty("detailedDebugModeRaw"));
	/**
	 * Set to true to use the native protobuf interface of the REST interface.
	 */
	public static final boolean NATIVE_PROTOBUF_REST = true;
	/**
	 * Set to true to use the native protobuf interface of the MQTT interface
	 * (currently not supported by the agrirouter).
	 */
	public static final boolean NATIVE_PROTOBUF_MQTT = false;
	/**
	 * Set to true to use Base64 encoding for sending and receiving non telemetry
	 * messages.
	 */
	public static final boolean USE_BASE64 = true;

	/**
	 * Redirection URL for secure onboarding.
	 */
	private final URI redirectUrl;
	/**
	 * URL to the agrirouter onboarding service.
	 */
	private final URI onboardingUrl;
	/**
	 * Agrirouter identification of the application.
	 */
	private final String applicationId;
	/**
	 * Agrirouter identification of the certified version.
	 */
	private final String versionId;
	/**
	 * Agrirouter connection gateway type
	 */
	private final ARGateway gateway;
	/**
	 * Push Notifications
	 */
	private final PushNotification pushNotifications;
	/**
	 * Template for the JSON onboarding request payload.
	 */
	private final String requestTemplate;
	/**
	 * The application capabilities.
	 */
	private final Set<ARCapability> capabilities;
	/**
	 * Private RSA key of the application. Use for signing messages.
	 */
	private final PrivateKey appPrivateKey;
	/**
	 * Public RSA key of the agrirouter. Use for verifying signed messages.
	 */
	private final PublicKey arPublicKey;

	/**
	 * Private constructor for builder pattern.
	 * 
	 * @param builder config builder
	 * @throws URISyntaxException       if the given URLs aren't valid URIs.
	 * @throws JSONException            if the given request template is no valid
	 *                                  json object.
	 * @throws NoSuchAlgorithmException if this java runtime doesn't know RSA.
	 * @throws IllegalArgumentException if one of the given keys is not valid
	 *                                  Base64.
	 * @throws InvalidKeySpecException  if one of the given keys is not valid.
	 */
	private ARConfig(ARConfigBuilder builder) throws URISyntaxException, JSONException, NoSuchAlgorithmException,
			IllegalArgumentException, InvalidKeySpecException {

		this.redirectUrl = new URIBuilder(builder.agrirouterHost)
				.setPath("/application/" + builder.applicationId + "/authorize").build();
		this.onboardingUrl = new URI(builder.onboardingUrl);

		this.applicationId = builder.applicationId;
		this.versionId = builder.versionId;
		this.gateway = builder.gateway;
		this.pushNotifications = builder.pushNotifications;

		this.requestTemplate = new JSONObject().put("id", builder.endpointIdPrefix)
				.put("applicationId", builder.applicationId).put("certificationVersionId", builder.versionId)
				.put("gatewayId", builder.gateway.value).put("certificateType", "P12").toString();

		this.capabilities = Collections.unmodifiableSet(builder.capabilities);

		KeyFactory kf = KeyFactory.getInstance("RSA");
		byte[] key = Base64.getDecoder().decode(builder.appPrivateKey);
		this.appPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(key));
		key = Base64.getDecoder().decode(builder.arPublicKey);
		this.arPublicKey = kf.generatePublic(new X509EncodedKeySpec(key));
	}

	/**
	 * @return Redirection URL for secure onboarding
	 */
	public URI getRedirectUrl() {
		return redirectUrl;
	}

	/**
	 * @return URL to the agrirouter onboarding service for CUs
	 */
	public URI getCUOnboardUrl() {
		return onboardingUrl;
	}

	/**
	 * @return URL to the agrirouter revoke onboarding service
	 */
	public URI getRevokeUrl() {
		return URI.create(onboardingUrl + "/revoke");
	}

	/**
	 * @return URL to the agrirouter verify onboarding service
	 */
	public URI getVerifyUrl() {
		return URI.create(onboardingUrl + "/verify");
	}

	/**
	 * @return URL to the agrirouter onboard service
	 */
	public URI getOnboardUrl() {
		return URI.create(onboardingUrl + "/request");
	}

	/**
	 * @return Agrirouter identification of the application
	 */
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * @return Agrirouter identification of the certified version
	 */
	public String getVersionId() {
		return versionId;
	}

	/**
	 * @return Agrirouter connection gateway type
	 */
	public ARGateway getGateway() {
		return gateway;
	}

	/**
	 * @return whether push notifications are used
	 */
	public PushNotification getPushNotifications() {
		return pushNotifications;
	}

	/**
	 * @return Template for the JSON onboarding request payload (safe to be
	 *         manipulated)
	 */
	public JSONObject getRequest() {
		return new JSONObject(requestTemplate);
	}

	/**
	 * @return the application capabilities
	 */
	public Set<ARCapability> getCapabilities() {
		return capabilities;
	}

	/**
	 * @return Private RSA key of the application. Use for signing messages.
	 */
	public PrivateKey getAppPrivateKey() {
		return appPrivateKey;
	}

	/**
	 * @return Public RSA key of the agrirouter. Use for verifying signed messages.
	 */
	public PublicKey getArPublicKey() {
		return arPublicKey;
	}

	/**
	 * @return New builder for an agrirouter configuration
	 */
	public static ARConfigBuilder newBuilder() {
		return new ARConfigBuilder();
	}

	/**
	 * Builder for an agrirouter configuration.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class ARConfigBuilder {
		private String agrirouterHost;
		private String onboardingUrl;
		private String endpointIdPrefix;
		private String applicationId;
		private String versionId;
		private ARGateway gateway;
		private PushNotification pushNotifications;
		private final Set<ARCapability> capabilities = new HashSet<>();
		private String appPrivateKey;
		private String arPublicKey;

		/**
		 * @return Host address of the agrirouter website
		 */
		public String getAgrirouterHost() {
			return agrirouterHost;
		}

		/**
		 * @param agrirouterHost Host address of the agrirouter website
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setAgrirouterHost(String agrirouterHost) {
			this.agrirouterHost = agrirouterHost;
			return this;
		}

		/**
		 * @return URL to the agrirouter onboarding service
		 */
		public String getOnboardingUrl() {
			return onboardingUrl;
		}

		/**
		 * @param onboardingUrl URL to the agrirouter onboarding service
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setOnboardingUrl(String onboardingUrl) {
			this.onboardingUrl = onboardingUrl;
			return this;
		}

		/**
		 * @return the prefix for IDs of new onboarded endpoints
		 */
		public String getEndpointIdPrefix() {
			return endpointIdPrefix;
		}

		/**
		 * @param endpointIdPrefix the prefix for IDs of new onboarded endpoints
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setEndpointIdPrefix(String endpointIdPrefix) {
			this.endpointIdPrefix = endpointIdPrefix;
			return this;
		}

		/**
		 * @return Agrirouter identification of the application
		 */
		public String getApplicationId() {
			return applicationId;
		}

		/**
		 * @param applicationId Agrirouter identification of the application
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setApplicationId(String applicationId) {
			this.applicationId = applicationId;
			return this;
		}

		/**
		 * @return Agrirouter identification of the application version
		 */
		public String getVersionId() {
			return versionId;
		}

		/**
		 * @param versionId Agrirouter identification of the application version
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setVersionId(String versionId) {
			this.versionId = versionId;
			return this;
		}

		/**
		 * @return Agrirouter connection gateway type
		 */
		public ARGateway getGateway() {
			return gateway;
		}

		/**
		 * @param gateway Agrirouter connection gateway type
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setGateway(ARGateway gateway) {
			this.gateway = gateway;
			return this;
		}

		/**
		 * @return whether push notifications are used
		 */
		public PushNotification getPushNotifications() {
			return pushNotifications;
		}

		/**
		 * @param enable to receive messages without request
		 * @return this object for method chaining
		 * @see ARConnection#setPushNotificationReceiver(ARPushNotificationReceiver)
		 */
		public ARConfigBuilder setPushNotifications(PushNotification pushNotifications) {
			this.pushNotifications = pushNotifications;
			return this;
		}

		/**
		 * Add a capability to the list.
		 * 
		 * @param cap agrirouter message type and direction
		 * @return this object for method chaining
		 */
		public ARConfigBuilder addCapability(ARCapability cap) {
			this.capabilities.add(cap);
			return this;
		}

		/**
		 * Add a capability to the list.
		 * 
		 * @param technicalMessageType agrirouter message type, see
		 *                             {@link ARMessageType#technicalMessageType()}
		 * @param direction            numeric agrirouter directioni, see
		 *                             {@link ARMessageType.ARDirection#number()}
		 * @return this object for method chaining
		 */
		public ARConfigBuilder addCapability(String technicalMessageType, int direction) {
			this.capabilities.add(new ARCapability(technicalMessageType, direction));
			return this;
		}

		/**
		 * @return Base64 private RSA key of the application
		 */
		public String getAppPrivateKey() {
			return appPrivateKey;
		}

		/**
		 * @param appPrivateKey Base64 private RSA key of the application
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setAppPrivateKey(String appPrivateKey) {
			this.appPrivateKey = appPrivateKey;
			return this;
		}

		/**
		 * @return Base64 public RSA key of the agrirouter
		 */
		public String getArPublicKey() {
			return arPublicKey;
		}

		/**
		 * @param arPublicKey Base64 public RSA key of the agrirouter
		 * @return this object for method chaining
		 */
		public ARConfigBuilder setArPublicKey(String arPublicKey) {
			this.arPublicKey = arPublicKey;
			return this;
		}

		/**
		 * Build the agrirouter configuration.
		 * 
		 * @return immutable agrirouter configuration
		 * @throws URISyntaxException       if the given URLs aren't valid URIs.
		 * @throws JSONException            if the given request template is no valid
		 *                                  json object.
		 * @throws NoSuchAlgorithmException if this java runtime doesn't know RSA.
		 * @throws IllegalArgumentException if one of the given keys is not valid
		 *                                  Base64.
		 * @throws InvalidKeySpecException  if one of the given keys is not valid.
		 */
		public ARConfig build() throws URISyntaxException, JSONException, NoSuchAlgorithmException,
				IllegalArgumentException, InvalidKeySpecException {
			return new ARConfig(this);
		}
	}

	/**
	 * Type of agrirouter connection gateway.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static enum ARGateway {
		REST("3"), MQTT("2");

		public final String value;

		private ARGateway(String value) {
			this.value = value;
		}
	}
}
