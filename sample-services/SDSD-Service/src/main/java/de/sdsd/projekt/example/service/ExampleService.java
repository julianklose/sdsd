package de.sdsd.projekt.example.service;

import java.io.IOException;

import de.sdsd.projekt.api.ServiceAPI;
import de.sdsd.projekt.api.ServiceAPI.ApiInstance;
import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;


/**
 * The Class ExampleService.
 */
public class ExampleService implements AutoCloseable {

	/** The api. */
	private final ServiceAPI api;
	
	/**
	 * Instantiates a new example service.
	 *
	 * @param serviceToken the service token
	 * @param local the local
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ExampleService(String serviceToken, boolean local) throws IOException {
		this.api = new ServiceAPI(local);
		try {
			api.setInstanceChangedListener(serviceToken, this::runTaskForInstance);
			for(ApiInstance inst : api.listActiveInstances(serviceToken)) {
				runTaskForInstance(inst);
			}
		} catch (JsonRpcException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Close.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void close() throws Exception {
		api.close();
	}
	
	/**
	 * Run task for instance.
	 *
	 * @param inst the inst
	 */
	private void runTaskForInstance(ApiInstance inst) {
		System.out.format("Found instance: activated(%s)\n", inst.activated);

		try {
			//TODO: write service code here
			
			inst.setError(null);
			//inst.complete("Result");
			//System.out.format("Instance completed: activated(%s)\n", inst.activated);
		} catch (Throwable e) {
			e.printStackTrace();
			try {
				inst.setError(e.getMessage());
			} catch (JsonRpcException e1) {
				e1.printStackTrace();
			}
		}
	}
	
}
