package de.sdsd.projekt.api.isoxml;

import java.util.Arrays;
import java.util.Optional;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.ISO11783_TaskData;

public class Partfield extends Elem {
	public Partfield(ISO11783_TaskData parent, String designator, long area) {
		super(parent, "PFD");
		if(area < 0 || area > 4294967294L)
			throw new IllegalArgumentException("invalid area");
		e.setAttribute("A", id);
		e.setAttribute("C", designator);
		e.setAttribute("D", Long.toString(area));
	}
	
	public Partfield setCode(String code) {
		e.setAttribute("B", code);
		return this;
	}
	
	public Partfield setCustomer(Task.Customer ctr) {
		e.setAttribute("E", ctr.id);
		return this;
	}
	
	public Partfield setFarm(Task.Farm frm) {
		e.setAttribute("F", frm.id);
		return this;
	}
	
	public Partfield setCropType(CropType ctp) {
		e.setAttribute("G", ctp.id);
		return this;
	}
	
	public Partfield setCropVariety(CropVariety cvt) {
		e.setAttribute("H", cvt.id);
		return this;
	}
	
	public Partfield setField(Partfield pfd) {
		e.setAttribute("I", pfd.id);
		return this;
	}
	
	public Polygon addPolygon(PolygonType type) {
		return new Polygon(this, type);
	}
	
	public LineString addLineString(LineStringType type) {
		return new LineString(this, type);
	}
	
	public Point.XmlPoint addPoint(Point.PointType type, double north, double east) {
		return new Point.XmlPoint(this, type, north, east);
	}
	
	public Point.BinPoint addPoint() {
		return new Point.BinPoint(this);
	}
	
	public GuidanceGroup addGuidanceGroup() {
		return new GuidanceGroup(this);
	}
	
	
	public static enum ProductType {
		SINGLE(1), MIXTURE(2), TEMPORARY_MIXTURE(3);
		
		public final int number;
		private ProductType(int number) {
			this.number = number;
		}
		
		public static Optional<ProductType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<ProductType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class Product extends Elem {
		public Product(ISO11783_TaskData parent, String designator) {
			super(parent, "PDT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public Product setGroup(ProductGroup group) {
			e.setAttribute("C", group.id);
			return this;
		}
		
		public Product setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("D", vpn.id);
			return this;
		}
		
		public Product setQuantityDDI(int ddi) {
			e.setAttribute("E", ddi(ddi));
			return this;
		}
		
		public Product setType(ProductType type) {
			e.setAttribute("F", Integer.toString(type.number));
			return this;
		}
		
		public Product setMixtureRecipeQuantity(int mixtureRecipeQuantity) {
			if(mixtureRecipeQuantity < 0)
				throw new IllegalArgumentException("invalid mixture recipe quantity");
			e.setAttribute("G", Integer.toString(mixtureRecipeQuantity));
			return this;
		}
		
		public Product setDensityMassPerVolume(int densityMassPerVolume) {
			if(densityMassPerVolume < 0)
				throw new IllegalArgumentException("invalid density mass per volume");
			e.setAttribute("H", Integer.toString(densityMassPerVolume));
			return this;
		}
		
		public Product setDensityMassPerCount(int densityMassPerCount) {
			if(densityMassPerCount < 0)
				throw new IllegalArgumentException("invalid density mass per count");
			e.setAttribute("I", Integer.toString(densityMassPerCount));
			return this;
		}
		
		public Product setDensityVolumePerCount(int densityVolumePerCount) {
			if(densityVolumePerCount < 0)
				throw new IllegalArgumentException("invalid density volume per count");
			e.setAttribute("J", Integer.toString(densityVolumePerCount));
			return this;
		}
		
		public ProductRelation addProductRelation(Product pdt, int quantityValue) {
			return new ProductRelation(this, pdt, quantityValue);
		}
	}
	
	public static class ProductRelation extends Elem {
		public ProductRelation(Product parent, Product pdt, int quantityValue) {
			super(parent, "PRN");
			e.setAttribute("A", pdt.id);
			if(quantityValue < 0)
				throw new IllegalArgumentException("invalid quantity value");
			e.setAttribute("B", Integer.toString(quantityValue));
		}
	}
	
	public static enum ProductGroupType {
		PRODUCT_GROUP(1), CROP_TYPE(2);
		
		public final int number;
		private ProductGroupType(int number) {
			this.number = number;
		}
		
		public static Optional<ProductGroupType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<ProductGroupType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class ProductGroup extends Elem {
		public ProductGroup(ISO11783_TaskData parent, String designator) {
			super(parent, "PGP");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public ProductGroup setType(ProductGroupType type) {
			e.setAttribute("C", Integer.toString(type.number));
			return this;
		}
	}
	
	public static class CropType extends Elem {
		public CropType(ISO11783_TaskData parent, String designator) {
			super(parent, "CTP");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public CropType setProductGroup(ProductGroup pgp) {
			e.setAttribute("C", pgp.id);
			return this;
		}
		
		public CropVariety addCropVariety(String designator) {
			return new CropVariety(this, designator);
		}
	}
	
	public static class CropVariety extends Elem {
		public CropVariety(CropType parent, String designator) {
			super(parent, "CVT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public CropVariety setProduct(Product pdt) {
			e.setAttribute("C", pdt.id);
			return this;
		}
	}
	
	public static enum PolygonType {
		PARTFIELD_BOUNDARY(1), TREATMENT_ZONE(2), WATER_SURFACE(3), BUILDING(4), ROAD(5), 
		OBSTACLE(6), FLAG(7), OTHER(8), MAINFIELD(9), HEADLAND(10), BUFFER_ZONE(11), WINDBREAK(12);
		
		public final int number;
		private PolygonType(int number) {
			this.number = number;
		}
		
		public static Optional<PolygonType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<PolygonType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class Polygon extends Elem {
		private Polygon(Elem parent, PolygonType type) {
			super(parent, "PLN");
			e.setAttribute("A", Integer.toString(type.number));
		}
		public Polygon(Partfield parent, PolygonType type) {
			this((Elem)parent, type);
		}
		public Polygon(GuidanceGroup parent, PolygonType type) {
			this((Elem)parent, type);
		}
		public Polygon(GuidancePattern parent, PolygonType type) {
			this((Elem)parent, type);
		}
		public Polygon(Grid.TreatmentZone parent, PolygonType type) {
			this((Elem)parent, type);
		}
		
		public Polygon setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public Polygon setArea(long area) {
			if(area < 0)
				throw new IllegalArgumentException("invalid area");
			e.setAttribute("C", Long.toString(area));
			return this;
		}
		
		public Polygon setColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("D", Integer.toString(color));
			return this;
		}
		
		public Polygon setID() {
			e.setAttribute("E", id);
			return this;
		}
		
		public LineString addRing(LineStringType type) {
			return new LineString(this, type);
		}
	}
	
	public static enum LineStringType {
		POLYGON_EXTERIOR(1), POLYGON_INTERIOR(2), TRAM_LINE(3), SAMPLING_ROUTE(4), 
		GUIDANCE_PATTERN(5), DRAINAGE(6), FENCE(7), FLAG(8), OBSTACLE(9);
		
		public final int number;
		private LineStringType(int number) {
			this.number = number;
		}
		
		public static Optional<LineStringType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<LineStringType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class LineString extends Elem {
		private LineString(Elem parent, LineStringType type) {
			super(parent, "LSG");
			e.setAttribute("A", Integer.toString(type.number));
		}
		public LineString(Partfield parent, LineStringType type) {
			this((Elem)parent, type);
		}
		public LineString(Polygon parent, LineStringType type) {
			this((Elem)parent, type);
		}
		public LineString(GuidancePattern parent, LineStringType type) {
			this((Elem)parent, type);
		}
		
		public LineString setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public LineString setWidth(long width) {
			if(width < 0 || width > 4294967294L)
				throw new IllegalArgumentException("invalid width");
			e.setAttribute("C", Long.toString(width));
			return this;
		}
		
		public LineString setLength(long length) {
			if(length < 0 || length > 4294967294L)
				throw new IllegalArgumentException("invalid length");
			e.setAttribute("D", Long.toString(length));
			return this;
		}
		
		public LineString setColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("E", Integer.toString(color));
			return this;
		}
		
		public LineString setID() {
			e.setAttribute("F", id);
			return this;
		}
		
		public Point.XmlPoint addPoint(Point.PointType type, double north, double east) {
			return new Point.XmlPoint(this, type, north, east);
		}
		
		public Point.BinPoint addPoint() {
			return new Point.BinPoint(this);
		}
	}
	
	public static class GuidanceGroup extends Elem {
		public GuidanceGroup(Partfield parent) {
			super(parent, "GGP");
			e.setAttribute("A", id);
		}
		
		public GuidanceGroup setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public GuidancePattern addGuidancePattern(GuidancePatternType type) {
			return new GuidancePattern(this, type);
		}
		
		public Polygon addPolygon(PolygonType type) {
			return new Polygon(this, type);
		}
	}

	public static enum GuidancePatternType {
		AB(1), A_PLUS(2), CURVE(3), PIVOT(4), SPIRAL(5);
		
		public final int number;
		private GuidancePatternType(int number) {
			this.number = number;
		}
		
		public static Optional<GuidancePatternType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<GuidancePatternType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	public static enum GuidancePatternOptions {
		CLOCKWISE(1), COUNTER_CLOCKWISE(2), FULL_CIRCLE(3);
		
		public final int number;
		private GuidancePatternOptions(int number) {
			this.number = number;
		}
		
		public static Optional<GuidancePatternOptions> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<GuidancePatternOptions> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	public static enum GuidancePatternPropagationDirection {
		BOTH(1), LEFT(2), RIGHT(3), NO_PROP(4);
		
		public final int number;
		private GuidancePatternPropagationDirection(int number) {
			this.number = number;
		}
		
		public static Optional<GuidancePatternPropagationDirection> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<GuidancePatternPropagationDirection> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	public static enum GuidancePatternExtension {
		BOTH(1), FIRST_ONLY(2), LAST_ONLY(3), NO_EXT(4);
		
		public final int number;
		private GuidancePatternExtension(int number) {
			this.number = number;
		}
		
		public static Optional<GuidancePatternExtension> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<GuidancePatternExtension> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	public static enum GuidancePatternGnssMethod {
		GNSS_FIX(1), DGNSS_FIX(2), PRECISE_GNSS(3), RTK_FIXED_INTEGER(4), RTK_FLOAT(5), 
		ESTIMATED_DR_MODE(6), MANUAL_INPUT(7), SIMULATE_MODE(8), DESKTOP_GENERATED_DATA(16), OTHER(17);
		
		public final int number;
		private GuidancePatternGnssMethod(int number) {
			this.number = number;
		}
		
		public static Optional<GuidancePatternGnssMethod> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<GuidancePatternGnssMethod> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class GuidancePattern extends Elem {
		public GuidancePattern(GuidanceGroup parent, GuidancePatternType type) {
			super(parent, "GPN");
			e.setAttribute("A", id);
			e.setAttribute("C", Integer.toString(type.number));
		}
		
		public GuidancePattern setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public GuidancePattern setOptions(GuidancePatternOptions options) {
			e.setAttribute("D", Integer.toString(options.number));
			return this;
		}
		
		public GuidancePattern setPropagationDirection(GuidancePatternPropagationDirection propagationDirection) {
			e.setAttribute("E", Integer.toString(propagationDirection.number));
			return this;
		}
		
		public GuidancePattern setExtension(GuidancePatternExtension extension) {
			e.setAttribute("F", Integer.toString(extension.number));
			return this;
		}
		
		public GuidancePattern setHeading(double heading) {
			if(heading < 0 || heading > 360)
				throw new IllegalArgumentException("invalid heading");
			e.setAttribute("G", floating(heading));
			return this;
		}
		
		public GuidancePattern setRadius(long radius) {
			if(radius < 0 || radius > 4294967294L)
				throw new IllegalArgumentException("invalid radius");
			e.setAttribute("H", Long.toString(radius));
			return this;
		}
		
		public GuidancePattern setGnssMethod(GuidancePatternGnssMethod gnssMethod) {
			e.setAttribute("I", Integer.toString(gnssMethod.number));
			return this;
		}
		
		public GuidancePattern setHorizontalAccuracy(double horizontalAccuracy) {
			if(horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("J", floating(horizontalAccuracy));
			return this;
		}
		
		public GuidancePattern setVerticalAccuracy(double verticalAccuracy) {
			if(verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("K", floating(verticalAccuracy));
			return this;
		}
		
		public GuidancePattern setBaseStation(BaseStation bsn) {
			e.setAttribute("L", bsn.id);
			return this;
		}
		
		public GuidancePattern setOriginalSRID(String srid) {
			e.setAttribute("M", srid);
			return this;
		}
		
		public GuidancePattern setNumberOfSwathsLeft(long swathsLeft) {
			if(swathsLeft < 0 || swathsLeft > 4294967294L)
				throw new IllegalArgumentException("invalid number");
			e.setAttribute("N", Long.toString(swathsLeft));
			return this;
		}
		
		public GuidancePattern setNumberOfSwathsRight(long swathsRight) {
			if(swathsRight < 0 || swathsRight > 4294967294L)
				throw new IllegalArgumentException("invalid number");
			e.setAttribute("O", Long.toString(swathsRight));
			return this;
		}
		
		public LineString addLineString(LineStringType type) {
			return new LineString(this, type);
		}
		
		public Polygon addPolygon(PolygonType type) {
			return new Polygon(this, type);
		}
	}
	
	public static class BaseStation extends Elem {
		public BaseStation(ISO11783_TaskData parent, String designator, 
				double north, double east, int up) {
			super(parent, "BSN");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
			e.setAttribute("C", north(north));
			e.setAttribute("D", east(east));
			e.setAttribute("E", Integer.toString(up));
		}
		
	}
}