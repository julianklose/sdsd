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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @See File
 */
public class FileContent {
	
	/** The Constant EXPIRES. */
	public static final String FILEID = "file", USER = "user", CONTENT = "content", UNCOMPRESSED = "uncompressed", EXPIRES = "expires";
	
	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param fileid the fileid
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId fileid) {
		return Filters.and(Filters.eq(USER, user.getName()), Filters.eq(FILEID, fileid));
	}
	
	/**
	 * Creates the.
	 *
	 * @param file the file
	 * @param content the content
	 * @param compress the compress
	 * @return the document
	 */
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
	
	/** The file. */
	private final ObjectId file;
	
	/** The user. */
	private final String user;
	
	/** The content. */
	private byte[] content;
	
	/** The uncompressed. */
	private int uncompressed;
	
	/**
	 * Instantiates a new file content.
	 *
	 * @param doc the doc
	 */
	public FileContent(Document doc) {
		this.file = doc.getObjectId(FILEID);
		this.user = doc.getString(USER);
		this.content = doc.get(CONTENT, Binary.class).getData();
		this.uncompressed = doc.getInteger(UNCOMPRESSED, -1);
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.and(Filters.eq(USER, user), Filters.eq(FILEID, file));
	}

	/**
	 * Gets the file.
	 *
	 * @return the file
	 */
	public ObjectId getFile() {
		return file;
	}

	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * Checks if is compressed.
	 *
	 * @return true, if is compressed
	 */
	public boolean isCompressed() {
		return uncompressed >= 0;
	}
	
	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public int getSize() {
		return uncompressed < 0 ? content.length : uncompressed;
	}

	/**
	 * Gets the content.
	 *
	 * @return the content
	 */
	public byte[] getContent() {
		return uncompressed < 0 ? content : decompress(content, uncompressed);
	}
	
	/**
	 * Sets the content.
	 *
	 * @param content the content
	 * @param compress the compress
	 * @return the bson
	 */
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
	
	/**
	 * Compress.
	 *
	 * @param content the content
	 * @return the byte[]
	 */
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
	
	/**
	 * Decompress.
	 *
	 * @param content the content
	 * @param size the size
	 * @return the byte[]
	 */
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
