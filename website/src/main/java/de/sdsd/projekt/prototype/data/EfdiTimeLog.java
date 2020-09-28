package de.sdsd.projekt.prototype.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.CheckForNull;

import org.apache.commons.io.IOUtils;

import com.google.protobuf.InvalidProtocolBufferException;

import efdi.GrpcEfdi;

/**
 * Represents an EFDI timelog, including the device description.
 * Includes helpers for reading and writing device description and timelogs from/to a zip archive.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class EfdiTimeLog {
	public static final String FILENAME_DEVICEDESCRIPTION = "DeviceDescription.bin";
	public static final String FORMAT_TIMELOG = "TLG%05d.bin";
	
	@CheckForNull
	private final byte[] deviceDescription;
	private final Map<String, byte[]> timelogs = new HashMap<>();
	
	public EfdiTimeLog(InputStream input) {
		byte[] deviceDescription = null;
		try(ZipInputStream zip = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = zip.getNextEntry()) != null) {
				if(entry.isDirectory())
					continue;
				else if(entry.getName().endsWith("DeviceDescription.bin"))
					deviceDescription = IOUtils.toByteArray(zip);
				else if(entry.getName().endsWith(".bin"))
					timelogs.put(entry.getName(), IOUtils.toByteArray(zip));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.deviceDescription = deviceDescription;
	}
	
	public EfdiTimeLog(byte[] content) {
		this(new ByteArrayInputStream(content));
	}
	
	public EfdiTimeLog(GrpcEfdi.ISO11783_TaskData deviceDescription) {
		this.deviceDescription = deviceDescription.toByteArray();
	}
	
	public EfdiTimeLog(DeviceDescription deviceDescription) {
		this.deviceDescription = deviceDescription.getBinaryContent();
	}
	
	@CheckForNull
	public GrpcEfdi.ISO11783_TaskData getDeviceDescription() throws InvalidProtocolBufferException {
		if(deviceDescription == null) return null;
		return GrpcEfdi.ISO11783_TaskData.parseFrom(deviceDescription);
	}
	
	public Set<String> getTimeLogNames() {
		return timelogs.keySet();
	}
	
	@CheckForNull
	public GrpcEfdi.TimeLog getTimeLog(String name) throws InvalidProtocolBufferException {
		byte[] bs = timelogs.get(name);
		return bs != null ? GrpcEfdi.TimeLog.parseFrom(bs) : null;
	}
	
	public EfdiTimeLog setTimeLog(String name, GrpcEfdi.TimeLog tlg) {
		timelogs.put(name, tlg.toByteArray());
		return this;
	}
	
	public EfdiTimeLog setTimeLog(String name, byte[] tlg) {
		timelogs.put(name, tlg);
		return this;
	}
	
	public String getFreeTimeLogName() {
		for(int i = 1; i < 100000; ++i) {
			String name = String.format(FORMAT_TIMELOG, i);
			if(!timelogs.containsKey(name))
				return name;
		}
		throw new IndexOutOfBoundsException();
	}
	
	public EfdiTimeLog addTimeLog(GrpcEfdi.TimeLog tlg) {
		timelogs.put(getFreeTimeLogName(), tlg.toByteArray());
		return this;
	}
	
	public EfdiTimeLog addTimeLog(byte[] tlg) {
		timelogs.put(getFreeTimeLogName(), tlg);
		return this;
	}
	
	public void writeToZip(OutputStream out) {
		try(ZipOutputStream zip = new ZipOutputStream(out, Charset.forName("Cp437"))) {
			if(deviceDescription != null) {
				zip.putNextEntry(new ZipEntry(FILENAME_DEVICEDESCRIPTION));
				zip.write(deviceDescription);
				zip.closeEntry();
			}
			for(Entry<String, byte[]> tlg : timelogs.entrySet()) {
				zip.putNextEntry(new ZipEntry(tlg.getKey()));
				zip.write(tlg.getValue());
				zip.closeEntry();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] toZipByteArray() {
		int size = deviceDescription != null ? deviceDescription.length : 0;
		size += timelogs.values().stream().mapToInt(c -> c.length).sum();
		ByteArrayOutputStream out = new ByteArrayOutputStream(size);
		writeToZip(out);
		return out.toByteArray();
	}
}
