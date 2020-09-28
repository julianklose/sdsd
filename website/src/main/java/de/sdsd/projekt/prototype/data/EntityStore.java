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
 * @param <T> Type to store, must be serializable
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public abstract class EntityStore<T extends Serializable> {

	protected final RedissonClient redis;
	protected final User user;
	protected final String itemNamespace, listNamespace;

	public EntityStore(RedissonClient redis, User user, String namespace) {
		this.redis = redis;
		this.user = user;
		this.itemNamespace = namespace + ":";
		this.listNamespace = namespace + ":list:";
	}
	
	protected abstract String getId(T e);
	protected abstract T createDefault(String id);
	
	public void clear() {
		RSet<String> set = redis.<String>getSet(listNamespace + user.getName());
		set.forEach(this::delete);
		set.clear();
	}
	
	public void delete(T e) {
		delete(getId(e));
	}
	
	public void delete(String id) {
		redis.getBucket(itemNamespace + id).delete();
		redis.<String>getSet(listNamespace + user.getName()).remove(id);
	}

	public void store(T e) {
		String id = getId(e);
		redis.getBucket(itemNamespace + id).set(e);
		redis.<String>getSet(listNamespace + user.getName()).add(id);
	}
	
	public void storeAll(Collection<T> entities) {
		entities.forEach(e -> redis.getBucket(itemNamespace + getId(e)).set(e));
		redis.<String>getSet(listNamespace + user.getName())
				.addAll(entities.stream().map(this::getId).collect(Collectors.toSet()));
	}
	
	public T read(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists() ? bucket.get() : createDefault(id);
	}
	
	public Set<T> readAll() {
		return redis.<String>getSet(listNamespace + user.getName()).readAll()
				.stream().map(this::read).collect(Collectors.toSet());
	}
	
	public boolean exists(String id) {
		RBucket<T> bucket = redis.<T>getBucket(itemNamespace + id);
		return bucket.isExists();
	}
}
