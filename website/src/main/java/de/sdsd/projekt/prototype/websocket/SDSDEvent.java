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
 * @param <I> identifier type.
 * @param <R> result type.
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class SDSDEvent<I, R> {
	private final Map<I, Map<Object, SDSDListener<I, R>>> observers = new ConcurrentHashMap<>();
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	
	public void setListener(I identifier, SDSDListener<I, R> listener) {
		observers.compute(identifier, (i, ob) -> {
			if(ob == null) ob = new HashMap<>();
			ob.put(listener.observerId(), listener);
			return ob;
		});
		listener.set(this, identifier);
	}
	
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
	
	public void unsetAllListener(I identifier) {
		Map<Object, SDSDListener<I, R>> removed = observers.remove(identifier);
		if(removed != null) {
			for(SDSDListener<I, R> listener : removed.values()) {
				listener.unset(this, identifier);
			}
		}
	}
	
	public void trigger(I identifier, R object) {
		if(observers.containsKey(identifier))
			executor.submit(new Reporter(identifier, object));
	}
	
	private class Reporter implements Runnable {
		private final I identifier;
		private final R object;
		
		public Reporter(I identifier, R object) {
			this.identifier = identifier;
			this.object = object;
		}

		@Override
		public void run() {
			Map<Object, SDSDListener<I, R>> listener = observers.get(identifier);
			if(listener != null)
				listener.values().forEach(l -> l.accept(object));
		}
	};
	
	public static interface SDSDListener<I, R> extends Consumer<R> {
		Object observerId();
		void set(SDSDEvent<I, R> event, I identifier);
		void unset(SDSDEvent<I, R> event, I identifier);
	}
}
