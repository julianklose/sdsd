package de.sdsd.projekt.parser;

import com.opencsv.bean.CsvBindByName;

/**
 * Bean for representing a line of data from CSV HackeData format. OpenCSV
 * annotations are used. OpenCSV is included by Maven.
 * 
 * @author ngs
 *
 */
public class HackeData {
	/**
	 * Because fields in data format are written in CAPSLOCK CsvBindByName(column=)
	 * is used. All values can be null. ATTENTION: Getter gives back Double.NaN for
	 * null values for Mocot and Dicot for use in MainParser!
	 */
	@CsvBindByName
	private Long time;
	
	/** The lon. */
	@CsvBindByName(column = "LON")
	private Double lon;
	
	/** The lat. */
	@CsvBindByName(column = "LAT")
	private Double lat;
	
	/** The alt. */
	@CsvBindByName(column = "ALT")
	private Double alt;
	
	/** The section. */
	@CsvBindByName(column = "SECTION")
	private Integer section;
	
	/** The lon head. */
	@CsvBindByName(column = "LON_HEAD")
	private Double lon_head;
	
	/** The lat head. */
	@CsvBindByName(column = "LAT_HEAD")
	private Double lat_head;
	
	/** The status. */
	@CsvBindByName(column = "STATUS")
	private Integer status;
	
	/** The result id. */
	@CsvBindByName(column = "RESULTID")
	private Integer resultId;
	
	/** The systime. */
	@CsvBindByName(column = "SYSTIME")
	private Long systime;
	
	/** The beavp. */
	@CsvBindByName(column = "BEAVP")
	private Double beavp;
	
	/** The brsnn. */
	@CsvBindByName(column = "BRSNN")
	private Double brsnn;
	
	/** The dicot. */
	@CsvBindByName(column = "DICOT")
	private Double dicot;
	
	/** The galap. */
	@CsvBindByName(column = "GALAP")
	private Double galap;
	
	/** The matin. */
	@CsvBindByName(column = "MATIN")
	private Double matin;
	
	/** The mocot. */
	@CsvBindByName(column = "MOCOT")
	private Double mocot;
	
	/** The trzaw. */
	@CsvBindByName(column = "TRZAW")
	private Double trzaw;
	
	/** The zeamx. */
	@CsvBindByName(column = "ZEAMX")
	private Double zeamx;

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "HackeData [time=" + time + ", lon=" + lon + ", lat=" + lat + ", alt=" + alt + ", section=" + section
				+ ", lon_head=" + lon_head + ", lat_head=" + lat_head + ", status=" + status + ", resultId=" + resultId
				+ ", systime=" + systime + ", beavp=" + beavp + ", brsnn=" + brsnn + ", dicot=" + dicot + ", galap="
				+ galap + ", matin=" + matin + ", mocot=" + mocot + ", trzaw=" + trzaw + ", zeamx=" + zeamx + "]";
	}

	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public Long getTime() {
		return time;
	}

	/**
	 * Sets the time.
	 *
	 * @param time the new time
	 */
	public void setTime(Long time) {
		this.time = time;
	}

	/**
	 * Gets the lon.
	 *
	 * @return the lon
	 */
	public Double getLon() {
		return lon;
	}

	/**
	 * Sets the lon.
	 *
	 * @param lon the new lon
	 */
	public void setLon(Double lon) {
		this.lon = lon;
	}

	/**
	 * Gets the lat.
	 *
	 * @return the lat
	 */
	public Double getLat() {
		return lat;
	}

	/**
	 * Sets the lat.
	 *
	 * @param lat the new lat
	 */
	public void setLat(Double lat) {
		this.lat = lat;
	}

	/**
	 * Gets the alt.
	 *
	 * @return the alt
	 */
	public Double getAlt() {
		return alt;
	}

	/**
	 * Sets the alt.
	 *
	 * @param alt the new alt
	 */
	public void setAlt(Double alt) {
		this.alt = alt;
	}

	/**
	 * Gets the section.
	 *
	 * @return the section
	 */
	public Integer getSection() {
		return section;
	}

	/**
	 * Sets the section.
	 *
	 * @param section the new section
	 */
	public void setSection(Integer section) {
		this.section = section;
	}

	/**
	 * Gets the lon head.
	 *
	 * @return the lon head
	 */
	public Double getLon_head() {
		return lon_head;
	}

	/**
	 * Sets the lon head.
	 *
	 * @param lon_head the new lon head
	 */
	public void setLon_head(Double lon_head) {
		this.lon_head = lon_head;
	}

	/**
	 * Gets the lat head.
	 *
	 * @return the lat head
	 */
	public Double getLat_head() {
		return lat_head;
	}

	/**
	 * Sets the lat head.
	 *
	 * @param lat_head the new lat head
	 */
	public void setLat_head(Double lat_head) {
		this.lat_head = lat_head;
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public Integer getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the new status
	 */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/**
	 * Gets the result id.
	 *
	 * @return the result id
	 */
	public Integer getResultId() {
		return resultId;
	}

	/**
	 * Sets the result id.
	 *
	 * @param resultId the new result id
	 */
	public void setResultId(Integer resultId) {
		this.resultId = resultId;
	}

	/**
	 * Gets the systime.
	 *
	 * @return the systime
	 */
	public Long getSystime() {
		return systime;
	}

	/**
	 * Sets the systime.
	 *
	 * @param systime the new systime
	 */
	public void setSystime(Long systime) {
		this.systime = systime;
	}

	/**
	 * Gets the beavp.
	 *
	 * @return the beavp
	 */
	public Double getBeavp() {
		return beavp;
	}

	/**
	 * Sets the beavp.
	 *
	 * @param beavp the new beavp
	 */
	public void setBeavp(Double beavp) {
		this.beavp = beavp;
	}

	/**
	 * Gets the brsnn.
	 *
	 * @return the brsnn
	 */
	public Double getBrsnn() {
		return brsnn;
	}

	/**
	 * Sets the brsnn.
	 *
	 * @param brsnn the new brsnn
	 */
	public void setBrsnn(Double brsnn) {
		this.brsnn = brsnn;
	}

	/**
	 * Gets the dicot.
	 *
	 * @return the dicot
	 */
	public Double getDicot() {
		return dicot;
	}

	/**
	 * Sets the dicot.
	 *
	 * @param dicot the new dicot
	 */
	public void setDicot(Double dicot) {
		this.dicot = dicot;
	}

	/**
	 * Gets the galap.
	 *
	 * @return the galap
	 */
	public Double getGalap() {
		return galap;
	}

	/**
	 * Sets the galap.
	 *
	 * @param galap the new galap
	 */
	public void setGalap(Double galap) {
		this.galap = galap;
	}

	/**
	 * Gets the matin.
	 *
	 * @return the matin
	 */
	public Double getMatin() {
		return matin;
	}

	/**
	 * Sets the matin.
	 *
	 * @param matin the new matin
	 */
	public void setMatin(Double matin) {
		this.matin = matin;
	}

	/**
	 * Gets the mocot.
	 *
	 * @return the mocot
	 */
	public Double getMocot() {
		return mocot;
	}

	/**
	 * Sets the mocot.
	 *
	 * @param mocot the new mocot
	 */
	public void setMocot(Double mocot) {
		this.mocot = mocot;
	}

	/**
	 * Gets the trzaw.
	 *
	 * @return the trzaw
	 */
	public Double getTrzaw() {
		return trzaw;
	}

	/**
	 * Sets the trzaw.
	 *
	 * @param trzaw the new trzaw
	 */
	public void setTrzaw(Double trzaw) {
		this.trzaw = trzaw;
	}

	/**
	 * Gets the zeamx.
	 *
	 * @return the zeamx
	 */
	public Double getZeamx() {
		return zeamx;
	}

	/**
	 * Sets the zeamx.
	 *
	 * @param zeamx the new zeamx
	 */
	public void setZeamx(Double zeamx) {
		this.zeamx = zeamx;
	}

}
