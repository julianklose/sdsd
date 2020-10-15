package de.sdsd.projekt.prototype.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * Base for storing and accessing cache data using redis database.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @param <T> Type to store, must be serializable
 */
public abstract class EntityStore<T extends Serializable> {

	/** The redis. */
	protected final RedissonClient redis;
	
	/** The user. */
	protected final User user;
	
	/** The list namespace. */
	protected final String itemNamespace, listNamespace;

	/**
	 * Instantiates a new entity store.
	 *
	 * @param redis the redis
	 * @param user the user
	 * @param namespace the namespace
	 */
	public EntityStore(RedissonClient redis, User user, String namespace) {
		this.redis = redis;
		this.user = user;
		this.itemNamespace = namespace + ":";
		this.listNamespace = namespace + ":list:";
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
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		set.forEach(this::delete);
		set.clear();
	}
	
	/**
	 * Delete.
	 *
	 * @param e the e
	 */
	public void delete(T e) {
		delete(getId(e));
	}
	
	/**
	 * Delete.
	 *
	 * @param id the id
	 */
	public void delete(String id) {
		redis.getBucket(itemNamespace + id).delete();
		redis.<String>getSet(listNamespace + user.getName()).remove(id);
	}

	/**
	 * Store.
	 *
	 * @param e the e
	 */
	public void store(T e) {
		String id = getId(e);
		redis.getBucket(itemNamespace + id).set(e);
		redis.<String>getSet(listNamespace + user.getName()).add(id);
	}
	
	/**
	 * Store all.
	 *
	 * @param entities the entities
	 */
	public void storeAll(Collection<T> entities) {
		entities.forEach(e -> redis.getBucket(itemNamespace + getId(e)).set(e));
		redis.<String>getSet(listNamespace + user.getName())
				.addAll(entities.stream().map(this::getId).collect(Collectors.toSet()));
	}
	
	/**
	 * Read.
	 *
	 * @param id the id
	 * @return the t
	 */
	public T read(String id) {
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
	public boolean exists(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists();
	}
}
