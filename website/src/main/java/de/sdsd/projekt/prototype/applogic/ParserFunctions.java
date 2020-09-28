package de.sdsd.projekt.prototype.applogic;

import static de.sdsd.projekt.prototype.Main.DEBUG_MODE;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.tika.Tika;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.prototype.applogic.TableFunctions.ElementKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.FileKey;
import de.sdsd.projekt.prototype.applogic.TableFunctions.GridBatch;
import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;
import de.sdsd.projekt.prototype.applogic.TableFunctions.PositionBatch;
import de.sdsd.projekt.prototype.applogic.TableFunctions.TimelogBatch;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.GeoElement.MinMax;
import de.sdsd.projekt.prototype.data.GeoElement.MinMaxValues;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Provides access functions for parsing and deleting.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ParserFunctions {
	private final ApplicationLogic app;
	
	ParserFunctions(ApplicationLogic app) {
		this.app = app;
	}
	
	public static final String TYPE_UNKNOWN = "https://app.sdsd-projekt.de/wikinormia.html?page=unknown";
	public static final String TYPE_SERVICE_RESULT = "https://app.sdsd-projekt.de/wikinormia.html?page=serviceresult";
	
	public SDSDType determineType(final byte[] content, String filename, @Nullable ARMessageType artype) {
		String mimetype = new Tika().detect(content, filename);
		List<SDSDType> possible = app.list.types.get(null, SDSDType.filter(mimetype, artype));
		System.out.format("Test for artype(%s) and mimetype(%s): %s\n", artype, mimetype, 
				possible.stream().map(SDSDType::getName).collect(Collectors.joining(", ")));
		if(possible.size() == 1) return possible.get(0);
		
		Optional<SDSDType> foundType = possible.parallelStream()
				.filter(type -> testType(type, content))
				.findAny();
		if(foundType.isPresent())
			return foundType.get();
		
		return app.list.types.get(null, TYPE_UNKNOWN);
	}
	
	private boolean testType(SDSDType type, byte[] content) {
		if(type.getTestCommand().isPresent()) {
			System.out.println("testing for " + type.getName());
			
			try {
				if(!Files.exists(Paths.get(type.getParser().get())))
					throw new FileNotFoundException("Parser missing: " + type.getParser().get());
				Process process = new ProcessBuilder(type.getTestCommand().get().split(" ")).start();
				try (OutputStream processIn = process.getOutputStream()) {
					IOUtils.copy(new ByteArrayInputStream(content), processIn);
				} catch (IOException e) {}
				if(process.waitFor(3, TimeUnit.SECONDS)) {
					System.out.println("Tested for " + type.getName() + ": " + (process.exitValue() == 0));
					if(process.exitValue() == 0)
						return true;
				}
				else {
					System.err.println("Testing for " + type.getName() + " timeouted");
					process.destroyForcibly();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Base class for queued jobs.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class Job extends CompletableFuture<Boolean> {
		public final User user;
		public final boolean log;
		private long started;
		
		public Job(User user, boolean log) {
			super();
			this.user = user;
			this.log = log;
		}

		public void started() {
			started = System.nanoTime();
		}
		public long getStarted() {
			return started;
		}
	}
	
	/**
	 * A queued parsing job.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class Parsing extends Job {
		public final File file;
		public final byte[] content;
		
		public Parsing(User user, File file, byte[] content, boolean log) {
			super(user, log);
			this.file = file;
			this.content = content;
		}
	}
	
	/**
	 * A queued deleting job.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class Deleting extends Job {
		public final String fileid;
		
		public Deleting(User user, String fileid, boolean log) {
			super(user, log);
			this.fileid = fileid;
		}
	}
	
	/**
	 * Job queue, because there are performance problems with parsing and storing multiple files at a time.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class JobQueue implements Runnable {
		private final Queue<Job> queue = new LinkedList<>();
		private Future<?> jobExecutor = null;
		
		public synchronized boolean offer(Job job) {
			boolean ok = queue.offer(job);
			if(ok && jobExecutor == null)
				jobExecutor = app.executor.submit(this);
			return ok;
		}
		
		protected synchronized Job poll() {
			Job job = queue.poll();
			if(job == null)
				jobExecutor = null;
			return job;
		}

		@Override
		public void run() {
			Job job;
			while((job = poll()) != null) {
				if(job instanceof Parsing) {
					job.complete(parseFile((Parsing)job));
				} else if(job instanceof Deleting) {
					job.complete(unleverageFile((Deleting)job));
				}
			}
		}
	}
	private final JobQueue jobQueue = new JobQueue();
	
	
	public CompletableFuture<Boolean> parseFileAsync(User user, File file, byte[] content, boolean log) {
		Parsing parse = new Parsing(user, file, content, log);
		jobQueue.offer(parse);
		return parse;
	}
	
	public CompletableFuture<Boolean> removeFileDataAsync(User user, String fileid, boolean log) {
		Deleting delete = new Deleting(user, fileid, log);
		jobQueue.offer(delete);
		return delete;
	}
	
	public CompletableFuture<Boolean> deleteAndParseFileAsync(final User user, final File file, final byte[] content, boolean log) {
		removeFileDataAsync(user, file.getId().toHexString(), false);
		return parseFileAsync(user, file, content, log);
	}
	
	private boolean parseFile(Parsing job) {
		app.triple.updateFile(job.user, job.file);
		SDSDType type = app.list.types.get(null, job.file.getType());
		if(type.getParseCommand().isPresent()) {
			job.started();
			try {
				if(type.getUri().equals(TYPE_SERVICE_RESULT))
					return readParserResult(job, new ByteArrayInputStream(job.content));
				else {
					Process process = new ProcessBuilder(type.getParseCommand().get().split(" ")).start();
		
					Future<Boolean> future = app.executor.submit(() -> readParserResult(job, process.getInputStream()));
					try (OutputStream processIn = process.getOutputStream()) {
						IOUtils.copy(new ByteArrayInputStream(job.content), processIn);
					} catch (IOException e) {}
					app.executor.schedule(process::destroyForcibly, 30, TimeUnit.SECONDS);
					return future.get();
				}
			} catch(Throwable e) {
				e.printStackTrace();
				if(job.log)
					app.logError(job.user, "Couldn't read file: " + job.file.getFilename());
				System.err.println(job.user.getName() + ": File leverage failed: " + job.file.getFilename());
			}
		}
		return false;
	}
	
	private boolean readParserResult(Parsing job, InputStream parserResultStream) {
		try {
			Map<String, byte[]> result = new HashMap<>();
			JSONObject meta = null;
			try (ZipInputStream in = new ZipInputStream(parserResultStream)) {
				ZipEntry entry;
				while((entry = in.getNextEntry()) != null) {
					if(entry.isDirectory()) continue;
					if(entry.getName().equalsIgnoreCase("meta.json"))
						meta = new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8));
					else
						result.put(entry.getName(), IOUtils.toByteArray(in));
				}
			}
			long t2 = System.nanoTime();
			if(meta == null)
				throw new FileNotFoundException("Parser result contains no 'meta.json'");
			
			if(meta.has("parseTime"))
				System.out.format("%s: %s: Parsed in %dms\n", 
						job.file.getUser(), job.file.getFilename(), meta.getInt("parseTime"));
			
			JSONArray errors = meta.optJSONArray("errors");
			if(errors != null) {
				for(int i = 0; i < errors.length(); ++i) {
					System.err.format("%s: %s: %s\n", 
							job.file.getUser(), job.file.getFilename(), errors.getString(i));
				}
			}
	
			Future<?> tripleInserter = null, geoInserter = null, tlgInserter = null;
			
			String rname = meta.optString("triples", null);
			if(rname != null) {
				if(result.containsKey(rname)) {
					final byte[] content = result.get(rname);
					tripleInserter = app.executor.submit(() -> insertTriples(job, content));
				}
				else
					System.err.format("%s: %s: Triples '%s' missing in parser result\n", 
							job.file.getUser(), job.file.getFilename(), rname);
			}
			
			rname = meta.optString("geo", null);
			if(rname != null) {
				if(result.containsKey(rname)) {
					byte[] content = result.get(rname);
					geoInserter = app.executor.submit(() -> insertGeo(job, content));
				}
				else
					System.err.format("%s: %s: Geometries '%s' missing in parser result\n", 
							job.file.getUser(), job.file.getFilename(), rname);
			}
			
			List<Entry<String, byte[]>> timelogs = new ArrayList<>(), grids = new ArrayList<>();
			JSONArray rnames = meta.optJSONArray("timelogs");
			if(rnames != null) {
				for(int i = 0; i < rnames.length(); ++i) {
					rname = rnames.optString(i);
					if(rname != null && result.containsKey(rname)) {
						int ext = rname.lastIndexOf('.');
						timelogs.add(new SimpleEntry<>(ext > 0 ? rname.substring(0, ext) : rname, result.get(rname)));
					}
					else
						System.err.format("%s: %s: Timelog '%s' missing in parser result\n", 
								job.file.getUser(), job.file.getFilename(), rname);
				}
			}
			
			rnames = meta.optJSONArray("grids");
			if(rnames != null) {
				for(int i = 0; i < rnames.length(); ++i) {
					rname = rnames.optString(i);
					if(rname != null && result.containsKey(rname)) {
						int ext = rname.lastIndexOf('.');
						grids.add(new SimpleEntry<>(ext > 0 ? rname.substring(0, ext) : rname, result.get(rname)));
					}
					else
						System.err.format("%s: %s: Grid '%s' missing in parser result\n", 
								job.file.getUser(), job.file.getFilename(), rname);
				}
			}
			
			if(timelogs.size() > 0 || grids.size() > 0) {
				tlgInserter = app.executor.submit(() -> {
					for(Entry<String, byte[]> tlg : timelogs) {
						insertTimelog(job, tlg.getKey(), tlg.getValue());
					}
					for(Entry<String, byte[]> grd : grids) {
						insertGrid(job, grd.getKey(), grd.getValue());
					}
				});
			}
			
			try {
				if(geoInserter != null) geoInserter.get();
				if(tripleInserter != null) tripleInserter.get();
				if(tlgInserter != null) tlgInserter.get();
			} catch(ExecutionException e) {
				throw e.getCause();
			}
			
			app.dedup.findDuplicates(job.user, job.file);
			
			//change leveraged date
			long t1 = job.getStarted();
			long t3 = System.nanoTime();
			System.out.format("%s: %s: File leverage completed in %d+%dms\n", job.user.getName(), job.file.getFilename(), (t2-t1)/1000000, (t3-t2)/1000000);
			if(job.log)
				app.logInfo(job.user, "File processing completed: %s", job.file.getFilename());
			app.list.files.update(job.user, job.file, job.file.setLeveraged(Instant.now()));
			app.file.parserFinished.trigger(job.user, job.file);
			return true;
		} catch(Throwable e) {
			e.printStackTrace();
			if(job.log)
				app.logError(job.user, "Couldn't read file: " + job.file.getFilename());
			System.err.println(job.user.getName() + ": File leverage failed: " + job.file.getFilename());
			return false;
		}
	}
	
	private void insertTriples(Parsing job, byte[] content) {
		try {
			long t1 = System.nanoTime();
			Model model = ModelFactory.createDefaultModel().read(new ByteArrayInputStream(content), null, "TTL");
			long t2 = System.nanoTime();
			app.triple.insertData(model, job.file.getURI());
			long t3 = System.nanoTime();
			if(DEBUG_MODE) System.out.format("%s: %s: added %d statements to triplestore (%d+%dms)\n", 
					job.file.getUser(), job.file.getFilename(), model.size(), (t2-t1)/1000000, (t3-t2)/1000000);
		} catch (Throwable e) {
			if(job.log)
				app.logError(job.user, "Leverage error in file '%s': Couldn't read triples", 
						job.file.getFilename());
			System.err.format("%s: Leverage error in file '%s' insertTriples: %s\n", 
					job.file.getUser(), job.file.getFilename(), e.getMessage());
		}
	}
	
	private void insertGeo(Parsing job, byte[] content) {
		try {
			JSONObject collection = new JSONObject(new String(content, StandardCharsets.UTF_8));
			JSONArray features = collection.optJSONArray("features");
			int geometries = 0;
			
			if(features != null) {
				Map<String, MinMax> valueRanges = new HashMap<>();
				for(int i = 0; i < features.length(); ++i) {
					JSONObject prop = features.getJSONObject(i).optJSONObject("properties");
					if(prop != null) {
						for(String key : prop.keySet()) {
							Object val = prop.get(key);
							if(val instanceof Number) {
								MinMax minMax = valueRanges.get(key);
								if(minMax == null) valueRanges.put(key, minMax = new MinMax());
								minMax.addValue(((Number)val).doubleValue());
							}
						}
					}
				}
				for(int i = 0; i < features.length(); ++i) {
					if(insertGeoFeature(job, features.getJSONObject(i), valueRanges))
						++geometries;
				}
			}
			else {
				if(insertGeoFeature(job, collection, Collections.emptyMap()))
					++geometries;
			}
			
			if(DEBUG_MODE) System.out.format("%s: %s: added %d geometries to geostore\n", 
					job.file.getUser(), job.file.getFilename(), geometries);
		} catch (Throwable e) {
			if(job.log)
				app.logError(job.user, "Leverage error in file '%s': Couldn't read geometries", 
						job.file.getFilename());
			System.err.format("%s: Leverage error in file '%s' insertGeo: %s\n", 
					job.file.getUser(), job.file.getFilename(), e.getMessage());
		}
	}
	
	private boolean insertGeoFeature(Parsing job, JSONObject feature, Map<String, MinMax> valueRanges) {
		Object uri = feature.remove("id");
		Object label = feature.remove("label");
		Object type = feature.remove("elementType");
		
		try {
			String geoUri = uri.toString();
			String geoLabel = label.toString();
			ElementType geoType = GeoElement.type((String) type);
			
			MinMaxValues values = null;
			JSONObject prop = feature.optJSONObject("properties");
			if(prop != null) {
				for(String key : prop.keySet()) {
					Object val = prop.get(key);
					if(val instanceof Number) {
						MinMax minMax = valueRanges.get(key);
						if(minMax != null && minMax.isRange()) {
							if(values == null) values = new MinMaxValues();
							values.put(key, minMax);
						}
					}
				}
			}
			
			try {
				app.geo.insert(job.file, geoUri, geoType, feature, geoLabel, values);
			} catch (Exception e) {
				Geometry geom = GeoFunctions.readGeoJson(feature.getJSONObject("geometry").toString());
				geom = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
				feature.put("geometry", new JSONObject(GeoFunctions.toGeoJson(geom)));
				app.geo.insert(job.file, geoUri, geoType, feature, geoLabel, values);
				if(job.log)
					app.logError(job.user, "Geometry repaired in file '%s': %s", 
							job.file.getFilename(), geoLabel);
				System.err.format("%s: Geometry repaired in file '%s': %s(%s)\n", 
						job.file.getUser(), job.file.getFilename(), geoUri, geoLabel);
			}
			return true;
		} catch (Exception e) {
			System.err.format("%s: Geometry error in file '%s' insertGeoFeature: %s(%s): %s\n", 
					job.file.getUser(), job.file.getFilename(), uri != null ? uri.toString() : "null", 
							label != null ? label.toString() : "null", e.getMessage());
			return false;
		}
	}
	
	private static final String CSV_SEPARATOR = ";";
	@SuppressFBWarnings(value="NP_DEREFERENCE_OF_READLINE_VALUE", justification="checked beforehand")
	private void insertTimelog(Parsing job, String name, byte[] content) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
			int total = -2;
			for(byte b : content) {
				if(b == '\n') ++total;
			}
			if(total < 1) return;
			
			PositionBatch positionBatch = app.table.createPositionBatch(total);
			ElementKey posKey = new ElementKey(job.user.getName(), job.file.getURI(), name);
			
			String tlgUri = reader.readLine();
			String[] vuris = reader.readLine().split(CSV_SEPARATOR);
			
			if(vuris.length < 4) return;
			
			List<TimelogBatch> batches = new ArrayList<>(vuris.length-4);
			for(int i = 4; i < vuris.length; ++i) {
				batches.add(app.table.createTimelogBatch(total));
			}
			
			Set<Long> timeSet = new HashSet<>();
			int[] lastValues = new int[batches.size()];
			List<Coordinate> coords = new ArrayList<>(total);
			
			for(int log = 0; log < total; ++log) {
				String[] values = reader.readLine().split(CSV_SEPARATOR);
				boolean skip = values.length < 3;
				long epochSeconds = skip ? 0 : Long.parseLong(values[0]);
				skip |= !timeSet.add(epochSeconds);
				Instant time = skip ? null : Instant.ofEpochSecond(epochSeconds);
				
				skip |= values[1].isEmpty() || values[2].isEmpty();
				if(!skip) {
					double lat = Double.parseDouble(values[1]);
					double lng = Double.parseDouble(values[2]);
					skip |= !Double.isFinite(lat) || !Double.isFinite(lng)
							|| (Math.abs(lat) < 3. && Math.abs(lng) <= 3.)
							|| lat <= 35 || lat >= 90. //Removes wrong coordinates in Afrika
							|| lng < -180. || lng > 180.;
					
					if(!skip) {
						Coordinate coord = new Coordinate(lng, lat);
						skip |= coords.size() > 0 && coord.distance(coords.get(coords.size()-1)) > 1;
						
						if(!skip) {
							coords.add(coord);
							double alt = values.length < 4 || values[3].isEmpty() ? 0. : Double.parseDouble(values[3]);
							if(!Double.isFinite(alt)) alt = 0.;
							positionBatch.add(posKey, time, lat, lng, alt);
							if(positionBatch.executeIfFull()) {
								if(DEBUG_MODE) System.out.format("%s: %s: %s: Positions: %3d%% added %d/%d to cassandra\n", 
										posKey.user, posKey.file, posKey.name, 
										positionBatch.getPercent(), positionBatch.getCount(), positionBatch.getTotal());
							}
						}
					}
				}
				if(skip) positionBatch.decTotal();
				
				for(int i = 0; i < lastValues.length; ++i) {
					int value;
					TimelogBatch batch = batches.get(i);
					if(skip || i+4 >= values.length 
							|| values[i+4].isEmpty() 
							|| (value = Integer.parseInt(values[i+4])) == lastValues[i]) { // skip repeated values
						batch.decTotal();
						continue;
					}
					lastValues[i] = value;
					
					Key key = new Key(posKey.user, posKey.file, posKey.name, vuris[i+4]);
					batch.add(key, time, value);
					if(batch.executeIfFull()) {
						if(DEBUG_MODE) System.out.format("%s: %s: %s: %s: %3d%% added %d/%d timelogs to cassandra\n", 
								key.user, key.file, key.name, key.valueUri,  
								batch.getPercent(), batch.getCount(), batch.getTotal());
					}
				}
			}
			
			if(positionBatch.execute()) {
				if(DEBUG_MODE) System.out.format("%s: %s: %s: Positions: %3d%% added %d/%d to cassandra\n", 
						posKey.user, posKey.file, posKey.name, 
						positionBatch.getPercent(), positionBatch.getCount(), positionBatch.getTotal());
			}
			
			for(int i = 0; i < batches.size(); ++i) {
				TimelogBatch batch = batches.get(i);
				if(batch.execute()) {
					if(DEBUG_MODE) System.out.format("%s: %s: %s: %s: %3d%% added %d/%d timelogs to cassandra\n", 
							posKey.user, posKey.file, posKey.name, vuris[i+4], 
							batch.getPercent(), batch.getCount(), batch.getTotal());
				}
			}
			
			if(coords.size() > 0) {
				app.geo.insertTlg(job.file, tlgUri, name, coords);
				if(DEBUG_MODE) System.out.format("%s: %s: %s: added simlified line of timelogs to geostore\n", 
						posKey.user, posKey.file, posKey.name);
			}
		} catch (Throwable e) {
			if(job.log)
				app.logError(job.user, "Leverage error in file '%s': Couldn't read timelog %s", 
						job.file.getFilename(), name);
			System.err.format("%s: Leverage error in file '%s' insertTimelog(%s): %s\n", 
					job.file.getUser(), job.file.getFilename(), name, e.getMessage());
		}
	}
	
	private void insertGrid(Parsing job, String name, byte[] content) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			if(line == null) return;
			String[] values = line.split(CSV_SEPARATOR);
			int rows = Integer.parseUnsignedInt(values[0]);
			int cols = Integer.parseUnsignedInt(values[1]);
			if(rows == 0 || cols == 0) return;
			Coordinate spos = new Coordinate(Double.parseDouble(values[3]), Double.parseDouble(values[2]));
			Coordinate size = new Coordinate(Double.parseDouble(values[5]), Double.parseDouble(values[4]));
			
			while((line = reader.readLine()) != null && !line.isEmpty()) {
				values = line.split(CSV_SEPARATOR);
				Key key = new Key(job.user.getName(), job.file.getURI(), name, values[0]);
				GridBatch batch = app.table.createGridBatch(rows*cols);
				
				for(int r = 0; r < rows; ++r) {
					line = reader.readLine();
					if(line == null) throw new SDSDException("Unexpected end of file");
					values = line.split(CSV_SEPARATOR);
					for(int c = 0; c < cols; ++c) {
						if(c >= values.length || values[c].isEmpty())
							batch.decTotal();
						else {
							int value = Integer.parseInt(values[c]);
							if(value == 0) // skip 0 values
								batch.decTotal();
							else {
								batch.add(key, size, new Coordinate(spos.x + c * size.x, spos.y + r * size.y), value);
								if(batch.executeIfFull()) {
									if(DEBUG_MODE) System.out.format("%s: %s: %s: %s: %3d%% added %d/%d grid cells to cassandra\n", 
											key.user, key.file, key.name, key.valueUri, 
											batch.getPercent(), batch.getCount(), batch.getTotal());
								}
							}
						}
					}
				}

				if(batch.execute()) {
					if(DEBUG_MODE) System.out.format("%s: %s: %s: %s: %3d%% added %d/%d grid cells to cassandra\n", 
							key.user, key.file, key.name, key.valueUri, 
							batch.getPercent(), batch.getCount(), batch.getTotal());
				}
			}
		} catch (Throwable e) {
			if(job.log)
				app.logError(job.user, "Leverage error in file '%s': Couldn't read grid %s", 
						job.file.getFilename(), name);
			System.err.format("%s: Leverage error in file '%s' insertGrid(%s): %s\n", 
					job.file.getUser(), job.file.getFilename(), name, e.getMessage());
		}
	}
	
	private boolean unleverageFile(Deleting job) {
		try {
			job.started();
			String fileUri = File.toURI(job.fileid);
			ObjectId oid = new ObjectId(job.fileid);
			FileKey fkey = new FileKey(job.user.getName(), fileUri);
			
			List<Key> timelogKeys = app.table.listTimelogKeys(fkey);
			for(int i = 0; i < timelogKeys.size(); ++i) {
				app.table.deleteTimelog(timelogKeys.get(i));
				if(DEBUG_MODE) System.out.format("%s: %s: Deleted %d/%d timelog partitions from cassandra\n", 
						job.user.getName(), job.fileid, i+1, timelogKeys.size());
			}
			
			List<Key> gridKeys = app.table.listGridKeys(fkey);
			for(int i = 0; i < gridKeys.size(); ++i) {
				app.table.deleteGrid(gridKeys.get(i));
				if(DEBUG_MODE) System.out.format("%s: %s: Deleted %d/%d grid partitions from cassandra\n", 
						job.user.getName(), job.fileid, i+1, gridKeys.size());
			}
			
			List<ElementKey> positionKeys = app.table.listPositionKeys(fkey);
			for(int i = 0; i < positionKeys.size(); ++i) {
				app.table.deletePosition(positionKeys.get(i));
				if(DEBUG_MODE) System.out.format("%s: %s: Deleted %d/%d position partitions from cassandra\n", 
						job.user.getName(), job.fileid, i+1, positionKeys.size());
			}
			
			app.geo.deleteFrom(job.user, oid);
			
			app.dedup.deleteFileRelations(job.user, fileUri);
			app.triple.deleteFile(job.user, fileUri);
			long t1 = job.getStarted();
			long t2 = System.nanoTime();
			
			System.out.format("%s: %s: Deleted all data in %dms\n", job.user.getName(), job.fileid, (t2-t1)/1000000);
			if(job.log)
				app.logInfo(job.user, "File data deleted: %s", job.fileid);
			return true;
		} catch (Throwable e) {
			System.err.println(job.user.getName() + ": Error while deleting data of file " + job.fileid);
			e.printStackTrace();
			if(job.log)
				app.logError(job.user, "Error while deleting data of file " + job.fileid);
			return false;
		}
	}
	
}
