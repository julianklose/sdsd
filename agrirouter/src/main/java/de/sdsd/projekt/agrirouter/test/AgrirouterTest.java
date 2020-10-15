package de.sdsd.projekt.agrirouter.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.Message;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import de.sdsd.projekt.agrirouter.ARConfig;
import de.sdsd.projekt.agrirouter.ARConfig.ARConfigBuilder;
import de.sdsd.projekt.agrirouter.ARConfig.ARGateway;
import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.AROnboarding;
import de.sdsd.projekt.agrirouter.request.AREndpoint;
import de.sdsd.projekt.agrirouter.request.ARListEndpointsRequest;
import de.sdsd.projekt.agrirouter.request.ARSendMessage;
import de.sdsd.projekt.agrirouter.request.ARSubscription;
import de.sdsd.projekt.agrirouter.request.feed.ARConfirmMessage;
import de.sdsd.projekt.agrirouter.request.feed.ARDeleteMessage;
import de.sdsd.projekt.agrirouter.request.feed.ARMsg;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader.ARMsgHeaderResult;
import de.sdsd.projekt.agrirouter.request.feed.ARQueryMessageHeaders;
import de.sdsd.projekt.agrirouter.request.feed.ARQueryMessages;
import efdi.GrpcEfdi;
/**
 * Test class the run small tests of the agrirouter interface.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AgrirouterTest {

	/** The Constant RANDOM. */
	private static final Random RANDOM = new Random();
	
	/**
	 * Creates the AR config.
	 *
	 * @param ar the ar
	 * @return the AR config
	 * @throws Exception the exception
	 */
	public static ARConfig createARConfig(JSONObject ar) throws Exception {
		ARConfigBuilder config = new ARConfigBuilder()
				.setAgrirouterHost(ar.getString("host"))
				.setOnboardingUrl(ar.getString("onboardingUrl"))
				.setEndpointIdPrefix(ar.getString("endpointIdPrefix"))
				.setApplicationId(ar.getString("applicationId"))
				.setVersionId(ar.getString("certificationVersionId"))
				.setAppPrivateKey(ar.getString("appPrivateKey"))
				.setArPublicKey(ar.getString("arPublicKey"))
				.setGateway("MQTT".equalsIgnoreCase(ar.optString("gateway")) ? ARGateway.MQTT : ARGateway.REST)
				.setPushNotifications(ar.optBoolean("pushNotifications", false) ? PushNotification.ENABLED : PushNotification.DISABLED);
		JSONArray caps = ar.getJSONArray("capabilities");
		for(int i = 0; i < caps.length(); ++i) {
			JSONObject cap = caps.getJSONObject(i);
			config.addCapability(cap.getString("technicalMessageType"), cap.getInt("direction"));
		}
		return config.build();
	}
	
	/**
	 * Gets the saved connection.
	 *
	 * @param executor the executor
	 * @param connfile the connfile
	 * @return the saved connection
	 * @throws Exception the exception
	 */
	public static ARConnection getSavedConnection(ScheduledExecutorService executor, String connfile) throws Exception {
		File file = new File(connfile);
		if(!file.canRead()) throw new IOException("Can't read file " + connfile);
		JSONObject json = new JSONObject(FileUtils.readFileToString(file, "UTF-8"));
		return ARConnection.create(json, executor);
	}
	
	/**
	 * Save connection.
	 *
	 * @param onboardingResponse the onboarding response
	 * @param connfile the connfile
	 * @throws Exception the exception
	 */
	public static void saveConnection(JSONObject onboardingResponse, String connfile) throws Exception {
		File file = new File(connfile);
		if(!file.canWrite()) throw new IOException("Can't write to file " + connfile);
		FileUtils.writeStringToFile(file, onboardingResponse.toString(), "UTF-8");
	}
	
	/**
	 * Gets the endpoints.
	 *
	 * @param conn the conn
	 * @param considerRoutes the consider routes
	 * @param direction the direction
	 * @param messagetype the messagetype
	 * @return the endpoints
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static void getEndpoints(ARConnection conn, boolean considerRoutes, ARDirection direction, ARMessageType messagetype) 
			throws IOException, InterruptedException, ARException {
		System.out.println("Get Endpoints...");
		ARListEndpointsRequest request = new ARListEndpointsRequest().considerRoutingRules(considerRoutes);
		if(direction != null) request.setDirection(direction);
		if(messagetype != null) request.setMessageTypeFilter(messagetype);
		List<AREndpoint> response = request.send(conn, 3, TimeUnit.SECONDS);
		System.out.println("Found " + response.size() + " endpoints: ");
		System.out.println(response.stream().map(e -> e.getName()).collect(Collectors.joining(", ")));
		
	}

	/**
	 * Creates the payload.
	 *
	 * @param length the length
	 * @return the byte[]
	 */
	public static byte[] createPayload(int length) {
		byte[] out = new byte[length];
		RANDOM.nextBytes(out);
		return out;
	}
	
	/**
	 * Send message.
	 *
	 * @param conn the conn
	 * @param type the type
	 * @param filename the filename
	 * @param payload the payload
	 * @param publish the publish
	 * @param receiver the receiver
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static boolean sendMessage(ARConnection conn, ARMessageType type, 
			String filename, byte[] payload, boolean publish, String receiver) 
			throws IOException, InterruptedException, ARException {
		if(receiver == null || receiver.isEmpty()) {
			if(publish) System.out.println("Publish the message...");
			else throw new IOException("No receiver set");
		} else if(publish) System.out.println("Publish and send message to (" + receiver + ")...");
		else System.out.println("Send message to (" + receiver + ")...");
		
		ARSendMessage msg = new ARSendMessage()
			.setType(type)
			.setPayload(payload)
			.setTeamSetContextId(filename)
			.setPublish(publish);
		if(receiver != null && !receiver.isEmpty())
			msg.addRecipient(receiver);
		return msg.send(conn, 10, TimeUnit.SECONDS);
	}
	
	/**
	 * Send message.
	 *
	 * @param conn the conn
	 * @param type the type
	 * @param filename the filename
	 * @param payload the payload
	 * @param publish the publish
	 * @param receiver the receiver
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static boolean sendMessage(ARConnection conn, ARMessageType type, 
			String filename, Message payload, boolean publish, String receiver) 
			throws IOException, InterruptedException, ARException {
		if(receiver == null || receiver.isEmpty()) {
			if(publish) System.out.println("Publish the message...");
			else throw new IOException("No receiver set");
		} else if(publish) System.out.println("Publish and send message to (" + receiver + ")...");
		else System.out.println("Send message to (" + receiver + ")...");
		
		ARSendMessage msg = new ARSendMessage()
			.setType(type)
			.setPayload(payload)
			.setTeamSetContextId(filename)
			.setPublish(publish);
		if(receiver != null && !receiver.isEmpty())
			msg.addRecipient(receiver);
		return msg.send(conn, 10, TimeUnit.SECONDS);
	}
	
	/**
	 * Receive headers.
	 *
	 * @param conn the conn
	 * @return the AR msg header result
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static ARMsgHeaderResult receiveHeaders(ARConnection conn) 
			throws IOException, InterruptedException, ARException {
		System.out.println("Receiving messages...");
		ARMsgHeaderResult headers = new ARQueryMessageHeaders()
				.setGetAllFilter()
				.send(conn, 3, TimeUnit.SECONDS);
		System.out.println(headers.size() + " messages found!");
		return headers;
	}
	
	/**
	 * Confirm pending messages.
	 *
	 * @param conn the conn
	 * @param headers the headers
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static void confirmPendingMessages(ARConnection conn, ARMsgHeaderResult headers) 
			throws IOException, InterruptedException, ARException {
		if(!headers.getPendingMessageIds().isEmpty()) {
			Messages resp = new ARConfirmMessage()
					.addMessages(headers.getPendingMessageIds())
					.send(conn, 3, TimeUnit.SECONDS);
			System.out.println(resp);
		}
	}
	
	/**
	 * Receive messages.
	 *
	 * @param conn the conn
	 * @param headers the headers
	 * @return the list
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static List<ARMsg> receiveMessages(ARConnection conn, List<ARMsgHeader> headers) 
			throws IOException, InterruptedException, ARException {
		List<ARMsgHeader> completeHeaders = headers.stream()
				.filter(ARMsgHeader::isComplete)
				.collect(Collectors.toList());
		List<ARMsg> msgs = completeHeaders.isEmpty() ? Collections.emptyList() : new ARQueryMessages()
				.addMessageFilter(completeHeaders)
				.send(conn, 180, TimeUnit.SECONDS);
		System.out.println("Received and confirmed " + msgs.size() + " messages successfully.");
		return msgs;
	}
	
	/**
	 * Clear message queue.
	 *
	 * @param conn the conn
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static void clearMessageQueue(ARConnection conn) 
			throws IOException, InterruptedException, ARException {
		System.out.println("Deleting messages...");
		List<ARMsgHeader> headers = new ARQueryMessageHeaders()
				.setValidityFilter(Instant.now().minus(27, ChronoUnit.DAYS), Instant.now())
				.send(conn, 3, TimeUnit.SECONDS);
		System.out.println(headers.size() + " messages found!");
		
		Messages resp = new ARDeleteMessage()
				.addMessages(headers)
				.send(conn, 3, TimeUnit.SECONDS);
		System.out.println(resp.getMessagesList().stream()
				.filter(msg -> "VAL_000209".equals(msg.getMessageCode()))
				.count() + " messages deleted!");
	}
	
	/**
	 * Send subscriptions.
	 *
	 * @param conn the conn
	 * @param types the types
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 * @throws ARException the AR exception
	 */
	public static void sendSubscriptions(ARConnection conn, ARMessageType ...types) 
			throws IOException, InterruptedException, ARException {
		System.out.print("Send subscriptions: (" + 
				Stream.of(types)
				.map(ARMessageType::technicalMessageType)
				.collect(Collectors.joining(", ")) + ")...");
		boolean ok = new ARSubscription(Arrays.asList(types))
				.send(conn, 3, TimeUnit.SECONDS);
		System.out.println(ok ? "OK" : "FAILURE");
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Agrirouter Test");
			JSONObject appSettings = new JSONObject(FileUtils.readFileToString(new File("settings.json"), StandardCharsets.UTF_8));
			JSONObject ar = appSettings.getJSONObject("agrirouter");
			ARConfig config = createARConfig(appSettings.getJSONObject("agrirouter"));
			AROnboarding onboarding = new AROnboarding(config);
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			try(ARConnection conn = getSavedConnection(executor, "arconn_test.json")) {
				long t1 = System.currentTimeMillis();
				
				try(InputStream in = FileUtils.openInputStream(new File("parser/DeviceDescription.bin"))) {
					sendMessage(conn, ARMessageType.DEVICE_DESCRIPTION, "TeamSetContextId", GrpcEfdi.ISO11783_TaskData.parseFrom(in), true, null);
				}
//				onboarding.getCapabilitieDeclaration().send(conn, 3, TimeUnit.SECONDS);
//				sendSubscriptions(conn, ARMessageType.TASKDATA);
//				sendMessage(conn, ARMessageType.TASKDATA, "directtest.txt", "häölüüüß".getBytes(StandardCharsets.ISO_8859_1), true, null);
//				getEndpoints(conn, true, ARDirection.SEND_RECEIVE, null);
//				sendMessage(conn, ARMessageType.TASKDATA, "publishtest.zip", createPayload(100000), true, null);
//				sendMessage(conn, ARMessageType.TASKDATA, "publishdirecttest.zip", createPayload(100000), true, "7577ffac-0973-4bfd-b856-c72347974e70");
//				ARMsgHeaderResult headers = receiveHeaders(conn);
//				List<ARMsg> msgs = receiveMessages(conn, headers);
//				System.out.format("%d < %d\n", headers.getSingleMessageCount(), headers.getTotalMessagesInQuery());
//				msgs.stream().map(ARMsg::getContent).map(bytes -> new String(bytes, StandardCharsets.ISO_8859_1)).forEach(System.out::println);
//				confirmPendingMessages(conn, headers);
//				clearMessageQueue(conn);
				System.out.println("Time needed: " + (System.currentTimeMillis() - t1) + " ms.");
			} catch (ARException e) {
				System.err.println(e.getLocalizedMessage());
				for(Message msg : e)
					System.err.print(msg.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				executor.shutdown();
				if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					System.out.println("Still waiting...");
					System.exit(0);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
