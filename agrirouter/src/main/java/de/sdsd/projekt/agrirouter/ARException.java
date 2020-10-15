package de.sdsd.projekt.agrirouter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import agrirouter.commons.MessageOuterClass.Message;
import agrirouter.commons.MessageOuterClass.Messages;

/**
 * Exception for all agrirouter related errors.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public class ARException extends Exception implements Iterable<Message> {

	private static final long serialVersionUID = 6633541640221784292L;

	private List<Message> messages = null;

	/**
	 * Constructs a new exception with the specified detail message.
	 * 
	 * @param message the detail message. The detail message is saved for later
	 *                retrieval by the getMessage() method.
	 */
	public ARException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 * 
	 * @param message the detail message (which is saved for later retrieval by the
	 *                getMessage() method).
	 * @param cause   the cause (which is saved for later retrieval by the
	 *                getCause() method). (A null value is permitted, and indicates
	 *                that the cause is nonexistent or unknown.)
	 */
	public ARException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception with the specified agrirouter messages.
	 * 
	 * @param msgs agrirouter messages
	 * @see Messages
	 */
	public ARException(List<Message> msgs) {
		super(createMessage(msgs));
		this.messages = msgs;
	}

	/**
	 * Creates the detail message from a list of agrirouter messages.
	 * 
	 * @param messages agrirouter messages
	 * @return the detail message (which is saved for later retrieval by the
	 *         getMessage() method).
	 */
	private static String createMessage(List<Message> messages) {
		if (messages != null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < messages.size(); ++i) {
				if (i > 0)
					sb.append('\n');
				Message msg = messages.get(i);
				sb.append(msg.getMessage());
			}
			return sb.toString();
		} else
			return "Empty agrirouter error";
	}

	/**
	 * Get iterable over every agrirouter error message, that is associated with
	 * this exception.
	 * 
	 * @return agrirouter error messages
	 */
	public Iterable<Message> getMessages() {
		return this;
	}

	/**
	 * Get the agrirouter error message with the given index.
	 * 
	 * @param i zero-based index of the message
	 * @return agrirouter error message or null, if there is no message for the
	 *         given index
	 */
	public Message getMessage(int i) {
		return (messages == null || messages.size() <= i) ? null : messages.get(i);
	}

	/**
	 * Get a list of every agrirouter error message, that is associated with this
	 * exception.
	 * 
	 * @return agrirouter error messages
	 */
	public List<Message> getMessageList() {
		return messages == null ? Collections.emptyList() : messages;
	}

	@Override
	public Iterator<Message> iterator() {
		return new Iterator<Message>() {
			private int cur = 0;

			@Override
			public Message next() {
				return getMessage(cur++);
			}

			@Override
			public boolean hasNext() {
				return messages != null && messages.size() > cur;
			}
		};
	}

	/**
	 * Returns all error messages with error codes from the agrirouter. Use this for
	 * intern logging and {@link #getMessage()} for extern representation of the
	 * error.
	 */
	@Override
	public String toString() {
		if (messages != null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < messages.size(); ++i) {
				if (i > 0)
					sb.append('\n');
				Message msg = messages.get(i);
				sb.append(msg.getMessageCode()).append(": ").append(msg.getMessage());
			}
			return sb.toString();
		} else
			return getMessage();
	}

}
