package de.sdsd.projekt.prototype.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Represents the file contents, stored in MongoDB.
 * 
 * @See File
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class FileContent {
	public static final String FILEID = "file", USER = "user", CONTENT = "content", UNCOMPRESSED = "uncompressed", EXPIRES = "expires";
	
	public static Bson filter(User user, ObjectId fileid) {
		return Filters.and(Filters.eq(USER, user.getName()), Filters.eq(FILEID, fileid));
	}
	
	public static Document create(File file, byte[] content, boolean compress) {
		Document doc = new Document()
				.append(FILEID, file.getId())
				.append(USER, file.getUser());
		if(compress)
			doc.append(UNCOMPRESSED, content.length)
					.append(CONTENT, new Binary(compress(content)));
		else
			doc.append(CONTENT, new Binary(content));
		if(file.getExpires() != null)
			doc.append(EXPIRES, Date.from(file.getExpires()));
		return doc;
	}
	
	private final ObjectId file;
	private final String user;
	private byte[] content;
	private int uncompressed;
	
	public FileContent(Document doc) {
		this.file = doc.getObjectId(FILEID);
		this.user = doc.getString(USER);
		this.content = doc.get(CONTENT, Binary.class).getData();
		this.uncompressed = doc.getInteger(UNCOMPRESSED, -1);
	}
	
	public Bson filter() {
		return Filters.and(Filters.eq(USER, user), Filters.eq(FILEID, file));
	}

	public ObjectId getFile() {
		return file;
	}

	public String getUser() {
		return user;
	}
	
	public boolean isCompressed() {
		return uncompressed >= 0;
	}
	
	public int getSize() {
		return uncompressed < 0 ? content.length : uncompressed;
	}

	public byte[] getContent() {
		return uncompressed < 0 ? content : decompress(content, uncompressed);
	}
	
	public Bson setContent(byte[] content, boolean compress) {
		if(compress) {
			this.uncompressed = content.length;
			this.content = compress(content);
			return Updates.combine(Updates.set(CONTENT, new Binary(this.content)), Updates.set(UNCOMPRESSED, uncompressed));
		} else {
			this.uncompressed = -1;
			this.content = content;
			return Updates.combine(Updates.set(CONTENT, new Binary(this.content)), Updates.unset(UNCOMPRESSED));
		}
	}
	
	private static byte[] compress(byte[] content) {
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		try (DeflaterOutputStream compressor = new DeflaterOutputStream(out)) {
			IOUtils.copy(in, compressor);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}
	
	private static byte[] decompress(byte[] content, int size) {
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		ByteArrayOutputStream out = new ByteArrayOutputStream(size);
		try (InflaterOutputStream decompressor = new InflaterOutputStream(out)) {
			IOUtils.copy(in, decompressor);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

}
