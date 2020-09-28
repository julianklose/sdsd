package de.sdsd.projekt.agrirouter;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;
import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE_RAW;
import static de.sdsd.projekt.agrirouter.ARConfig.NATIVE_PROTOBUF_REST;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import com.sap.iotservices.common.protobuf.gateway.CommandResponseListProtos.CommandResponseList;
import com.sap.iotservices.common.protobuf.gateway.CommandResponseMessageProtos.CommandResponseMessage;
import com.sap.iotservices.common.protobuf.gateway.CommandResponseProtos.CommandResponse;
import com.sap.iotservices.common.protobuf.gateway.MeasureProtos.MeasureRequest;
import com.sap.iotservices.common.protobuf.gateway.MeasureProtos.MeasureRequest.Measure;
import com.sap.iotservices.common.protobuf.gateway.MeasureRequestMessageProtos.MeasureRequestMessage;

import de.sdsd.projekt.agrirouter.ARConnection.RestConnection;
import de.sdsd.projekt.agrirouter.ARMessage.Response;

/**
 * Intern class for sending request to the agrirouter and receiving the results.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
class ARRestSender extends ARSender {

	/**
	 * Agrirouter connection (endpoint) to use for all requests.
	 */
	private final RestConnection conn;
	/**
	 * Universal get request to obtain all agrirouter results.
	 */
	private final HttpGet httpget;
	/**
	 * Executor that runs the response waiter.
	 * @see #startExecutorIfNotRunning()
	 * @see #stopExecutorIfDone()
	 * @see #responseGetter
	 */
	private ScheduledFuture<?> ex = null;

	/**
	 * Constructor
	 * @param executor executor service with thread pool to use for receiving responses from the agrirouter. Doesn't need more than one thread.
	 * @param conn agrirouter connection (endpoint) to use for all requests.
	 */
	public ARRestSender(ScheduledExecutorService executor, RestConnection conn) {
		super(executor);
		this.conn = conn;
		this.httpget = new HttpGet(conn.getUrlResult());
		if(NATIVE_PROTOBUF_REST) {
			this.httpget.addHeader("Content-Type", "application/x-protobuf");
			this.httpget.addHeader("Accept", "application/x-protobuf");
		}
		else {
			this.httpget.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			this.httpget.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
		}
	}

	/**
	 * Start the {@link #responseGetter} if there are {@link #pendingRequests}.
	 */
	protected synchronized void startExecutorIfNotRunning() {
		if ((ex == null || ex.isCancelled() || ex.isDone()) && !pendingRequests.isEmpty()) {
			ex = executor.scheduleWithFixedDelay(responseGetter, 0, 333, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Stop the {@link #responseGetter} if there are no more {@link #pendingRequests}.
	 */
	protected synchronized void stopExecutorIfDone() {
		if (ex != null && pendingRequests.isEmpty()) {
			ex.cancel(false);
		}
	}
	
	@Override
	protected ResponseFuture sendMsg(ARMessage message, RequestFuture rf) throws IOException, ARException {
		String messageId = message.getHeader().getApplicationMessageId();
		rf.setCurrentMessage(messageId);

		if (DEBUG_MODE) {
			System.out.println("==================================== SEND =====================================");
			System.out.println(message.toString());
			if(!DEBUG_MODE_RAW)
				System.out.println("==================================== END ======================================");
		}
		
		HttpPost post = new HttpPost(conn.getUrlRequest());
		if(NATIVE_PROTOBUF_REST) {
			MeasureRequestMessage msg = MeasureRequestMessage.newBuilder().setMessage(message.toByteString()).build();
			StringValue timestamp = StringValue.newBuilder().setValue(Long.toString(System.currentTimeMillis())).build();
			MeasureRequest measure = MeasureRequest.newBuilder()
					.setCapabilityAlternateId(conn.getCapabilityAlternateId())
					.setSensorAlternateId(conn.getEndpointId())
					.setTimestamp(System.currentTimeMillis())
					.addMeasures(Measure.newBuilder()
							.addValues(Any.pack(msg, "message"))
							.addValues(Any.pack(timestamp, "timestamp"))
							.build())
					.build();
			
			
			post.addHeader("Content-Type", "application/x-protobuf");
			post.setEntity(new ByteArrayEntity(measure.toByteArray(), ContentType.create("application/x-protobuf")));
			
			if(DEBUG_MODE_RAW) {
				System.out.println("==================================== RAW ======================================");
				System.out.println(post.getURI().toString());
				System.out.println(Stream.of(post.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
				System.out.println(measure.toString());
			}
		} else {
			JSONObject measure = new JSONObject()
					.put("sensorAlternateId", conn.getEndpointId())
					.put("capabilityAlternateId", conn.getCapabilityAlternateId())
					.put("measures", new JSONArray().put(new JSONArray()
						.put(Base64.getEncoder().encodeToString(message.toByteString().toByteArray()))
						.put(Long.toString(System.currentTimeMillis()))));
			
			post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			post.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
			post.setEntity(new StringEntity(measure.toString(), ContentType.APPLICATION_JSON));
			
			if(DEBUG_MODE_RAW) {
				System.out.println("==================================== RAW ======================================");
				System.out.println(post.getURI().toString());
				System.out.println(Stream.of(post.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
				System.out.println(measure.toString());
			}
		}

		ResponseFuture future = new ResponseFuture(rf.req);
		responseMap.put(messageId, future);

		try (CloseableHttpResponse response = conn.send(post)) {
//			System.out.format("Message %s sent: %d\n", messageId, System.currentTimeMillis());
			if(DEBUG_MODE_RAW) {
				System.out.println("================================== RESPONSE ===================================");
				System.out.println(response.getStatusLine().toString());
				System.out.println(Stream.of(post.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
			}
			if (response.getStatusLine().getStatusCode() >= 300 || response.getStatusLine().getStatusCode() < 200) {
				System.err.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
				if(DEBUG_MODE_RAW) {
					System.out.println("==================================== END ======================================");
				}
				throw new ARException(response.getStatusLine().toString());
			}
			if(DEBUG_MODE_RAW) {
				if(response.getEntity() != null)
					System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
				System.out.println("==================================== END ======================================");
			}
		} catch (Throwable e) {
			responseMap.remove(messageId);
			throw e;
		}

		startExecutorIfNotRunning();
		return future;
	}

	/**
	 * Runnable to see if there are any responses of the agrirouter.
	 * This runnable is called repeatedly by the {@link #ex scheduled executor}.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private final Runnable responseGetter = new Runnable() {
		@Override
		public void run() {
			while (!ex.isDone()) { // get all available messages from the outbox
				try (CloseableHttpResponse response = conn.send(httpget)) {
					if (response.getStatusLine().getStatusCode() == 200) {
						List<Response> list = new ArrayList<>(1);
						if(NATIVE_PROTOBUF_REST) {
							CommandResponseList proto = CommandResponseList.parseFrom(response.getEntity().getContent());
							if(DEBUG_MODE_RAW) {
								System.out.println("=================================== OUTBOX ====================================");
								System.out.println(Stream.of(response.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
								System.out.println(proto.toString());
								System.out.println("==================================== END ======================================");
							}
							for(CommandResponse cmdresp : proto.getCommandsList()) {
								ByteString bytes = CommandResponseMessage.parseFrom(cmdresp.getCommand().getValues(0).getValue()).getMessage();
								list.add(new Response(bytes.newInput()));
							}
						}
						else {
							JSONArray json = new JSONArray(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
							if(DEBUG_MODE_RAW) {
								System.out.println("=================================== OUTBOX ====================================");
								System.out.println(Stream.of(response.getAllHeaders()).map(h -> h.toString()).collect(Collectors.joining("\n")));
								System.out.println(json.toString());
								System.out.println("==================================== END ======================================");
							}
							for(int i = 0; i < json.length(); ++i) {
								String b64resp = json.getJSONObject(i).getJSONObject("command").getString("message");
								list.add(new Response(new ByteArrayInputStream(Base64.getDecoder().decode(b64resp))));
							}
						}
						
						if(list.isEmpty()) break;
						
						for(Response resp : list) {
							if (DEBUG_MODE) {
								System.out.println("================================== RESPONSE ===================================");
								System.out.println("--------------- HEADER ----------------");
								System.out.println(resp.header.toString());
								System.out.println("--------------- DETAILS ---------------");
								System.out.println(resp.payload.getTypeUrl());
								System.out.println("==================================== END ======================================");
							}
	
							// See if the app waits for this message.
							ResponseFuture future = responseMap.get(resp.header.getApplicationMessageId());
							if (future != null && !future.isDone()) future.partCompleteAsync(resp);
						}
					}
				} catch (IllegalStateException e) {
					System.err.println("Connection closed.");
					responseMap.clear();
					ex.cancel(false);
				} catch (Throwable e) {
					e.printStackTrace();
					break;
				} 
			}

			if(pendingRequests.isEmpty())
				stopExecutorIfDone();
		}
	};

	@Override
	public void close() {
		super.close();
		synchronized (this) {
			if (ex != null) {
				ex.cancel(true);
			}
		}
	}
}
