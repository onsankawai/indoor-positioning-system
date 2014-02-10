package fyp.hkust.doorposition;

import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;

public class MapPointer {

	private final double refLat1 = Math.toRadians(22.33766425); // ref Point's latitude (radians) 22.3375019
	private final double refLon1 = Math.toRadians(114.26420386); // ref Point's longitude (radians) 
	public final double refLat2 = 22.337506;
	public final double refLon2 = 114.26414883;
	private final double EARTH_RADIUS = 6371000; // earth's radius in meters
	private final double D_REF_PT_PIXEL = 275.0;
	private final double DEGREE_OFFSET = 90;
	
	private double pixelPerMeter;
	
	private Point refPoint;
	public Point point;
	public Point gridPoint;
	double azimuth_angle;
    double pitch_angle;
    double roll_angle;
    int gridSize;
    Matrix position;
	
	public MapPointer(int gridSize) {
		point = new Point(350,550);
		gridPoint = new Point(350/gridSize, 550/gridSize);
		refPoint = new Point(242,130);
		position = new Matrix();
		double d = computeMeterDistanceFromRef(refLat2,refLon2);
		pixelPerMeter = D_REF_PT_PIXEL / d;
		Log.d("IndoorDebug","pixelPerMeter:"+pixelPerMeter);
		this.gridSize = gridSize;
	}
	
	protected void setPoint(int x, int y) {
		this.point.x = x;
		this.point.y = y;
		this.gridPoint.x = x/gridSize;
		this.gridPoint.y = y/gridSize;
	}
	
	protected void computePedometer(double x, double y) {
		double pixelDistx = x * pixelPerMeter;
		double pixelDisty = y * pixelPerMeter;
		Log.d("IndoorDebug","pixelDist:"+pixelDistx+pixelDisty);
		
		setPoint((int) (point.x + pixelDistx),  (int) (point.y - pixelDisty));
		//point.x = (int) (point.x + pixelDistx);
		//point.y = (int) (point.y - pixelDisty);
		Log.d("IndoorDebug","gridx:"+gridPoint.x);
		Log.d("IndoorDebug","gridy:"+gridPoint.y);
		//Log.d("IndoorDebug","bearing:"+bearing);
		
	}
	
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
	
	
}
