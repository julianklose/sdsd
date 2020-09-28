package de.sdsd.projekt.agrirouter;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;
import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE_RAW;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;

import de.sdsd.projekt.agrirouter.request.ARCapabilities;

/**
 * Provides onboarding functions for CU und App onboarding.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public final class AROnboarding {

	/**
	 * Configuration of the agrirouter library.
	 */
	private final ARConfig config;
	
	/**
	 * Agrirouter request to set the application capabilities.
	 */
	private final ARCapabilities capabilities;
	
	/**
	 * Static only class.
	 */
	public AROnboarding(ARConfig config) {
		this.config = config;
		this.capabilities = new ARCapabilities(config.getApplicationId(), config.getVersionId())
				.setPushNotifications(config.getPushNotifications());
		config.getCapabilities().forEach(this.capabilities::addCapability);
	}

	/**
	 * Returns the MAC of the used network card.
	 * 
	 * @return MAC address
	 * @throws SocketException if an I/O error occurs.
	 * @throws UnknownHostException if the local host name could not be resolved into an address.
	 */
	public static String getMAC() throws SocketException, UnknownHostException {
		byte[] mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; ++i) {
			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
		}
		return sb.toString();
	}

	/**
	 * Performs an agrirouter onboarding for CUs. This is not allowed for web applications.
	 * 
	 * @param instanceId Unique instance id. This is prefixed by the "id" specified in the onboard settings.
	 * @param registrationCode registration code (TAN) from the agrirouter control center
	 * @return agrirouter connection information to pass to {@link ARConnection#ARConnection(JSONObject, java.util.concurrent.ScheduledExecutorService)}
	 * @throws IOException if an I/O error occurs
	 * @throws JSONException if the agrirouter answered with invalid json
	 * @throws ARException passes errors from the agrirouter
	 */
	public JSONObject onboardCU(String instanceId, String registrationCode) 
			throws IOException, JSONException, ARException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			JSONObject request = config.getRequest();
			request.put("id", request.getString("id") + instanceId);
			
			HttpPost post = new HttpPost(config.getCUOnboardUrl());
			post.addHeader("Authorization", "Bearer " + registrationCode);
			post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			post.setEntity(new StringEntity(request.toString(), ContentType.APPLICATION_JSON));

			JSONObject json;
			try (CloseableHttpResponse response = client.execute(post)) {
				if (response.getStatusLine().getStatusCode() != 201) {
					throw new ARException(response.getStatusLine().toString());
				}

				json = new JSONObject(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
			}

			return json;
		}
	}

	/**
	 * Returns an pre-filled agrirouter request to set the application capabilities.
	 * @return pre-filled agrirouter request to set the application capabilities
	 */
	public ARCapabilities getCapabilitieDeclaration() {
		return capabilities;
	}
	
	/**
	 * Returns the agrirouter configuration.
	 * @return agrirouter configuration
	 */
	public ARConfig getConfig() {
		return config;
	}
	
	/**
	 * Sign the input bytes with the application private key.
	 * 
	 * @param input bytes to sign
	 * @return SHA256 RSA signature
	 * @throws GeneralSecurityException if this java runtime doesn't know SHA256withRSA or the key is invalid.
	 */
	public byte[] signSHA256RSA(byte[] input) throws GeneralSecurityException {
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(config.getAppPrivateKey());
		sig.update(input);
		return sig.sign();
	}
	
	/**
	 * Verify a signature from the agrirouter.
	 * 
	 * @param input signed bytes
	 * @param signature SHA256 RSA signature of the signed bytes
	 * @return whether the signature is valid
	 * @throws GeneralSecurityException if this java runtime doesn't know SHA256withRSA or the key is invalid.
	 */
	public boolean verifyARSHA256RSA(byte[] input, byte[] signature) throws GeneralSecurityException {
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initVerify(config.getArPublicKey());
		sig.update(input);
		return sig.verify(signature);
	}
	
	/**
	 * Sign the input string with the application private key.
	 * 
	 * @param input string to sign
	 * @return SHA256 RSA signature
	 * @throws GeneralSecurityException if this java runtime doesn't know SHA256withRSA or the key is invalid.
	 * @see #signSHA256RSA(byte[])
	 */
	public byte[] signSHA256RSA(String input) throws GeneralSecurityException {
		return signSHA256RSA(input.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Verify a signature from the agrirouter.
	 * 
	 * @param input signed string
	 * @param signature SHA256 RSA signature of the signed string
	 * @return whether the signature is valid
	 * @throws GeneralSecurityException if this java runtime doesn't know SHA256withRSA or the key is invalid.
	 * @see #verifyARSHA256RSA(byte[], byte[])
	 */
	public boolean verifyARSHA256RSA(String input, byte[] signature) throws GeneralSecurityException {
		return verifyARSHA256RSA(input.getBytes(StandardCharsets.UTF_8), signature);
	}
	
	/**
	 * A HTTP DELETE with a body.
	 * The default HTTP DELETE doesn't allow setting the body.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
		public static final String METHOD_NAME = "DELETE";
	 
		public String getMethod() {
			return METHOD_NAME;
		}
	 
		public HttpDeleteWithBody(final URI uri) {
			super();
			setURI(uri);
		}

	}
	
	public static void arError(CloseableHttpResponse response) throws ARException {
		String message;
		try {
			String answer = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
			System.err.println(answer);
			message = new JSONObject(answer).getJSONObject("error").getString("message");
			if(DEBUG_MODE) {
				System.out.println("==================================== END ======================================");
			}
		} catch(Throwable e) {
			e.printStackTrace();
			System.err.println("Can't read agrirouter error message!");
			if(DEBUG_MODE) {
				System.out.println("==================================== END ======================================");
			}
			throw new ARException(response.getStatusLine().toString());
		}
		throw new ARException(message);
	}
	
	/**
	 * Delete the given endpoint from the agrirouter.
	 * 
	 * @param conn Endpoint connection.
	 * @param accountId Identifier of the associated agrirouter account
	 * @return true
	 * @throws ARException if the agrirouter returned any errors.
	 * @throws IOException if the request couln't be sent.
	 * @throws GeneralSecurityException if an error occurred while creating the signature.
	 */
	public boolean revoke(ARConnection conn, String accountId) 
			throws ARException, IOException, GeneralSecurityException {
		String body = new JSONObject()
				.put("accountId", accountId)
				.put("endpointIds", new JSONArray().put(conn.getEndpointId()))
				.put("UTCTimestamp", DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now()))
				.toString();
		
		HttpDeleteWithBody delete = new HttpDeleteWithBody(config.getRevokeUrl());
		delete.addHeader("X-Agrirouter-ApplicationId", config.getApplicationId());
		delete.addHeader("X-Agrirouter-Signature", 
				Hex.encodeHexString(signSHA256RSA(body)));
		delete.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		delete.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
		
		if(DEBUG_MODE) {
			System.out.println("================================== REVOKE =====================================");
			if(DEBUG_MODE_RAW) {
				System.out.println(delete.getURI().toString());
				System.out.println(Stream.of(delete.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
			}
			System.out.println(body);
		}
		
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			try (CloseableHttpResponse response = client.execute(delete)) {
				if(DEBUG_MODE) {
					System.out.println("================================== RESPONSE ===================================");
					System.out.println(response.getStatusLine().toString());
					if(DEBUG_MODE_RAW)
						System.out.println(Stream.of(response.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
				}
				if (response.getStatusLine().getStatusCode() != 204) 
					arError(response);
				else if(DEBUG_MODE && response.getEntity() != null) {
					System.out.println(IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8));
				}
				if(DEBUG_MODE) {
					System.out.println("==================================== END ======================================");
				}
			}
		}
		try {
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Creates and returns a new secure onboarding context.
	 * @return Newly created secure onboarding context
	 */
	public SecureOnboardingContext secureOnboard() {
		return new SecureOnboardingContext();
	}
	
	/**
	 * The secure onboarding context holds the state of the secure onboarding process.
	 * Secure onboarding process:
	 * 1. Redirect the user to the {@link #getVerifyUrl() agrirouter authentication website}.
	 * 2. When the user is redirected back to the apps website, {@link #readReturn(String, String, String) read the query parameters}.
	 * 3. {@link #onboard(String) Start the actual onboarding}.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class SecureOnboardingContext {

		protected final String state;
		
		protected String account = null, regcode = null;
		protected OffsetDateTime expires = null;
		
		/**
		 * Creates a new secure onboarding context.
		 */
		private SecureOnboardingContext() {
			this.state = UUID.randomUUID().toString();
		}
		
		/**
		 * Returns the random state identifier of this secure onboarding context.
		 * @return state identifier
		 */
		public String getState() {
			return state;
		}
		
		/**
		 * Returns the identifier of the agrirouter account of the user.
		 * This is not set until you called {@link #readReturn(String, String, String)}.
		 * @return agrirouter account id
		 */
		public String getAccount() {
			return account;
		}

		/**
		 * Returns the registration code (TAN) to onboard.
		 * This is not set until you called {@link #readReturn(String, String, String)}.
		 * @return onboarding registration code (TAN)
		 */
		public String getRegcode() {
			return regcode;
		}

		/**
		 * Returns the time when the registration code expires.
		 * This is not set until you called {@link #readReturn(String, String, String)}.
		 * @return expiration time of the registration code
		 */
		public OffsetDateTime getExpires() {
			return expires;
		}

		/**
		 * Creates the redirection URL for the user to the agrirouter authentication website.
		 * This is the first stage of the secure onboarding process.
		 * 
		 * @return redirection URL to the authentication website
		 * @throws URISyntaxException if the created URI is not valid
		 * @throws GeneralSecurityException if there was an error during creation of the signature
		 */
		public URI getVerifyUrl() throws URISyntaxException, GeneralSecurityException {
			URI uri = new URIBuilder(config.getRedirectUrl())
				.addParameter("response_type", "onboard")
				.addParameter("state", state)
				.build();
			if(DEBUG_MODE) {
				System.out.println("================================== VERIFY URL =================================");
				System.out.println(uri.toString());
			}
			return uri;
		}
		
		/**
		 * Read the answer from the agrirouter authentication website.
		 * This is the second stage of the secure onboarding process.
		 * 
		 * @param state state query parameter
		 * @param token token query parameter
		 * @param signature signature query parameter
		 * @return agrirouter account ID of the user
		 * @throws ARException if the answer from the agrirouter was not valid.
		 * @throws GeneralSecurityException if there was an internal error during verification of the signature.
		 */
		public String readReturn(String state, String token, String signature) 
				throws ARException, GeneralSecurityException {
			
			if(DEBUG_MODE) {
				System.out.println("================================== RESPONSE ===================================");
				System.out.println("state: " + state);
				System.out.println("token: " + token);
				System.out.println("signature: " + signature);
				System.out.println("==================================== END ======================================");
			}
			
			if(!this.state.equals(state))
				throw new ARException("Wrong state id");
			
			if(!verifyARSHA256RSA(state + token, Base64.getDecoder().decode(signature)))
				throw new ARException("Signature verification failed");
			
			try {
				byte[] decode = Base64.getDecoder().decode(token.getBytes(StandardCharsets.UTF_8));
				JSONObject json = new JSONObject(new String(decode, StandardCharsets.UTF_8));
				this.account = json.getString("account");
				this.regcode = json.optString("regcode", null);
				String exp = json.optString("expires", null);
				
				this.expires = exp != null ? OffsetDateTime.parse(exp) : null;
			} catch(JSONException | IllegalArgumentException e) {
				throw new ARException("Invalid token content", e);
			}
			
			return account;
		}
		
		/**
		 * Returns whether this secure onboarding context is ready to onboard.
		 * @return true if ready
		 */
		public boolean isReadyToOnboard() {
			return account != null && regcode != null && OffsetDateTime.now().isBefore(expires);
		}
		
		/**
		 * This is a method for testing where you can give the registration code directly.
		 * Normally you get it from stage two of the secure onboarding process.
		 * 
		 * @param instanceId unique identifier of this endpoint, can be the username
		 * @param regcode registration code (TAN) to use
		 * @return agrirouter connection information to pass to {@link ARConnection#ARConnection(JSONObject, java.util.concurrent.ScheduledExecutorService)}
		 * @throws IOException if an I/O error occurs
		 * @throws JSONException if the agrirouter answered with invalid json
		 * @throws ARException passes errors from the agrirouter
		 * @throws GeneralSecurityException if there was an error during creation of the signature
		 */
		public JSONObject onboard(String instanceId, String regcode) 
				throws IOException, JSONException, ARException, GeneralSecurityException {
			this.regcode = regcode;
			return onboard(instanceId);
		}
		
		/**
		 * Perform the actual onboarding using the information got during the first two stages of the secure onboarding process.
		 * This is the third stage of the secure onboarding process.
		 * 
		 * @param instanceId unique identifier of this endpoint, can be the username
		 * @return agrirouter connection information to pass to {@link ARConnection#ARConnection(JSONObject, java.util.concurrent.ScheduledExecutorService)}
		 * @throws IOException if an I/O error occurs
		 * @throws JSONException if the agrirouter answered with invalid json
		 * @throws ARException passes errors from the agrirouter
		 * @throws GeneralSecurityException if there was an error during creation of the signature
		 */
		public JSONObject onboard(String instanceId) 
				throws IOException, JSONException, ARException, GeneralSecurityException {
			try (CloseableHttpClient client = HttpClients.createDefault()) {
				JSONObject request = config.getRequest();
				String body = request
						.put("id", request.getString("id") + instanceId)
						.put("UTCTimestamp", DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now()))
						.put("timeZone", ZoneOffset.systemDefault().getRules().getOffset(Instant.now()).toString())
						.toString();
				
				HttpPost post = new HttpPost(config.getVerifyUrl());
				post.addHeader("Authorization", "Bearer " + regcode);
				post.addHeader("X-Agrirouter-ApplicationId", config.getApplicationId());
				post.addHeader("X-Agrirouter-Signature", 
						Hex.encodeHexString(signSHA256RSA(body)));
				post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
				post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
				
				if(DEBUG_MODE) {
					System.out.println("=================================== VERIFY ====================================");
					if(DEBUG_MODE_RAW) {
						System.out.println(post.getURI().toString());
						System.out.println(Stream.of(post.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
					}
					System.out.println(body);
				}
				
				JSONObject json;
				try (CloseableHttpResponse response = client.execute(post)) {
					if(DEBUG_MODE) {
						System.out.println("================================== RESPONSE ===================================");
						System.out.println(response.getStatusLine().toString());
						if(DEBUG_MODE_RAW)
							System.out.println(Stream.of(response.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
					}
					if (response.getStatusLine().getStatusCode() != 200) 
						arError(response);

					json = new JSONObject(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
					if(DEBUG_MODE) {
						System.out.println(json.toString());
						System.out.println("==================================== END ======================================");
					}
				}
				
				if(!json.getString("accountId").equals(account))
					throw new ARException("Onboarding account doesn't equal trust flow account.");
				
				post.setURI(config.getOnboardUrl());
				
				if(DEBUG_MODE) {
					System.out.println("================================== ONBOARD ====================================");
					if(DEBUG_MODE_RAW) {
						System.out.println(post.getURI().toString());
						System.out.println(Stream.of(post.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
					}
					System.out.println(body);
				}
				
				try (CloseableHttpResponse response = client.execute(post)) {
					if(DEBUG_MODE) {
						System.out.println("================================== RESPONSE ===================================");
						System.out.println(response.getStatusLine().toString());
						if(DEBUG_MODE_RAW)
							System.out.println(Stream.of(response.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
					}
					if (response.getStatusLine().getStatusCode() != 201) 
						arError(response);

					json = new JSONObject(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
					if(DEBUG_MODE) {
						System.out.println(json.toString());
						System.out.println("==================================== END ======================================");
					}
				}
				return json;
			}
		}
	}
}
