package de.sdsd.projekt.prototype.jsonrpc;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.request.payload.endpoint.Capabilities.CapabilitySpecification.PushNotification;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.AROnboarding.SecureOnboardingContext;
import de.sdsd.projekt.agrirouter.request.ARCapability;
import de.sdsd.projekt.agrirouter.request.AREndpoint;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader.ARMsgHeaderResult;
import de.sdsd.projekt.prototype.applogic.AgrirouterFunctions.ReceivedMessageResult;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.ARCaps;
import de.sdsd.projekt.prototype.data.ARConn;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * JSONRPC-Endpoint for agrirouter functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AgrirouterEndpoint extends JsonRpcEndpoint {

	public AgrirouterEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	public JSONObject status(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if(user != null) {
				ARConn arConn = user.agrirouter();
				SecureOnboardingContext soc = user.getSecureOnboardingContext();
				JSONObject out = new JSONObject()
						.put("pendingEndpoints", user.getPendingEndpoints() != null)
						.put("onboarding", soc != null && soc.isReadyToOnboard());
				if(arConn != null) {
					out
							.put("onboarded", true)
							.put("qa", arConn.isQA())
							.put("mqtt", arConn.isMQTT())
							.put("expireDays", Duration.between(Instant.now(), arConn.getExpirationDate()).toDays());
				} else {
					out
							.put("onboarded", false)
							.put("qa", false)
							.put("mqtt", false);
				}
				return out;
			}
			else return new JSONObject();
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject reconnect(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("reconnect: user(" + (user != null ? user.getName() : "none") + ")");
	
			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				application.agrirouter.reconnect(user);
				return new JSONObject().put("success", true);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject reonboard(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("reonboard: user(" + (user != null ? user.getName() : "none") + ")");
	
			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				return new JSONObject().put("success", true).put("redirectUrl", application.agrirouter.reonboard(user).toString());
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	public JSONObject startSecureOnboarding(HttpServletRequest req, boolean qa, boolean mqtt) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("StartSecureOnboarding: user(" + (user != null ? user.getName() : "none") + ")");
	
			if (user == null)
				throw new NoLoginException();
			else {
				return new JSONObject().put("success", true).put("redirectUrl", application.agrirouter.startSecureOnboarding(user, qa, mqtt).toString());
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject agrirouterSecureOnboard(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("agrirouterSecureOnboard: user(" + (user != null ? user.getName() : "none") + ")");
	
			if (user == null)
				throw new NoLoginException();
			else {
				boolean ok = application.agrirouter.secureOnboard(user);
				if(ok) application.logInfo(user, "Onboarded to agrirouter account " + user.agrirouter().getAccountId() 
						+ " as endpoint " + user.agrirouter().getOwnEndpointId());
				return new JSONObject().put("success", ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject agrirouterOffboard(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("agrirouterOffboard: user(" + (user != null ? user.getName() : "none") + ") onboarded("
					+ (user != null ? user.agrirouter() != null : "none") + ")");
			
			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				boolean ok = application.agrirouter.offboard(user);
				if(ok) application.logInfo(user, "Onboarding revoked");
				return new JSONObject().put("success", ok);
			}
				
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	private JSONObject endpointToJson(AREndpoint ep) {
		if(ep == null) return new JSONObject();
		JSONArray accepts = ep.getCapabilities() == null ? new JSONArray() : ep.getCapabilities().stream()
				.filter(cap -> cap.getDirection() != ARDirection.SEND)
				.map(cap -> cap.getType().technicalMessageType())
				.collect(Util.toJSONArray());
		return new JSONObject()
				.put("id", ep.getId())
				.put("name", ep.getName())
				.put("type", ep.getType())
				.put("active", ep.isActive())
				.put("accepts", accepts);
	}
	
	public JSONObject listEndpoints(HttpServletRequest req, boolean update) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listEndpoints: user(" + (user != null ? user.getName() : "none")
				+ ") onboarding(" + (user != null ? user.agrirouter() != null : "none") + ") update(" + update + ")");
			
			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				try {
					List<AREndpoint> endpointLists = update ? 
							application.agrirouter.readEndpoints(user).join() 
							: application.agrirouter.getCachedEndpoints(user);
							
					final User finaluser = user;
					return new JSONObject()
							.put("all", endpointLists.stream()
									.filter(e -> !e.getId().equals(finaluser.agrirouter().getOwnEndpointId()) &&
									(e.getType().equals("application") || e.getType().equals("pairedAccount") 
											|| e.getType().equals("machine") || e.getType().equals("virtualCU")))
									.map(this::endpointToJson)
									.collect(Util.toJSONArray()))
							.put("receiver", endpointLists.stream()
									.filter(AREndpoint::canReceive)
									.map(this::endpointToJson)
									.collect(Util.toJSONArray()));
				} catch (CompletionException e) {
					throw e.getCause();
				}
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	private CompletableFuture<Boolean> sendFile(final User user, String fileid, boolean publish, String[] targets) 
			throws SDSDException, IOException, ARException {
		ObjectId fid = new ObjectId(fileid);
		if(!application.list.files.exists(user, fid)) throw new SDSDException("File not found");
		final File file = application.list.files.get(user, fid);
		final List<String> targetlist = Arrays.asList(targets);

		return application.agrirouter.sendFile(user, file, publish, targetlist)
				.handle((v, e) -> {
					try {
						StringBuilder sb = new StringBuilder("Send file \"")
								.append(file.getFilename())
								.append("\" to ")
								.append(targetlist.stream()
										.map(id -> application.list.endpoints.get(user, id).getName())
										.collect(Collectors.joining("\", \"")))
								.append("\": ");
						String error = null;
						if (e != null) {
							if (e.getCause() != null)
								error = e.getCause().getMessage();
							else
								error = e.getMessage();
							application.logError(user, sb.append(error).toString());
						}
						else if(v != null && v)
							application.logInfo(user, sb.append("Successful").toString());
						else 
							return false;
						return true;
					} catch (Throwable e1) {
						e1.printStackTrace();
						return false;
					}
				});
	}
	
	public JSONObject sendFiles(HttpServletRequest req, String[] files, String[] targets, boolean wait) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("sendFiles: user(" + (user != null ? user.getName() : "none") + ") files("
					+ String.join(", ", files) + ") targets(" + String.join(", ", targets) + ")");

			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				boolean ok = true;
				if(files.length > 0 && targets.length > 0) {
					ArrayList<CompletableFuture<Boolean>> futures = new ArrayList<CompletableFuture<Boolean>>(files.length);
					for (String file : files) {
						futures.add(sendFile(user, file, false, targets));
					}
					if(wait) {
						for (CompletableFuture<Boolean> f : futures) {
							ok &= f.join();
						}
					}
				}
				return success(ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject publishFiles(HttpServletRequest req, String[] files, String[] targets, boolean wait) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("publishFiles: user(" + (user != null ? user.getName() : "none") + ") files("
					+ String.join(", ", files) + ") targets(" + String.join(", ", targets) + ")");

			if (user == null)
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				boolean ok = true;
				if(files.length > 0) {
					ArrayList<CompletableFuture<Boolean>> futures = new ArrayList<CompletableFuture<Boolean>>(files.length);
					for (String file : files) {
						futures.add(sendFile(user, file, true, targets));
					}
					if(wait) {
						for (CompletableFuture<Boolean> f : futures) {
							ok &= f.join();
						}
					}
				}
				return success(ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject receiveFiles(HttpServletRequest req, boolean recent) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if (user == null)
				throw new NoLoginException();
			else if(user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				try {
					ARMsgHeaderResult headers = recent 
							? application.agrirouter.readMessageHeaders(user, Instant.now().truncatedTo(ChronoUnit.DAYS), Instant.now()).join() 
							: application.agrirouter.readAllMessageHeaders(user).join();
					List<ARMsgHeader> complete = headers.stream()
							.filter(h -> !h.getIds().isEmpty() && h.isComplete())
							.collect(Collectors.toList());
							
					System.out.println("receiveFiles: user(" + (user != null ? user.getName() : "none") 
							+ ") recent(" + recent + ") found " + headers.size() + " new messages, " 
							+ complete.size() + " of these are complete and valid.");
					application.logInfo(user, headers.size() + (recent ? " recent" : " new") + " messages available");
					if(complete.size() < headers.size())
						application.logInfo(user, "There are " + (headers.size() - complete.size()) 
								+ " incomplete messages, that won't be downloaded now.");
					
					int messagesReceived = receiveFiles(user, complete);
					System.out.println("receiveFiles: user(" + (user != null ? user.getName() : "none") 
							+ ") recent(" + recent + ") received " + messagesReceived + " new messages.");
					return new JSONObject().put("received", messagesReceived)
							.put("moreMessagesAvailable", headers.getSingleMessageCount() < headers.getTotalMessagesInQuery());
				} catch(CompletionException e) {
					throw e.getCause();
				}
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	private int receiveFiles(User user, List<ARMsgHeader> headers) {
		if(headers.isEmpty()) return 0;
		int received = 0;
		try {
			List<ReceivedMessageResult> results = application.agrirouter.receiveMessages(user, headers).join();
			for(int i = 0; i < results.size(); ++i) {
				ReceivedMessageResult res = results.get(i);
				if(res.isSaved()) {
					if(res.isNew())
						application.logInfo(user, "Received file: \"" + res.getName() + "\" from " 
								+ application.list.endpoints.get(user, headers.get(i).getSender()).getName());
					++received;
				}
				else if(res.isError())
					throw res.getError();
				else {
					System.out.println("ARReceive: Discarded file because of missing storage task.");
					application.logInfo(user, "Discarded file because of missing storage task");
				}
			}
		} catch (Throwable e) {
			if(e instanceof CompletionException) e = e.getCause();
			createError(user, e);
		}
		return received;
	}
	
	public JSONObject agrirouterClearFeeds(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("agrirouterClearFeeds: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				long count = 0;
				try {
					Messages msgs = application.agrirouter.clearFeed(user).join();
					count = msgs.getMessagesList().stream()
							.filter(msg -> "VAL_000209".equals(msg.getMessageCode()))
							.count();
					
					application.logInfo(user, "Deleted " + count + " messages in the agrirouter feed");
				} catch (CompletionException e) {
					throw e.getCause();
				}
				return new JSONObject()
						.put("success", true)
						.put("count", count);
			}
			
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject agrirouterSubList(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("agrirouterSubList: user(" + (user != null ? user.getName() : "none") + 
					") onboarding(" + (user != null ? user.agrirouter() != null : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				return new JSONObject().put("subs", new JSONArray());
			else {
				Set<ARMessageType> subs = user.agrirouter().getSubscriptions();
				ARCaps caps = application.list.capabilities.get(user, user.username);
				JSONArray array = caps.getCapabilities().stream()
						.sorted()
						.filter(ARCapability::isReceive)
						.map(cap -> artype(cap.getType())
								.put("active", subs.contains(cap.getType())))
						.collect(Util.toJSONArray());
				return new JSONObject().put("subs", array);
			}
			
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject agrirouterSetSubs(HttpServletRequest req, String[] subs) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("agrirouterSetSubs: user(" + (user != null ? user.getName() : "none") 
					+ ") subs(" + String.join(", ", subs) + ")");

			if (user == null) 
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				boolean ok = application.agrirouter.setSubscriptions(user, subs);
				if(ok) application.logInfo(user, "Set agrirouter subscriptions to " + String.join(", ", subs));
				return new JSONObject().put("success", ok);
			}
			
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject listCapabilities(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listCapabilities: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else if(user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				ARCaps caps = application.list.capabilities.get(user, user.username);
				
				JSONArray array = new JSONArray();
				for(ARMessageType t : ARMessageType.values()) {
					if(t.technicalMessageType().isEmpty()) continue;
					if(t == ARMessageType.OTHER && !user.agrirouter().isQA()) continue;
					JSONObject cap = artype(t);
					ARDirection dir = caps.getCapability(t);
					if(dir != null) cap.put("direction", dir.number());
					array.put(cap);
				}
				return new JSONObject().put("capabilities", array)
						.put("pushNotifications", caps.getPushNotification().getNumber());
			}
			
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setCapabilities(HttpServletRequest req, JSONArray capabilities, int pushNotifications) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("setCapabilities: user(%s) capabilities(%d) pushNotifications(%d)\n", 
					user != null ? user.getName() : "none", capabilities.length(), pushNotifications);

			if (user == null) 
				throw new NoLoginException();
			else if(user.agrirouter() == null)
				throw new NoOnboardingException();
			else {
				Map<ARMessageType, ARDirection> capMap = new HashMap<>(capabilities.length());
				for(int i = 0; i < capabilities.length(); ++i) {
					JSONObject cap = capabilities.getJSONObject(i);
					ARDirection dir = ARDirection.from(cap.optInt("direction", -1));
					if(dir == null) continue;
					ARMessageType type = ARMessageType.from(cap.getString("type"));
					capMap.put(type, dir);
				}
				PushNotification push = PushNotification.forNumber(pushNotifications);
				if(push == null) throw new SDSDException("Unknown pushNotifications value: " + pushNotifications);
				
				boolean ok = application.agrirouter.setCapabilities(user, capMap, push);
				if(ok) application.logInfo(user, "Set %d agrirouter capabilities and pushnotification %s", 
						capabilities.length(), push.name());
				return success(ok);
			}
			
		} catch (Throwable e) {
			throw createError(user, e);
		}
	} 
}
