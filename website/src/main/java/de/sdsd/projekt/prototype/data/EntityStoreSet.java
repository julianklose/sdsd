package de.sdsd.projekt.prototype.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * Base class for sets to store in the entity store.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @param <T> Type to store, must be serializable
 * @see EntityStore
 */
public abstract class EntityStoreSet<T extends Serializable> {
	
	/** The redis. */
	protected final RedissonClient redis;
	
	/** The user. */
	protected final User user;
	
	/** The list namespace. */
	protected final String itemNamespace, listNamespace;

	/**
	 * Instantiates a new entity store set.
	 *
	 * @param redis the redis
	 * @param user the user
	 * @param itemNamespace the item namespace
	 * @param listNamespaceSuffix the list namespace suffix
	 */
	public EntityStoreSet(RedissonClient redis, User user, String itemNamespace, String listNamespaceSuffix) {
		this.redis = redis;
		this.user = user;
		this.itemNamespace = itemNamespace + ":";
		this.listNamespace = this.itemNamespace + listNamespaceSuffix + ":";
	}
	
	/**
	 * Gets the id.
	 *
	 * @param e the e
	 * @return the id
	 */
	protected abstract String getId(T e);
	
	/**
	 * Creates the default.
	 *
	 * @param id the id
	 * @return the t
	 */
	protected abstract T createDefault(String id);
	
	/**
	 * Clear.
	 */
	public void clear() {
		redis.<String>getSet(listNamespace + user.getName()).clear();
	}
	
	/**
	 * Update.
	 */
	public void update() {
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		Set<String> existing = set.readAll()
					.stream().filter(this::exists).collect(Collectors.toSet());
		set.retainAll(existing);
	}
	
	/**
	 * Sets the.
	 *
	 * @param entities the entities
	 */
	public void set(Collection<T> entities) {
		Set<String> ids = entities.stream().map(this::getId).collect(Collectors.toSet());
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		set.addAll(ids);
		set.retainAll(ids);
	}
	
	/**
	 * Removes the.
	 *
	 * @param e the e
	 */
	public void remove(T e) {
		remove(getId(e));
	}
	
	/**
	 * Removes the.
	 *
	 * @param id the id
	 */
	public void remove(String id) {
		redis.getSet(listNamespace + user.getName()).remove(id);
	}

	/**
	 * Adds the.
	 *
	 * @param e the e
	 */
	public void add(T e) {
		redis.getSet(listNamespace + user.getName()).add(getId(e));
	}
	
	/**
	 * Read.
	 *
	 * @param id the id
	 * @return the t
	 */
	protected T read(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists() ? bucket.get() : createDefault(id);
	}
	
	/**
	 * Read all.
	 *
	 * @return the sets the
	 */
	public Set<T> readAll() {
		return redis.<String>getSet(listNamespace + user.getName()).readAll()
				.stream().map(this::read).collect(Collectors.toSet());
	}
	
	/**
	 * Exists.
	 *
	 * @param id the id
	 * @return true, if successful
	 */
	protected boolean exists(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists();
	}
}
