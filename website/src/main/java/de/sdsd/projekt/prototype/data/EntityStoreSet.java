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
 * @see EntityStore
 * @param <T> Type to store, must be serializable
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public abstract class EntityStoreSet<T extends Serializable> {
	protected final RedissonClient redis;
	protected final User user;
	protected final String itemNamespace, listNamespace;

	public EntityStoreSet(RedissonClient redis, User user, String itemNamespace, String listNamespaceSuffix) {
		this.redis = redis;
		this.user = user;
		this.itemNamespace = itemNamespace + ":";
		this.listNamespace = this.itemNamespace + listNamespaceSuffix + ":";
	}
	
	protected abstract String getId(T e);
	protected abstract T createDefault(String id);
	
	public void clear() {
		redis.<String>getSet(listNamespace + user.getName()).clear();
	}
	
	public void update() {
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		Set<String> existing = set.readAll()
					.stream().filter(this::exists).collect(Collectors.toSet());
		set.retainAll(existing);
	}
	
	public void set(Collection<T> entities) {
		Set<String> ids = entities.stream().map(this::getId).collect(Collectors.toSet());
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		set.addAll(ids);
		set.retainAll(ids);
	}
	
	public void remove(T e) {
		remove(getId(e));
	}
	
	public void remove(String id) {
		redis.getSet(listNamespace + user.getName()).remove(id);
	}

	public void add(T e) {
		redis.getSet(listNamespace + user.getName()).add(getId(e));
	}
	
	protected T read(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists() ? bucket.get() : createDefault(id);
	}
	
	public Set<T> readAll() {
		return redis.<String>getSet(listNamespace + user.getName()).readAll()
				.stream().map(this::read).collect(Collectors.toSet());
	}
	
	protected boolean exists(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists();
	}
}
