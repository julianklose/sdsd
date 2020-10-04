package de.sdsd.projekt.prototype.applogic;

import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.client.model.Updates;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.commons.MessageOuterClass.Metadata;
import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import de.sdsd.projekt.agrirouter.ARConfig;
import de.sdsd.projekt.agrirouter.ARConfig.ARConfigBuilder;
import de.sdsd.projekt.agrirouter.ARConfig.ARGateway;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.AROnboarding;
import de.sdsd.projekt.agrirouter.AROnboarding.SecureOnboardingContext;
import de.sdsd.projekt.agrirouter.request.ARCapabilities;
import de.sdsd.projekt.agrirouter.request.ARCapability;
import de.sdsd.projekt.agrirouter.request.AREndpoint;
import de.sdsd.projekt.agrirouter.request.ARListEndpointsRequest;
import de.sdsd.projekt.agrirouter.request.ARSendMessage;
import de.sdsd.projekt.agrirouter.request.ARSubscription;
import de.sdsd.projekt.agrirouter.request.feed.ARDeleteMessage;
import de.sdsd.projekt.agrirouter.request.feed.ARMsg;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader.ARMsgHeaderResult;
import de.sdsd.projekt.agrirouter.request.feed.ARQueryMessageHeaders;
import de.sdsd.projekt.agrirouter.request.feed.ARQueryMessages;
import de.sdsd.projekt.prototype.data.ARCaps;
import de.sdsd.projekt.prototype.data.ARConn;
import de.sdsd.projekt.prototype.data.AREndpointStore;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import efdi.GrpcEfdi;

/**
 * Provides all agrirouter functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AgrirouterFunctions {
	public static final int TIMEOUT_SECONDS = 9;
	
	private final ApplicationLogic app;
	private final AROnboarding onboardingRest, onboardingRestQA, onboardingMqtt, onboardingMqttQA;
	
	AgrirouterFunctions(ApplicationLogic app) throws Exception {
		this.app = app;
		this.onboardingRest = createArOnboarding(app.settings.getJSONObject("agrirouter"), ARGateway.REST);
		this.onboardingRestQA = createArOnboarding(app.settings.getJSONObject("agrirouter-qa"), ARGateway.REST);
		this.onboardingMqtt = createArOnboarding(app.settings.getJSONObject("agrirouter"), ARGateway.MQTT);
		this.onboardingMqttQA = createArOnboarding(app.settings.getJSONObject("agrirouter-qa"), ARGateway.MQTT);
	}
	
	public AROnboarding getOnboarding(User user) throws SDSDException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			throw new SDSDException("Not onboarded");
		if(ar.isMQTT()) return ar.isQA() ? onboardingMqttQA : onboardingMqtt;
		else return ar.isQA() ? onboardingRestQA : onboardingRest;
	}
	
	private AROnboarding createArOnboarding(JSONObject cfg, ARGateway gateway) throws Exception {
		ARConfigBuilder config = new ARConfigBuilder()
				.setAgrirouterHost(cfg.getString("host"))
				.setOnboardingUrl(cfg.getString("onboardingUrl"))
				.setEndpointIdPrefix(cfg.getString("endpointIdPrefix"))
				.setApplicationId(cfg.getString("applicationId"))
				.setVersionId(cfg.getString("certificationVersionId"))
				.setAppPrivateKey(cfg.getString("appPrivateKey"))
				.setArPublicKey(cfg.getString("arPublicKey"))
				.setGateway(gateway)
				.setPushNotifications(gateway == ARGateway.MQTT 
						? PushNotification.ENABLED : PushNotification.DISABLED);
		JSONArray caps = cfg.getJSONArray("capabilities");
		for(int i = 0; i < caps.length(); ++i) {
			JSONObject cap = caps.getJSONObject(i);
			config.addCapability(cap.getString("technicalMessageType"), cap.getInt("direction"));
		}
		return new AROnboarding(config.build());
	}
	
	public boolean checkStateSignature(String state, String signature) throws GeneralSecurityException {
		return DigestUtils.md5Hex(onboardingRest.signSHA256RSA(state)).equalsIgnoreCase(signature);
	}
	
	public URI startSecureOnboarding(User user, boolean qa, boolean mqtt) throws URISyntaxException, GeneralSecurityException {
		AROnboarding onboarding;
		if(mqtt) onboarding = qa ? onboardingMqttQA : onboardingMqtt;
		else onboarding = qa ? onboardingRestQA : onboardingRest;
		
		List<ARCaps> capslist = app.list.capabilities.getList(user);
		if(capslist.size() > 0) {
			ARCaps caps = capslist.get(0);
			if(caps.getPushNotification() != onboarding.getConfig().getPushNotifications())
				app.list.capabilities.update(user, caps, caps.setPushNotification(onboarding.getConfig().getPushNotifications()));
		}
		
		SecureOnboardingContext soc = onboarding.secureOnboard();
		user.setSecureOnboardingContext(soc);
		URI verifyUrl = soc.getVerifyUrl();
		String redirectToLocalhost = System.getProperty("redirectToLocalhost");
		if(redirectToLocalhost != null) 
			verifyUrl = new URIBuilder(verifyUrl).addParameter("redirect_uri", redirectToLocalhost 
					+ "/" + DigestUtils.md5Hex(onboarding.signSHA256RSA(soc.getState()))).build();
		return verifyUrl;
	}
	
	public void reconnect(User user) throws MqttException, SDSDException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			throw new SDSDException("Not onboarded");
		user.closeAgrirouterConn();
		ar.conn();
	}
	
	public URI reonboard(User user) throws URISyntaxException, GeneralSecurityException, SDSDException {
		if(user.agrirouter() == null)
			throw new SDSDException("Not onboarded");
		AROnboarding onboarding = getOnboarding(user);
		SecureOnboardingContext soc = onboarding.secureOnboard();
		user.setSecureOnboardingContext(soc);
		URI verifyUrl = soc.getVerifyUrl();
		String redirectToLocalhost = System.getProperty("redirectToLocalhost");
		if(redirectToLocalhost != null) 
			verifyUrl = new URIBuilder(verifyUrl).addParameter("redirect_uri", redirectToLocalhost 
					+ "/" + DigestUtils.md5Hex(onboarding.signSHA256RSA(soc.getState()))).build();
		return verifyUrl;
	}
	
	public boolean secureOnboard(User user) 
			throws JSONException, GeneralSecurityException, IOException, ARException, MqttException {
		SecureOnboardingContext soc = user.getSecureOnboardingContext();
		if(soc != null && soc.isReadyToOnboard()) {
			try {
				Bson update = user.setAgrirouter(soc.getAccount(), soc.onboard(user.getName()));
				
				try {
					for (int i = 0; i < 3; ++i) {
						try {
							if(sendCapabilities(user)) break;
						} catch (ARException e) {
							if(i < 2) System.err.println(e.getMessage());
							else throw e;
						} catch (InterruptedException e) {
							if(i < 2) e.printStackTrace();
							else throw new IOException(e);
						}
					}
				} catch(ARException | IOException e) {
					user.clearAgrirouterConn();
					throw e;
				}
				
				app.user.updateUser(user, update);
				readEndpoints(user);
				return true;
			} finally {
				user.setSecureOnboardingContext(null);
			}
		}
		else return false;
	}
	
	protected boolean sendCapabilities(User user) throws IOException, InterruptedException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return false;
		try {
			List<ARCaps> caps = app.list.capabilities.getList(user);
			if(caps.isEmpty()) {
				ARCapabilities request = getOnboarding(user).getCapabilitieDeclaration();
				return ar.conn().sendRequest(request, TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} else
				return sendCapabilities(user, caps.get(0));
		} catch (SDSDException e) {
			return false;
		}
	}
	
	protected boolean sendCapabilities(User user, ARCaps caps) throws IOException, InterruptedException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return false;
		try {
			ARConfig config = getOnboarding(user).getConfig();
			ARCapabilities request = new ARCapabilities(config.getApplicationId(), config.getVersionId());
			caps.getCapabilities().forEach(request::addCapability);
			request.setPushNotifications(caps.getPushNotification());
			return ar.conn().sendRequest(request, TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (SDSDException e) {
			return false;
		}
	}
	
	public void secureOnboard(User user, String state, String token, String signature) 
			throws ARException, GeneralSecurityException, JSONException, IOException {
		SecureOnboardingContext soc = user.getSecureOnboardingContext();
		if(soc != null)
			soc.readReturn(state, token, signature);
		else
			throw new ARException("No onboarding request sent.");
	}
	
	public boolean offboard(User user) throws IOException {
		final ARConn ar = user.agrirouter();
		if(ar != null) {
			try {
				getOnboarding(user).revoke(ar.conn(), ar.getAccountId());
			} catch (ARException e) {
				System.err.println(e.getMessage());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		new AREndpointStore(app.redis, user).clear();
		return app.user.updateUser(user, user.clearAgrirouterConn());
	}
	
	public List<AREndpoint> getCachedEndpoints(User user) {
		return app.list.endpoints.getList(user);
	}
	
	public CompletableFuture<List<AREndpoint>> readEndpoints(User user) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if (ar == null)
			return CompletableFuture.completedFuture(Collections.emptyList());
		
		CompletableFuture<List<AREndpoint>> pendingEndpoints = user.getPendingEndpoints();
		if(pendingEndpoints != null) 
			return pendingEndpoints;
		else {
			ARListEndpointsRequest request = new ARListEndpointsRequest();
			
			CompletableFuture<List<AREndpoint>> list = ar.conn()
					.sendRequestAsync(request, TIMEOUT_SECONDS, TimeUnit.SECONDS);
			
			CompletableFuture<List<AREndpoint>> routes = ar.conn().sendRequestAsync(
					request.considerRoutingRules(true).setDirection(ARDirection.SEND_RECEIVE), 
					TIMEOUT_SECONDS, TimeUnit.SECONDS);
			
			CompletableFuture<List<AREndpoint>> future = list.thenApply(l -> processEndpoints(user, l, routes));
			user.setPendingEndpoints(future);
			return future;
		}
	}
	
	private List<AREndpoint> processEndpoints(User user, List<AREndpoint> list, CompletableFuture<List<AREndpoint>> routes) {
		Map<String, AREndpoint> ep = new HashMap<>();
		for(AREndpoint e : list) {
			e.clearCapabilities();
			ep.put(e.getId(), e);
		}
		for(AREndpoint e : routes.join()) {
			ep.put(e.getId(), e);
		}
		
		AREndpointStore store = new AREndpointStore(app.redis, user);
		store.clear();
		store.storeAll(ep.values());
		return ep.values().stream().sorted().collect(toList());
	}
	
	public CompletableFuture<Boolean> sendFile(User user, File file, boolean publish, List<String> targets) 
			throws FileNotFoundException, ARException, IOException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(false);
		byte[] content = app.file.downloadFile(user, file);
		ARMessageType artype = app.list.types.get(null, file.getType()).getARType();
		return new ARSendMessage()
				.setTeamSetContextId(file.getFilename())
				.setMetadata(Metadata.newBuilder()
						.setFileName(file.getFilename())
						.build())
				.setType(artype)
				.setPayload(content)
				.addAllRecipients(targets)
				.setPublish(publish)
				.sendAsync(ar.conn(), (file.getSize() / ARSendMessage.MAX_MESSAGE_SIZE + 1) * TIMEOUT_SECONDS , TimeUnit.SECONDS);
	}
	
	public CompletableFuture<Boolean> sendDeviceDescription(User user, GrpcEfdi.ISO11783_TaskData deviceDescription, String contextId) 
			throws ARException, IOException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(false);
		return new ARSendMessage()
				.setTeamSetContextId(contextId)
				.setType(ARMessageType.DEVICE_DESCRIPTION)
				.setPayload(deviceDescription)
				.setPublish(true)
				.sendAsync(ar.conn(), TIMEOUT_SECONDS , TimeUnit.SECONDS);
	}
	
	public CompletableFuture<Boolean> sendTimelog(User user, GrpcEfdi.TimeLog timelog, String contextId) 
			throws ARException, IOException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(false);
		return new ARSendMessage()
				.setTeamSetContextId(contextId)
				.setType(ARMessageType.TIME_LOG)
				.setPayload(timelog)
				.setPublish(true)
				.sendAsync(ar.conn(), timelog.getTimeCount() > 20 ? 6 * TIMEOUT_SECONDS : TIMEOUT_SECONDS , TimeUnit.SECONDS);
	}
	
	public CompletableFuture<ARMsgHeaderResult> readAllMessageHeaders(User user) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(new ARMsgHeaderResult());
		return new ARQueryMessageHeaders()
				.setGetAllFilter()
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<ARMsgHeaderResult> readMessageHeaders(User user, Instant from, Instant until) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(new ARMsgHeaderResult());
		return new ARQueryMessageHeaders()
				.setValidityFilter(from, until)
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<ARMsgHeaderResult> readMessageHeaders(User user, AREndpoint sender) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(new ARMsgHeaderResult());
		return new ARQueryMessageHeaders()
				.addSenderFilter(sender.getId())
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<ARMsgHeaderResult> readMessageHeaders(User user, AREndpoint sender, Instant from, Instant until) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null)
			return CompletableFuture.completedFuture(new ARMsgHeaderResult());
		return new ARQueryMessageHeaders()
				.addSenderFilter(sender.getId())
				.setValidityFilter(from, until)
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<Messages> deleteMessages(final User user, List<ARMsgHeader> headers) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(headers.isEmpty() || ar == null) 
			return CompletableFuture.completedFuture(Messages.getDefaultInstance());
		return new ARDeleteMessage()
				.addMessages(headers)
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<Messages> clearFeed(User user) throws IOException, ARException {
		final ARConn ar = user.agrirouter();
		if(ar == null) 
			return CompletableFuture.completedFuture(Messages.getDefaultInstance());
		return new ARDeleteMessage()
				.setValidityFilter(Instant.now().minus(27, ChronoUnit.DAYS), Instant.now())
				.sendAsync(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}
	
	private static final Pattern ISODATE_REGEX = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z_?");
	public CompletableFuture<List<ReceivedMessageResult>> receiveMessages(final User user, List<ARMsgHeader> headers) 
			throws ARException, IOException {
		final ARConn ar = user.agrirouter();
		if(ar == null || headers.isEmpty())
			return CompletableFuture.completedFuture(Collections.emptyList());
		if(headers.stream().filter(h -> h.getIds().isEmpty() || !h.isComplete()).findAny().isPresent()) 
			throw new ARException("At least one message is incomplete or corrupted");
		
		return new ARQueryMessages()
				.addMessageFilter(headers)
				.sendAsync(ar.conn(), 
						headers.stream().mapToInt(ARMsgHeader::getChunkCount).sum() * TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.thenApply(msgs -> msgs.stream().map(msg -> handleReceivedMessage(user, msg)).collect(toList()));
	}
	
	public void onPushNotificationMsg(User user, ARMsg msg) {
		ReceivedMessageResult res = handleReceivedMessage(user, msg);
		if(res.isSaved()) {
			if(res.isNew())
				app.logInfo(user, "Received file: \"" + res.getName() + "\" from " 
						+ app.list.endpoints.get(user, msg.header.getSender()).getName());
		}
		else if(res.isError())
			onPushNotificationError(user, res.getError());
		else {
			System.out.println("ARReceive: Discarded file because of missing storage task.");
			app.logInfo(user, "Discarded file because of missing storage task");
		}
	}
	
	public void onPushNotificationError(User user, Throwable error) {
		System.err.println(user.getName() + ": PushNotificationError: " + error.getMessage());
		error.printStackTrace();
		app.logError(user, "PushNotificationError: " + error.getLocalizedMessage());
	}
	
	/**
	 * Information of received messages.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class ReceivedMessageResult {
		protected final String name;
		protected final boolean isNew;
		protected final Throwable error;
		
		public ReceivedMessageResult(@Nullable String name, boolean isNew) {
			this.name = name;
			this.isNew = isNew;
			this.error = null;
		}
		public ReceivedMessageResult(@Nullable Throwable error) {
			this.name = null;
			this.isNew = false;
			this.error = error;
		}
		
		public boolean isSaved() {
			return name != null;
		}
		public boolean isError() {
			return error != null;
		}
		public boolean isNew() {
			return isNew;
		}
		public String getName() {
			return name != null ? name : "";
		}
		public Throwable getError() {
			return error != null ? error : new Exception();
		}
	}
	
	private ReceivedMessageResult handleReceivedMessage(User user, ARMsg msg) {
		try {
			if(msg.header.getType() == ARMessageType.DEVICE_DESCRIPTION) {
				String contextId = msg.header.getTeamSetContextId();
				if(contextId == null || contextId.isEmpty()) 
					throw new ARException("Received device description without team set context id");
				app.file.deviceDescriptions.store(user, contextId, msg.getContent());
				return new ReceivedMessageResult("DeviceDescription: " + contextId, true);
			} else if(msg.header.getType() == ARMessageType.TIME_LOG) {
				String contextId = msg.header.getTeamSetContextId();
				if(contextId == null || contextId.isEmpty()) 
					throw new ARException("Received timelog without team set context id");
				File file = app.file.storeTimeLog(user, contextId, msg.getContent(), 
						msg.header.getSentTime(), msg.header.getSender());
				if(file != null)
					return new ReceivedMessageResult("TimeLog: " + file.getFilename(), msg.header.getSentTime().equals(file.getCreated()));
				else
					return new ReceivedMessageResult(null, false);
			} else if(msg.header.getType() == ARMessageType.GPS) {
				File file = app.file.storeGpsInfo(user, msg.getContent(), 
						msg.header.getSentTime(), msg.header.getSender());
				if(file != null)
					return new ReceivedMessageResult("GpsInfo: " + file.getFilename(), msg.header.getSentTime().equals(file.getCreated()));
				else
					return new ReceivedMessageResult(null, false);
			} else {
				String filename;
				Metadata metadata = msg.header.getMetadata();
				if(metadata != null && !metadata.getFileName().isEmpty())
					filename = metadata.getFileName();
				else {
					filename = DateTimeFormatter.ISO_OFFSET_DATE_TIME
							.format(msg.header.getSentTime().truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC));
					if(!msg.header.getTeamSetContextId().isEmpty()) {
						String tscid = ISODATE_REGEX.matcher(msg.header.getTeamSetContextId()).replaceAll("");
						filename += '_' + tscid;
					}
				}
				if(filename.length() > 250)
					filename = filename.substring(0, 250);
				
				File file = app.file.storeFile(user, filename, msg.getContent(), 
						msg.header.getSentTime(), msg.header.getSender(), msg.header.getType());
				return new ReceivedMessageResult(file != null ? file.getFilename() : null, file != null);
			}
		} catch (InvalidProtocolBufferException e) {
			return new ReceivedMessageResult(new ARException("Invalid protobuf in " + msg.header.getType().toString()));
		} catch (Throwable e) {
			return new ReceivedMessageResult(e);
		} 
	}
	
	public boolean setSubscriptions(User user, String[] technicalMessageTypes) 
			throws IOException, InterruptedException, ARException {
		Set<ARMessageType> types = Stream.of(technicalMessageTypes)
				.map(ARMessageType::from).collect(Collectors.toSet());
		final ARConn ar = user.agrirouter();
		if(ar != null && new ARSubscription(types).send(ar.conn(), TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			return app.user.updateUser(user, ar.setSubscriptions(types));
		}
		else
			return false;
	}
	
	public boolean setCapabilities(User user, Map<ARMessageType, ARDirection> capabilities, PushNotification pushNotifications) 
			throws IOException, InterruptedException, ARException, SDSDException {
		List<ARCaps> list = app.list.capabilities.getList(user);
		if(list.isEmpty()) {
			if(capEquals(getOnboarding(user).getConfig(), capabilities, pushNotifications)) return false;
			Document create = ARCaps.create(user, capabilities, pushNotifications);
			ARCaps caps = new ARCaps(create);
			if(!sendCapabilities(user, caps)) return false;
			app.list.capabilities.add(user, create);
			return true;
		} else {
			ARCaps caps = list.get(0);
			Bson update = null;
			if(!capabilities.equals(caps.getCapabilitieMap()))
				update = caps.setCapabilities(capabilities);
			if(caps.getPushNotification() != pushNotifications) {
				Bson upd = caps.setPushNotification(pushNotifications);
				update = update != null ? Updates.combine(update, upd) : upd;
			}
			if(update == null) return false;
			if(!sendCapabilities(user, caps)) return false;
			return app.list.capabilities.update(user, caps, update);
		}
	}
	
	private boolean capEquals(ARConfig config, Map<ARMessageType, ARDirection> capabilities, PushNotification pushNotifications) {
		if(config.getPushNotifications() != pushNotifications) return false;
		if(config.getCapabilities().size() != capabilities.size()) return false;
		for(ARCapability c : config.getCapabilities()) {
			ARDirection dir = capabilities.get(c.getType());
			if(c.getDirection() != dir) return false;
		}
		return true;
	}

}
