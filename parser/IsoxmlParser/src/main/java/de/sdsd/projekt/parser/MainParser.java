package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import agrirouter.technicalmessagetype.Gps.GPSList;
import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.GeoWriter;
import de.sdsd.projekt.api.ParserAPI.GridWriter;
import de.sdsd.projekt.api.ParserAPI.TimeLogWriter;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;
import de.sdsd.projekt.api.ServiceResult.WikiInstance;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.parser.isoxml.Attribute.DatetimeAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.ULongAttr;
import de.sdsd.projekt.parser.isoxml.Geo;
import de.sdsd.projekt.parser.isoxml.Grid;
import de.sdsd.projekt.parser.isoxml.Grid.TreatmentZone;
import de.sdsd.projekt.parser.isoxml.GridValue;
import de.sdsd.projekt.parser.isoxml.ISOXMLParser;
import de.sdsd.projekt.parser.isoxml.IsoXmlElement;
import de.sdsd.projekt.parser.isoxml.TelemetryConverter;
import de.sdsd.projekt.parser.isoxml.TimeLog;
import de.sdsd.projekt.parser.isoxml.TimeLog.ValueDescription;
import de.sdsd.projekt.parser.isoxml.TimeLogEntry;
import efdi.GrpcEfdi;

/**
 * The Class MainParser.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class MainParser {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch (args[0].toLowerCase()) {
			case "parse":
				isoxml(in, out);
				break;
			case "validate":
				validate(in, out);
				break;
			case "efdi":
				efdi(in, out);
				break;
			case "gps":
				gps(in, out);
				break;
			case "test":
				System.exit(testIsoxml(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse', 'validate', 'efdi', 'gps', 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|validate|efdi|gps|test filepath");
	}

	/** The Constant NS_WIKI. */
	public static final String NS_WIKI = "https://app.sdsd-projekt.de/wikinormia.html?page=";

	/**
	 * Creates the wiki resource.
	 *
	 * @param name the name
	 * @return the resource
	 */
	public static Resource createWikiResource(String name) {
		return ResourceFactory.createResource(NS_WIKI + name);
	}

	/**
	 * Creates the wiki property.
	 *
	 * @param res  the res
	 * @param name the name
	 * @return the property
	 */
	public static Property createWikiProperty(Resource res, String name) {
		return ResourceFactory.createProperty(res.getURI() + '#', name);
	}

	/**
	 * The Class GeoObject.
	 */
	private static class GeoObject {

		/** The geo. */
		public final Geo geo;

		/** The geojson. */
		public final JSONObject geojson;

		/**
		 * Instantiates a new geo object.
		 *
		 * @param geo     the geo
		 * @param geojson the geojson
		 */
		public GeoObject(Geo geo, JSONObject geojson) {
			this.geo = geo;
			this.geojson = geojson;
		}
	}

	/** The ddi map. */
	private static Map<String, WikiInstance> ddiMap = null;

	/**
	 * Gets the ddi designator.
	 *
	 * @param ddi the ddi
	 * @return the ddi designator
	 */
	public static String getDdiDesignator(int ddi) {
		try {
			if (ddiMap == null)
				ddiMap = ParserAPI.getWikinormiaInstances(Util.UNKNOWN.res("ddi"), true);
			WikiInstance wikiddi = ddiMap.get(Integer.toString(ddi));
			if (wikiddi != null)
				return wikiddi.label;
		} catch (JsonRpcException e) {
			System.err.println(e.getMessage());
		}
		return String.format("%04X", ddi);
	}

	/**
	 * Elementtype.
	 *
	 * @param type the type
	 * @return the element type
	 */
	public static ElementType elementtype(Geo.GeoType type) {
		if (type == Geo.PolygonType.PARTFIELD_BOUNDARY)
			return ElementType.Field;
		else if (type == Geo.PolygonType.TREATMENT_ZONE)
			return ElementType.TreatmentZone;
		else if (type == Geo.LineStringType.GUIDANCE_PATTERN)
			return ElementType.GuidancePattern;
		else if (type == Geo.PointType.FIELD_ACCESS)
			return ElementType.FieldAccess;
		else
			return ElementType.Other;
	}

	/**
	 * The Class TimedTimeLog.
	 */
	public static class TimedTimeLog {

		/** The start. */
		public final Instant start;

		/** The tlg. */
		public final ParserAPI.TimeLog tlg;

		/**
		 * Instantiates a new timed time log.
		 *
		 * @param timelog the timelog
		 */
		public TimedTimeLog(TimeLog timelog) {
			this.start = timelog.get(0).getHead(0, Instant.class);
			this.tlg = timelog.getTimeLog();
		}
	}

	/**
	 * Isoxml.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void isoxml(InputStream input, OutputStream output) {
		Validation errors = new Validation();
		Model model = null;
		List<GeoObject> features = new ArrayList<>();
		List<TimeLog> timelogs = new ArrayList<>();
		List<Grid> grids = new ArrayList<>();
		long t1 = System.nanoTime();

		try {
			ISOXMLParser isoxml = new ISOXMLParser(input);
			IsoXmlElement taskdata = isoxml.readTaskData();
			isoxml.resolveAllXFR(taskdata, errors);
			errors.addAll(taskdata.getAllErrors());
			model = ModelFactory.createDefaultModel();

			for (IsoXmlElement tsk : taskdata.findChildren("TSK")) {
				List<TimedTimeLog> tsktlgs = new ArrayList<>();
				for (IsoXmlElement child : tsk.getChildren()) {
					try {
						if (child.getTag().equals("TLG")) {
							TimeLog timelog = isoxml.getTimeLog(child);
							if (!timelog.isEmpty()) {
								timelog.findReferences();
								timelog.getTimeLog().writeTo(model, ISOXMLParser.FORMAT.res("TLG"));
								tsktlgs.add(new TimedTimeLog(timelog));

								List<ValueDescription> vds = timelog.getValueDescriptions();
								if (vds == null)
									throw new Exception("Missing column descriptions");
								for (ValueDescription vd : vds) {
									String label = vd.getDesignator();
									if (label.isEmpty()) {
										label = getDdiDesignator(vd.ddi);
										vd.setDesignator(label);
									}
									vd.dataLogValue.setLabel(vd.deviceElement.getLabel() + ": " + label);
									isoxml.toRDF(model, vd.dataLogValue, true);
									vd.writeTo(model, ISOXMLParser.FORMAT.res("DLV"));
								}

								timelogs.add(timelog);
								errors.addAll(timelog.getAllErrors());
							}
						} else if (child.getTag().equals("GRD")) {
							Grid grid = isoxml.getGrid(child);
							if (!grid.isEmpty()) {
								grid.getGrid().writeTo(model, ISOXMLParser.FORMAT.res("GRD"));

								for (TreatmentZone tz : grid.getTreatmentZones()) {
									for (Grid.ValueInfo vi : tz) {
										if (vi.getDesignator().isEmpty()) {
											vi.setDesignator(getDdiDesignator(vi.ddi));
										}
										vi.writeTo(model, ISOXMLParser.FORMAT.res("PDV"));
									}
								}

								grids.add(grid);
								errors.addAll(grid.getAllErrors());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						errors.fatal(child.prefixEnd(e.getMessage()));
					}
				}

				List<IsoXmlElement> tims = tsk.findChildren("TIM");
				for (int i = 0; i < tims.size(); ++i) {
					IsoXmlElement tim = tims.get(i);
					Instant start = tim.getAttribute("start", DatetimeAttr.class).getValue(), stop;
					{
						DatetimeAttr stopattr = tim.getAttribute("stop", DatetimeAttr.class);
						if (stopattr.hasValue())
							stop = stopattr.getValue();
						ULongAttr duration = tim.getAttribute("duration", ULongAttr.class);
						stop = start.plusSeconds(duration.getValue());
					}
					ParserAPI.TimeLog tlg = null;
					for (TimedTimeLog t : tsktlgs) {
						if (t.start.isBefore(start) || t.start.isAfter(stop))
							continue;
						tlg = t.tlg;
						break;
					}

					for (IsoXmlElement dlv : tim.findChildren("DLV")) {
						try {
							ValueDescription vd = ValueDescription.create(dlv, tlg, true);
							if (tlg != null)
								vd.writeTo(model, ISOXMLParser.FORMAT.res("DLV"));
							else
								vd.writeToNoParent(model, ISOXMLParser.FORMAT.res("DLV"));
						} catch (SAXException e) {
							errors.error(dlv.prefixEnd(e.getMessage()));
						}
					}
				}
			}

			for (Geo geo : isoxml.getAllGeometries()) {
				try {
					features.add(new GeoObject(geo, geo.toGeoJson()));
					errors.addAll(geo.getErrors());
				} catch (Exception e) {
					errors.error(geo.element.prefixEnd(e.getMessage()));
				}
			}

			isoxml.toRDF(model, taskdata, true);
		} catch (Throwable e) {
			e.printStackTrace();
			errors.fatal(e.getMessage());
		}

		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);
			api.writeTriples(model);

			if (features.size() > 0) {
				try (GeoWriter geo = api.writeGeo()) {
					for (GeoObject feature : features) {
						IsoXmlElement element = feature.geo.element.getParent(); // PFD, TZN, GGP or GPN
						if (element == null)
							element = feature.geo.element; // shouldn't be possible

						geo.writeFeature(feature.geojson, elementtype(feature.geo.getType()), element.getUris().get(0),
								element.getLabel() != null ? element.getLabel() : element.getTag());
					}
				}
			}

			for (TimeLog timelog : timelogs) {
				List<ValueDescription> valueInfos = timelog.getValueDescriptions();
				boolean posUp = timelog.getHeaderNames().get(3).equalsIgnoreCase("positionUp");
				Long[] values = new Long[valueInfos.size()];
				try (TimeLogWriter tlw = api.addTimeLog(timelog.getTimeLog(), valueInfos)) {
					for (TimeLogEntry entry : timelog) {
						Instant time = entry.getHead(0, Instant.class);
						if (time == null)
							continue;
						Double latitude = entry.getHead(1, Double.class);
						if (latitude == null)
							continue;
						Double longitude = entry.getHead(2, Double.class);
						if (longitude == null)
							continue;
						double altitude = posUp && entry.hasValue(3) ? entry.getHead(3, Integer.class) / 1000.
								: Double.NaN;
						for (int i = 0; i < values.length; ++i) {
							values[i] = entry.hasValue(i) ? Long.valueOf(entry.getValue(i)) : null;
						}
						tlw.write(time, latitude, longitude, altitude, values);
					}
				}
			}

			for (Grid grid : grids) {
				try (GridWriter gw = api.addGrid(grid.getGrid(), grid.getRowCount(), grid.getColumnCount())) {
					Map<Integer, Grid.ValueInfo> infos = new HashMap<>();
					for (TreatmentZone tzn : grid.getTreatmentZones()) {
						for (Grid.ValueInfo info : tzn) {
							infos.putIfAbsent(info.ddi, info);
						}
					}

					Long[] values = new Long[grid.getColumnCount()];
					for (Grid.ValueInfo info : infos.values()) {
						boolean empty = true;
						for (int row = 0; row < grid.getRowCount(); ++row) {
							for (int col = 0; col < grid.getColumnCount(); ++col) {
								GridValue value = grid.get(row, col).getFromDdi(info.ddi);
								if (empty && value != null) {
									gw.startGridValue(value.info);
									for (int i = 0; i < row; ++i) {
										gw.writeGridRow(values);
									}
									empty = false;
								}
								values[col] = value != null ? Long.valueOf(value.getValue()) : null;
							}
							if (!empty)
								gw.writeGridRow(values);
						}
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Validate.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void validate(InputStream input, OutputStream output) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
			Validation errors = new Validation();
			try {
				ISOXMLParser isoxml = new ISOXMLParser(input);

				IsoXmlElement taskdata = isoxml.readTaskData();
				isoxml.resolveAllXFR(taskdata, errors);
				errors.addAll(taskdata.getAllErrors());

				try {
					for (Geo geo : isoxml.getAllGeometries()) {
						errors.addAll(geo.getErrors());
					}
				} catch (Exception e) {
					errors.error(e.getMessage());
				}
				for (IsoXmlElement tlg : isoxml.getAllTimeLogs()) {
					try {
						TimeLog timeLog = isoxml.getTimeLog(tlg);
						errors.addAll(timeLog.getAllErrors());
					} catch (Exception e) {
						errors.error(tlg.prefixEnd(e.getMessage()));
					}
				}
				for (IsoXmlElement grd : isoxml.getAllGrids()) {
					try {
						Grid grid = isoxml.getGrid(grd);
						errors.addAll(grid.getAllErrors());
					} catch (Exception e) {
						errors.error(grd.prefixEnd(e.getMessage()));
					}
				}
			} catch (Throwable e) {
				errors.fatal(e.getMessage());
			}

			errors.forEach(System.out::println);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Efdi.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void efdi(InputStream input, OutputStream output) {
		try {
			ISOXMLParser isoxml = new ISOXMLParser(input);

			IsoXmlElement taskdata = isoxml.readTaskData();
			isoxml.resolveAllXFR(taskdata, null);

			List<IsoXmlElement> tlgs = isoxml.getAllTimeLogs();
			List<TimeLog> timelogs = new ArrayList<>(tlgs.size());
			for (IsoXmlElement tlg : tlgs) {
				try {
					timelogs.add(isoxml.getTimeLog(tlg));
				} catch (IOException e) {
				}
			}

			try (ZipOutputStream zip = new ZipOutputStream(output, Charset.forName("Cp437"))) {
				GrpcEfdi.ISO11783_TaskData deviceDescription;
				if (timelogs.size() != 1) {
					timelogs.sort((a, b) -> a.getTimeLog().getName().compareTo(b.getTimeLog().getName()));
					deviceDescription = TelemetryConverter.readAllDevicesDescription(taskdata);
				} else
					deviceDescription = TelemetryConverter.readDeviceDescription(taskdata, timelogs.get(0));

				zip.putNextEntry(new ZipEntry("DeviceDescription.bin"));
				deviceDescription.writeTo(zip);
				zip.closeEntry();

				for (TimeLog log : timelogs) {
					TelemetryConverter converter = new TelemetryConverter(log);

					zip.putNextEntry(new ZipEntry(log.getTimeLog().getName() + ".bin"));
					GrpcEfdi.TimeLog.Builder times = GrpcEfdi.TimeLog.newBuilder()
							.setFilename(log.getTimeLog().getName());
					for (int i = 0; i < log.size(); ++i) {
						times.addTime(converter.readTime(i));
					}
					times.build().writeTo(zip);
					zip.closeEntry();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gps.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void gps(InputStream input, OutputStream output) {
		try {
			ISOXMLParser isoxml = new ISOXMLParser(input);

			IsoXmlElement taskdata = isoxml.readTaskData();
			isoxml.resolveAllXFR(taskdata, null);

			List<IsoXmlElement> tlgs = isoxml.getAllTimeLogs();
			List<TimeLog> timelogs = new ArrayList<>(tlgs.size());
			for (IsoXmlElement tlg : tlgs) {
				try {
					timelogs.add(isoxml.getTimeLog(tlg));
				} catch (IOException e) {
				}
			}

			try (ZipOutputStream zip = new ZipOutputStream(output, Charset.forName("Cp437"))) {
				if (timelogs.size() > 1) {
					timelogs.sort((a, b) -> a.getTimeLog().getName().compareTo(b.getTimeLog().getName()));
				}

				for (TimeLog log : timelogs) {
					TelemetryConverter converter = new TelemetryConverter(log);

					zip.putNextEntry(new ZipEntry(log.getTimeLog().getName() + ".bin"));
					GPSList.Builder list = GPSList.newBuilder();
					for (int i = 0; i < log.size(); ++i) {
						list.addGpsEntries(converter.readGpsEntry(i));
					}
					list.build().writeTo(zip);
					zip.closeEntry();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test isoxml.
	 *
	 * @param input  the input
	 * @param output the output
	 * @return true, if successful
	 */
	public static boolean testIsoxml(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName().toLowerCase();
				if (name.endsWith("taskdata.xml"))
					return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

}
