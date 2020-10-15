package de.sdsd.projekt.prototype.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableObject;

/**
 * Generic event for the SDSD listener system.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @param <I> identifier type.
 * @param <R> result type.
 */
public class SDSDEvent<I, R> {
	
	/** The observers. */
	private final Map<I, Map<Object, SDSDListener<I, R>>> observers = new ConcurrentHashMap<>();
	
	/** The Constant executor. */
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	
	/**
	 * Sets the listener.
	 *
	 * @param identifier the identifier
	 * @param listener the listener
	 */
	public void setListener(I identifier, SDSDListener<I, R> listener) {
		observers.compute(identifier, (i, ob) -> {
			if(ob == null) ob = new HashMap<>();
			ob.put(listener.observerId(), listener);
			return ob;
		});
		listener.set(this, identifier);
	}
	
	/**
	 * Unset listener.
	 *
	 * @param identifier the identifier
	 * @param listenerID the listener ID
	 * @return true, if successful
	 */
	public boolean unsetListener(I identifier, Object listenerID) {
		final MutableObject<SDSDListener<I,R>> removed = new MutableObject<>();
		observers.compute(identifier, (i, ob) -> {
			if(ob != null) {
				removed.setValue(ob.remove(listenerID));
				if(ob.isEmpty()) 
					ob = null;
			}
			return ob;
		});
		if(removed.getValue() != null) 
			removed.getValue().unset(this, identifier);
		return removed.getValue() != null;
	}
	
	/**
	 * Unset all listener.
	 *
	 * @param identifier the identifier
	 */
	public void unsetAllListener(I identifier) {
		Map<Object, SDSDListener<I, R>> removed = observers.remove(identifier);
		if(removed != null) {
			for(SDSDListener<I, R> listener : removed.values()) {
				listener.unset(this, identifier);
			}
		}
	}
	
	/**
	 * Trigger.
	 *
	 * @param identifier the identifier
	 * @param object the object
	 */
	public void trigger(I identifier, R object) {
		if(observers.containsKey(identifier))
			executor.submit(new Reporter(identifier, object));
	}
	
	/**
	 * The Class Reporter.
	 */
	private class Reporter implements Runnable {
		
		/** The identifier. */
		private final I identifier;
		
		/** The object. */
		private final R object;
		
		/**
		 * Instantiates a new reporter.
		 *
		 * @param identifier the identifier
		 * @param object the object
		 */
		public Reporter(I identifier, R object) {
			this.identifier = identifier;
			this.object = object;
		}

		/**
		 * Run.
		 */
		@Override
		public void run() {
			Map<Object, SDSDListener<I, R>> listener = observers.get(identifier);
			if(listener != null)
				listener.values().forEach(l -> l.accept(object));
		}
	};
	
	/**
	 * The listener interface for receiving SDSD events.
	 * The class that is interested in processing a SDSD
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addSDSDListener<code> method. When
	 * the SDSD event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @param <I> the generic type
	 * @param <R> the generic type
	 * @see SDSDEvent
	 */
	public static interface SDSDListener<I, R> extends Consumer<R> {
		
		/**
		 * Observer id.
		 *
		 * @return the object
		 */
		Object observerId();
		
		/**
		 * Sets the.
		 *
		 * @param event the event
		 * @param identifier the identifier
		 */
		void set(SDSDEvent<I, R> event, I identifier);
		
		/**
		 * Unset.
		 *
		 * @param event the event
		 * @param identifier the identifier
		 */
		void unset(SDSDEvent<I, R> event, I identifier);
	}
}
