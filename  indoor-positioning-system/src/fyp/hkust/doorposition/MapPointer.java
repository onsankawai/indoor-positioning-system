package fyp.hkust.doorposition;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

public class MapPointer {

	public final double refLat2 = 22.337506;
	public final double refLon2 = 114.26414883;
	private final double PIXEL_PER_METER = 60.0/3.72;
	
	protected Point center;
	protected Rect pointerDisplayRect;
	public Point point;
	public Point gridPoint;
	public Point prevGridPoint;
	double azimuth_angle;
    double pitch_angle;
    double roll_angle;
    int gridSize;
    Matrix position;
    boolean isRSSILocating = false;
    double credibility = 0.0;
	
	public MapPointer(int gridSize) {
		point = new Point(350,550);
		gridPoint = new Point(350/gridSize, 550/gridSize);
		prevGridPoint = new Point(gridPoint.x, gridPoint.y);
		new Point(242,130);
		position = new Matrix();
		//double d = computeMeterDistanceFromRef(refLat2,refLon2);
		Log.d("IndoorDebug","pixelPerMeter:"+PIXEL_PER_METER);
		this.gridSize = gridSize;
	}
	
	public Rect getPointerDisplayRect() {
		return pointerDisplayRect;
	}
	
	public Point getCenter() {
		return center;
	}
	
	protected void setPoint(int x, int y) {
		this.point.x = x;
		this.point.y = y;
		this.prevGridPoint.x = this.gridPoint.x;
		this.prevGridPoint.y = this.gridPoint.y;
		this.gridPoint.x = x/gridSize;
		this.gridPoint.y = y/gridSize;
	}
	
	protected boolean isGridChanged() {
		return (this.prevGridPoint.x == this.gridPoint.x && this.prevGridPoint.y == this.gridPoint.y ? false : true);
	}
	
	protected void setGridPoint(int grid_x, int grid_y) {
		this.gridPoint.x = grid_x;
		this.gridPoint.y = grid_y;
		this.point.x = this.gridPoint.x * gridSize + gridSize/2;
		this.point.y = this.gridPoint.y * gridSize + gridSize/2;
	}
	
	protected void computePedometer(double x, double y, Rect boundary) {
		double pixelDistx = x * PIXEL_PER_METER;
		double pixelDisty = y * PIXEL_PER_METER;
		Log.d("IndoorDebug","pixelDist:"+pixelDistx+pixelDisty);
		int newX = (int) (point.x - pixelDistx);
		int newY = (int) (point.y + pixelDisty);
		if (boundary.contains(newX, newY)) {
			setPoint(newX,  newY);
		
		}
		//point.x = (int) (point.x + pixelDistx);
		//point.y = (int) (point.y - pixelDisty);
		Log.d("IndoorDebug","gridx:"+gridPoint.x);
		Log.d("IndoorDebug","gridy:"+gridPoint.y);
		//Log.d("IndoorDebug","bearing:"+bearing);
		
	}
	/*
	protected void computeCoordinate(double lat, double lon) {
		double d = computeMeterDistanceFromRef(lat,lon);
		double pixelDist = d * pixelPerMeter;
		Log.d("IndoorDebug","pixelDist:"+pixelDist);
		
		double bearing = computeAdjustedBearingFromRef(lat,lon);
		point.x = (int) (refPoint.x + pixelDist * Math.sin(bearing));
		point.y = (int) (refPoint.y - pixelDist * Math.cos(bearing));
		Log.d("IndoorDebug","x:"+point.x);
		Log.d("IndoorDebug","y:"+point.y);
		//Log.d("IndoorDebug","bearing:"+bearing);
		
	}
	
	// Return distance between a point and the ref point in meters
	protected double computeMeterDistanceFromRef(double lat, double lon) {
		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);
		double dLat = refLat1 - lat;
		double dLon = refLon1 - lon;
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) 
				+ Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(refLat1) * Math.cos(lat);
		
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = EARTH_RADIUS * c;
		
		return d;
	}
	
	// Return true bearing from ref point to a point, adjusted by orientation
	protected double computeAdjustedBearingFromRef(double lat, double lon) {
		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);
		double dLon = lon - refLon1;
		double y = Math.sin(dLon) * Math.cos(lat);
		double x = Math.cos(refLat1)*Math.sin(lat) -
		        Math.sin(refLat1)*Math.cos(lat)*Math.cos(dLon);
		double bearing = Math.atan2(y, x);
		// convert bearing to true bearing from 0 to 360
		bearing = Math.toDegrees(bearing);  // to degree
		Log.d("IndoorDebug","bearing:"+bearing);
		bearing = (bearing + 360) % 360;
		
		// degree offset for the map
		bearing = (bearing - DEGREE_OFFSET) % 360;
		bearing = Math.toRadians(bearing);
		
		return bearing;
	}
	*/
	protected void decayCredibilityPerStep() {
		credibility -= 0.02;
		
	}
	
	
	
}
