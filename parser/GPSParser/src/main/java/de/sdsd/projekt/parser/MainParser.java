package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import com.google.protobuf.Timestamp;

import agrirouter.technicalmessagetype.Gps.GPSList;
import agrirouter.technicalmessagetype.Gps.GPSList.GPSEntry;
import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.TimeLog;
import de.sdsd.projekt.api.ParserAPI.TimeLogWriter;
import de.sdsd.projekt.api.ParserAPI.ValueInfo;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;

public class MainParser {

	public static void main(String[] args) throws IOException {
		if(args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch(args[0].toLowerCase()) {
			case "parse":
				parse(in, out);
				break;
			case "test":
				System.exit(test(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse', 'test')");
				break;
			}
		} else 
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}
	
	public static Instant toInstant(Timestamp time) {
		return Instant.ofEpochSecond(time.getSeconds(), time.getNanos());
	}
	
	private static final WikiFormat FORMAT = new WikiFormat("gps");
	private static final WikiType LOG = FORMAT.res("log"), INFO = FORMAT.res("info");
	
	public static void parse(InputStream input, OutputStream output) {
		try (ParserAPI api = new ParserAPI(output)) {
			List<String> errors = new ArrayList<>();
			long t1 = System.nanoTime();
			
			try {
				List<GPSEntry> list = GPSList.parseFrom(input).getGpsEntriesList();
				
				Model model = ModelFactory.createDefaultModel();
				TimeLog tlg = new TimeLog("sdsd:gps", "GPS", 
						toInstant(list.get(0).getGpsUtcTimestamp()),
						toInstant(list.get(list.size()-1).getGpsUtcTimestamp()),
						list.size());
				tlg.writeTo(model, LOG);
				
				List<ValueInfo> infos = new ArrayList<>(5);
				infos.add(new ValueInfo("sdsd:posStatus").setDesignator("Position status").addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:pdop").setDesignator("Position DOP").setScale(0.1).setNumberOfDecimals(1).addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:hdop").setDesignator("Horizontal DOP").setScale(0.1).setNumberOfDecimals(1).addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:satellites").setDesignator("Number of satellites").addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:fieldStatus").setDesignator("Field status").addTimeLog(tlg));
				for(ValueInfo info : infos) {
					info.writeTo(model, INFO);
				}
				api.writeTriples(model);
				
				Long[] values = new Long[5];
				try (TimeLogWriter tlw = api.addTimeLog(tlg, infos)) {
					for(GPSEntry gps : list) {
						values[0] = (long) gps.getPositionStatusValue();
						values[1] = Math.round(gps.getPdop() * 10.);
						values[2] = Math.round(gps.getHdop() * 10.);
						values[3] = (long) gps.getNumberOfSatellites();
						values[4] = (long) gps.getFieldStatusValue();
						
						tlw.write(toInstant(gps.getGpsUtcTimestamp()), gps.getPositionNorth(), 
								gps.getPositionEast(), gps.getPositionUp(), values);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				errors.add(e.getMessage());
			}
			
			api.setParseTime((System.nanoTime()-t1)/1000000);
			api.setErrors(errors);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} 
	}
	
	public static boolean test(InputStream input, OutputStream output) {
		try {
			GPSList list = GPSList.parseFrom(input);
			return list.getGpsEntriesCount() > 0;
		} catch (IOException e) {}
		return false;
	}

}
