package de.sdsd.projekt.agrirouter;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;
import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE_RAW;
import static de.sdsd.projekt.agrirouter.ARConfig.NATIVE_PROTOBUF_MQTT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import com.sap.iotservices.common.protobuf.gateway.CommandResponseMessageProtos.CommandResponseMessage;
import com.sap.iotservices.common.protobuf.gateway.CommandResponseProtos.CommandResponse;
import com.sap.iotservices.common.protobuf.gateway.MeasureProtos.MeasureRequest;
import com.sap.iotservices.common.protobuf.gateway.MeasureProtos.MeasureRequest.Measure;
import com.sap.iotservices.common.protobuf.gateway.MeasureRequestMessageProtos.MeasureRequestMessage;

import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARConnection.MqttConnection;
import de.sdsd.projekt.agrirouter.ARMessage.Response;

/**
 * Intern class for sending request to the agrirouter and receiving the results.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
class ARMqttSender extends ARSender implements MqttCallback {

	/**
	 * Agrirouter connection (endpoint) to use for all requests.
	 */
	private final MqttConnection conn;

	/**
	 * Constructor
	 * @param executor executor service with thread pool to use for receiving responses from the agrirouter. Doesn't need more than one thread.
	 * @param conn agrirouter connection (endpoint) to use for all requests.
	 */
	public ARMqttSender(ScheduledExecutorService executor, MqttConnection conn) {
		super(executor);
		this.conn = conn;
	}
	
	@Override
	protected ResponseFuture sendMsg(ARMessage message, RequestFuture rf) throws IOException, ARException {
		String messageId = message.getHeader().getApplicationMessageId();
		rf.setCurrentMessage(messageId);

		if (DEBUG_MODE) {
			System.out.println("==================================== SEND =====================================");
			System.out.println(message.toString());
			if(DEBUG_MODE_RAW) {
				System.out.println("==================================== RAW ======================================");
				System.out.println(conn.getServerURI());
				System.out.println(conn.getUrlRequest());
			} else
				System.out.println("==================================== END ======================================");
		}
	
		MqttMessage mqttMessage;
		if(NATIVE_PROTOBUF_MQTT) {
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
			mqttMessage = new MqttMessage(measure.toByteArray());
			
			if(DEBUG_MODE_RAW)
				System.out.println(measure.toString());
		} else {
			JSONObject measure = new JSONObject()
					.put("sensorAlternateId", conn.getEndpointId())
					.put("capabilityAlternateId", conn.getCapabilityAlternateId())
					.put("measures", new JSONArray().put(new JSONArray()
						.put(Base64.getEncoder().encodeToString(message.toByteString().toByteArray()))
						.put(Long.toString(System.currentTimeMillis()))));
			mqttMessage = new MqttMessage(measure.toString().getBytes(StandardCharsets.UTF_8));
			
			if(DEBUG_MODE_RAW)
				System.out.println(measure.toString());
		}
		
		try {
			conn.send(mqttMessage);
		} catch (MqttException e) {
			throw new ARException(e.getMessage(), e);
		} 

		ResponseFuture future = new ResponseFuture(rf.req);
		responseMap.put(messageId, future);
		return future;
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		if(DEBUG_MODE_RAW) {
			System.out.println("================================== RESPONSE ===================================");
			if(token.getException() != null)
				System.err.println(token.getException().getMessage());
			else
				System.out.println("Message delivery complete");
			System.out.println("==================================== END ======================================");
		}
	}
	
	private final long[] reconnects = new long[10];
	private int reconnectIndex = 0;
	
	@Override
	public void connectionLost(Throwable cause) {
		final long now = System.currentTimeMillis();
		System.out.println(conn.getEndpointId() + " connection lost: " + cause.getMessage());
		synchronized (this) {
			reconnectIndex = (reconnectIndex + 1) % 10;
			if (now - reconnects[reconnectIndex] < 300000l) {
				try {
					System.err.println(conn.getEndpointId() + ": Too many reconnects. Connection closed.");
					conn.close();
				} catch (Exception e) {}
			} else
				reconnects[reconnectIndex] = now;
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		Response resp;
		if(NATIVE_PROTOBUF_MQTT) {
			CommandResponse proto = CommandResponse.parseFrom(message.getPayload());
			if(DEBUG_MODE_RAW) {
				System.out.println("=================================== OUTBOX ====================================");
				System.out.println("Topic: " + topic);
				System.out.println(proto.toString());
				System.out.println("==================================== END ======================================");
			}
			ByteString bytes = CommandResponseMessage.parseFrom(proto.getCommand().getValues(0).getValue()).getMessage();
			resp = new Response(bytes.newInput());
		}
		else {
			JSONObject json = new JSONObject(new String(message.getPayload(), StandardCharsets.UTF_8));
			if (DEBUG_MODE_RAW) {
				System.out.println("=================================== OUTBOX ====================================");
				System.out.println("Topic: " + topic);
				System.out.println(json.toString());
				System.out.println("==================================== END ======================================");
			}
			String b64resp = json.getJSONObject("command").getString("message");
			resp = new Response(new ByteArrayInputStream(Base64.getDecoder().decode(b64resp)));
		}
		
		if (DEBUG_MODE) {
			System.out.println("================================== MESSAGE ====================================");
			System.out.println("--------------- HEADER ----------------");
			System.out.println(resp.header.toString());
			System.out.println("--------------- DETAILS ---------------");
			System.out.println(resp.payload.getTypeUrl());
			System.out.println("==================================== END ======================================");
		}
		
		if(resp.header.getType() == ResponseBodyType.PUSH_NOTIFICATION) {
//			System.out.format("Message %s received: %d\n", 
//					resp.header.getMessageId(), System.currentTimeMillis());
			conn.pushNotification(resp);
		} else {
			ResponseFuture future = responseMap.get(resp.header.getApplicationMessageId());
			if (future != null && !future.isDone())
				future.partCompleteAsync(resp);
		}
	}
}
