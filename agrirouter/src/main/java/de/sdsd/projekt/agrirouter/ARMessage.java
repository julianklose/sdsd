package de.sdsd.projekt.agrirouter;

import static de.sdsd.projekt.agrirouter.ARConfig.USE_BASE64;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.ByteString.Output;
import com.google.protobuf.Message;

import agrirouter.request.Request.RequestEnvelope;
import agrirouter.request.Request.RequestPayloadWrapper;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponsePayloadWrapper;

/**
 * Message containing a request or a file to sent to the agrirouter.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARMessage {
	private final static String TYPEURL_PREFIX = "types.agrirouter/";
	
	private final RequestEnvelope header;
	private byte[] payload = null;
	private Message params = null;
	
	/**
	 * Package-internal constructor.
	 * @param header header with information about the message
	 */
	ARMessage(RequestEnvelope header) {
		this.header = header;
	}
	
	/**
	 * Returns the file content if this is a file message or null if it is a request.
	 * @return file content or null
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Add file content to the message. A message can either contain a file or a {@link #setParams(Message) request body}.
	 * @param payload file content
	 * @return this object for method chaining
	 */
	public ARMessage setPayload(byte[] payload) {
		this.payload = payload;
		return this;
	}

	/**
	 * Returns the request body.
	 * @return request body
	 */
	public Message getParams() {
		return params;
	}

	/**
	 * Set the request body of this message. A message can either contain a {@link #setPayload(byte[]) file} or a request body.
	 * @param params request body
	 * @return this object for method chaining
	 */
	public ARMessage setParams(Message params) {
		this.params = params;
		return this;
	}

	/**
	 * Returns the message header with information about this message.
	 * @return message header
	 */
	public RequestEnvelope getHeader() {
		return header;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("--------------- HEADER ----------------\n");
		sb.append(header.toString());
		if(params != null)
			sb.append("--------------- PARAMS ----------------\n")
				.append(params.toString());
		if(payload != null)
			sb.append("--------------- PAYLOAD ---------------\n")
				.append(payload.length);
		return sb.toString();
	}
	
	/**
	 * Writes the whole message into a delimited stream and packages it in a ByteString.
	 * 
	 * @return delimited ByteString
	 * @throws IOException if there are errors writing the protobuf messages.
	 */
	public ByteString toByteString() throws IOException {
		Output out = ByteString.newOutput();
		header.writeDelimitedTo(out);
		if(params != null)
			RequestPayloadWrapper.newBuilder()
				.setDetails(Any.pack(params, TYPEURL_PREFIX))
				.build().writeDelimitedTo(out);
		if(payload != null) {
			Any param = USE_BASE64 
					? Any.newBuilder().setValue(ByteString.copyFromUtf8(Base64.getEncoder().encodeToString(payload))).build()
					: Any.pack(BytesValue.newBuilder().setValue(ByteString.copyFrom(payload)).build());
			RequestPayloadWrapper.newBuilder()
				.setDetails(param)
				.build().writeDelimitedTo(out);
		}
		return out.toByteString();
	}
	
	/**
	 * Response struct that combines header and payload.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	static class Response {
		public final ResponseEnvelope header;
		public final Any payload;
		
		/**
		 * Read the response from an input stream.
		 * 
		 * @param in input stream containing delimited protobuf response
		 * @throws IOException if the response couldn't be read from the stream
		 */
		public Response(InputStream in) throws IOException {
			this.header = ResponseEnvelope.parseDelimitedFrom(in);
			this.payload = ResponsePayloadWrapper.parseDelimitedFrom(in).getDetails();
		}
	}
	
}