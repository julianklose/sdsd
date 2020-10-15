package de.sdsd.projekt.prototype.applogic;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import de.sdsd.projekt.prototype.data.Permissions;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.Service;
import de.sdsd.projekt.prototype.data.ServiceInstance;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.websocket.SDSDEvent;

/**
 * Provides all service functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ServiceFunctions {
	
	/** The app. */
	private final ApplicationLogic app;
	
	/** The instances. */
	final MongoCollection<Document> services, instances;
	
	/** The instance stopped. */
	public final SDSDEvent<Service, ServiceInstance> instanceChanged = new SDSDEvent<>(), 
			instanceCanceled = new SDSDEvent<>(),
			instanceStopped = new SDSDEvent<>();

	/**
	 * Instantiates a new service functions.
	 *
	 * @param app the app
	 */
	public ServiceFunctions(ApplicationLogic app) {
		this.app = app;
		this.services = app.mongo.sdsd.getCollection("services");
		services.createIndex(Indexes.ascending(Service.TOKEN), new IndexOptions().unique(true));
		services.createIndex(Indexes.ascending(Service.NAME), new IndexOptions().unique(true));
		services.createIndex(Indexes.ascending(Service.AUTHOR, Service.VISIBLE));
		this.instances = app.mongo.sdsd.getCollection("serviceInstances");
		instances.createIndex(Indexes.ascending(ServiceInstance.TOKEN), new IndexOptions().unique(true));
		instances.createIndex(Indexes.ascending(ServiceInstance.USER, ServiceInstance.COMPLETED, 
				ServiceInstance.SERVICE, ServiceInstance.PARAMETER, ServiceInstance.PERM));
	}
	
	/**
	 * Adds the service.
	 *
	 * @param user the user
	 * @param name the name
	 * @param params the params
	 * @param accessUris the access uris
	 * @return the service
	 */
	public Service addService(User user, String name, JSONArray params, Set<String> accessUris) {
		Document doc = Service.create(user, name, params, accessUris);
		Service service = new Service(doc);
		services.insertOne(doc);
		return service;
	}
	
	/**
	 * Update service.
	 *
	 * @param service the service
	 * @param update the update
	 * @return true, if successful
	 */
	public boolean updateService(Service service, Bson update) {
		return services.updateOne(service.filter(), update).wasAcknowledged();
	}
	
	/**
	 * Delete service.
	 *
	 * @param service the service
	 * @return true, if successful
	 * @throws SDSDException the SDSD exception
	 */
	public boolean deleteService(Service service) throws SDSDException {
		Bson filter = Filters.and(ServiceInstance.filterService(service), ServiceInstance.filterCompleted(false));
		if(getInstance(filter).isPresent())
			throw new SDSDException("There are active instances of the service");
		return services.deleteOne(service.filter()).wasAcknowledged();
	}
	
	/**
	 * Clear all.
	 *
	 * @param user the user
	 */
	public void clearAll(User user) {
		for(ServiceInstance instance : listInstances(ServiceInstance.filter(user))) {
			if(instance.isReadyToStart()) {
				try {
					Service service = getService(instance);
					instanceCanceled.trigger(service, instance);
					instanceStopped.trigger(service, instance);
				} catch (SDSDException e) {}
			}
		}
		instances.deleteMany(ServiceInstance.filter(user)).wasAcknowledged();
		
		for(Service service : listServices(Service.filterAuthor(user))) {
			for(ServiceInstance instance : listInstances(ServiceInstance.filterReady(service))) {
				instanceCanceled.trigger(service, instance);
				instanceStopped.trigger(service, instance);
			}
			instances.deleteMany(ServiceInstance.filterService(service)).wasAcknowledged();
		}
		services.deleteMany(Service.filterAuthor(user)).wasAcknowledged();
	}
	
	/**
	 * Activate service.
	 *
	 * @param user the user
	 * @param service the service
	 * @return the service instance
	 */
	public ServiceInstance activateService(User user, Service service) {
		Document doc = ServiceInstance.create(user, service);
		ServiceInstance instance = new ServiceInstance(doc);
		instances.insertOne(doc);
		instance.getPermissions(app).createNew(service.getAccess());
		if(instance.isReadyToStart())
			instanceChanged.trigger(service, instance);
		return instance;
	}
	
	/**
	 * Update instance.
	 *
	 * @param instance the instance
	 * @param update the update
	 * @return true, if successful
	 */
	public boolean updateInstance(ServiceInstance instance, Bson update) {
		return instances.updateOne(instance.filter(), update).wasAcknowledged();
	}
	
	/**
	 * Complete instance.
	 *
	 * @param instance the instance
	 * @param result the result
	 * @return true, if successful
	 */
	public boolean completeInstance(ServiceInstance instance, String result) {
		if(instances.updateOne(instance.filter(), instance.setCompleted(result)).wasAcknowledged()) {
			try {
				instanceStopped.trigger(getService(instance), instance);
			} catch (SDSDException e) {} // ignore when no service found
			return true;
		}
		return false;
	}
	
	/**
	 * Delete instance.
	 *
	 * @param instance the instance
	 * @return true, if successful
	 */
	public boolean deleteInstance(ServiceInstance instance) {
		Permissions permissions = instance.getPermissions(app);
		boolean deleted = instances.deleteOne(instance.filter()).wasAcknowledged();
		if(deleted) {
			permissions.deleteInstance();
			
			if(instance.getCompleted() == null) { // instance got cancelled by the user
				try {
					Service service = getService(instance);
					instanceCanceled.trigger(service, instance);
					instanceStopped.trigger(service, instance);
				} catch (SDSDException e) {} // ignore when no service found
			}
		}
		return deleted;
	}
	
	/**
	 * Gets the service.
	 *
	 * @param instance the instance
	 * @return the service
	 * @throws SDSDException the SDSD exception
	 */
	public Service getService(ServiceInstance instance) throws SDSDException {
		return getService(Service.filter(instance.getServiceId()))
				.orElseThrow(() -> new SDSDException("The service of this instance was deleted"));
	}
	
	/**
	 * Gets the service.
	 *
	 * @param filter the filter
	 * @return the service
	 */
	public Optional<Service> getService(Bson filter) {
		Document service = services.find(filter).first();
		return service != null ? Optional.of(new Service(service)) : Optional.empty();
	}
	
	/**
	 * List services.
	 *
	 * @param filter the filter
	 * @return the list
	 */
	public List<Service> listServices(Bson filter) {
		return StreamSupport.stream(services.find(filter).spliterator(), false)
				.map(Service::new)
				.sorted()
				.collect(toList());
	}
	
	/**
	 * Gets the single instance of ServiceFunctions.
	 *
	 * @param user the user
	 * @param id the id
	 * @return single instance of ServiceFunctions
	 * @throws SDSDException the SDSD exception
	 */
	public ServiceInstance getInstance(User user, String id) throws SDSDException {
		return getInstance(ServiceInstance.filter(user, new ObjectId(id)))
				.orElseThrow(() -> new SDSDException("Service instance with given ID doesn't exist"));
	}
	
	/**
	 * Gets the single instance of ServiceFunctions.
	 *
	 * @param filter the filter
	 * @return single instance of ServiceFunctions
	 */
	public Optional<ServiceInstance> getInstance(Bson filter) {
		Document instance = instances.find(filter).first();
		return instance != null ? Optional.of(new ServiceInstance(instance)) : Optional.empty();
	}
	
	/**
	 * List instances.
	 *
	 * @param filter the filter
	 * @return the list
	 */
	public List<ServiceInstance> listInstances(Bson filter) {
		return StreamSupport.stream(instances.find(filter).spliterator(), false)
				.map(ServiceInstance::new)
				.sorted()
				.collect(toList());
	}

}
