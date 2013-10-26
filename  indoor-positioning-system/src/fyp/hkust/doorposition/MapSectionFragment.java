package fyp.hkust.doorposition;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

public class MapSectionFragment extends Fragment{

	private static final int MAP_X_MAX = 800;
	private static final int MAP_Y_MAX = 600;
	private int mapWidth = 0;
	private int mapHeight = 0;
	private int x_coor = 0;
	private int y_coor = 0;
	private float accuracy = 0;
	
	
	private double latitude = 0;
	private double longitude = 0;
	private double bearing = 0;
	
	private float xAxis = 0;
	private float yAxis = 0;
	private float zAxis = 0;
	
	double x = 0;     // current position
	double vx = 0;     // current velocity
	double y = 0;     // current y position
	double vy = 0;     // current y velocity
	long lastTime = System.currentTimeMillis();      // previous time, nanoseconds
	long newTime = 0;       // current time
	float accelx = 0;
	float accely = 0;
	double timepassed = 0;
	
	private SensorManager mAccManager;
	private Sensor mAccelerometer; 

	private SensorManager mLinearManager;
	private Sensor mLinear; 
	
	private SensorManager mMagManager;
	private Sensor mMagnetic; 
	
	double azimuth_angle;
    double pitch_angle;
    double roll_angle;
    float geomagnetic[];
    float accValues[];
    float linearValues[];
    boolean sensorReady;
	
	MapView map;
	MapViewGroup mapViewGroup;
	MapPointer pointer;
	Bitmap mapPic;
	Bitmap pointerPic;
	Paint paint;
	Rect mapDisplayRect;
	Rect ptrDisplayRect;
	//Bitmap bmp2;
	LocationManager locationManager;
	
	public MapSectionFragment() {
	}


	
	// Main create view
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//View rootView = inflater.inflate(R.layout.map,container, false);

		
		//TextView titleTextView = (TextView) rootView.findViewById(R.id.view_map_title);
		//titleTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
		//titleTextView.setText(R.string.title_map);
		//titleTextView.setTextColor(getResources().getColor(R.color.light_grey));

		// magnetic field sensor init
		mMagManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mMagnetic = mMagManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    mMagManager.registerListener(sensorListener, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
		
		// accelerometer sensor init
		mAccManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mAccelerometer = mAccManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    mAccManager.registerListener(sensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL); 
		
	    // linear accelerometer senor init
	    mLinearManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mLinear = mLinearManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
	    mLinearManager.registerListener(sensorListener, mLinear, SensorManager.SENSOR_DELAY_NORMAL); 
		
		// location manager init
		//locationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);	
		//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		
		// Create paint object
		paint = new Paint();
		
		// Get display region
		WindowManager wm = (WindowManager) this.getActivity().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		Log.d("IndoorDebug", "Width:" + width);
		Log.d("IndoorDebug", "Height:" + height);
		mapWidth = width;
		mapHeight = height-242;
		
		mapDisplayRect = new Rect(0,0,mapWidth,mapHeight);
		// Create bitmap - MAP
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.atrium_map);
		mapPic = Bitmap.createBitmap(bmp);
		
		ptrDisplayRect = new Rect(350, 550, 400, 600); 
		// Create bitmap - pointer
		bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pointer);
		pointerPic = Bitmap.createScaledBitmap(bmp, 40, 40,true);
		//pointerPic = Bitmap.createBitmap(bmp);
		
		map = new MapView(this.getActivity());
		mapViewGroup = new MapViewGroup(this.getActivity(),map);
		
		// Create Pointer
		pointer = new MapPointer();
		pointer.computeCoordinate(pointer.refLat2, pointer.refLon2);
		//pointerDisplayRect = new Rect(0,0,40,40);
		
		return mapViewGroup;
		
	}

	// GPS location sensor
	protected LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			//Log.d("IndoorDebug", "latitude:" + location.getLatitude());
			//Log.d("IndoorDebug", "longitude:" + location.getLongitude());
			accuracy = location.getAccuracy();
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			if (accuracy <= 6.0) {
				
				//bearing = location.getBearing();
				pointer.computeCoordinate(latitude, longitude);
				x_coor = pointer.point.x;
				y_coor = pointer.point.y;
			//	pointerDisplayRect = new Rect(x_coor-20,y_coor-20,x_coor+20,y_coor+20);
				//map.postInvalidate();
			}
			map.postInvalidate();
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	// Orientation, motion sensor
	protected SensorEventListener sensorListener = new SensorEventListener() {
		

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub

			switch (event.sensor.getType()) {
		    	case Sensor.TYPE_MAGNETIC_FIELD:
		    		geomagnetic = event.values.clone();
		    		sensorReady = true;
		    		break;
		    	case Sensor.TYPE_ACCELEROMETER:
		        	accValues = event.values.clone();
		    	case Sensor.TYPE_LINEAR_ACCELERATION:
		    		linearValues = event.values.clone();
		    }   

		    if (geomagnetic != null && accValues != null && sensorReady) {
		        sensorReady = false;

		        float[] R = new float[16];
		        float[] I = new float[16];

		        SensorManager.getRotationMatrix(R, I, accValues, geomagnetic);

		        float[] actual_orientation = new float[3];
		        SensorManager.getOrientation(R, actual_orientation);
	    	  	pointer.azimuth_angle = (360 + Math.toDegrees(actual_orientation[0])) % 360;
	    	  	
	    	  	// set rotation and x,y-translation
			    Matrix mtx = new Matrix();
				mtx.postRotate((float) pointer.azimuth_angle - 90, pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				mtx.postTranslate(pointer.point.x, pointer.point.y);
				mtx.postTranslate(-pointerPic.getWidth()/2, -pointerPic.getHeight()/2);
				pointer.position.set(mtx);
				//pointer.position.setRotate((float) pointer.azimuth_angle - 90, pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				//pointer.position.postTranslate(pointer.point.x, pointer.point.y);
				
		    }
		    if (linearValues != null) {
		        accelx = linearValues[0] * 100 ;   // X axis is our axis of acceleration
		        accely = linearValues[1] * 100 ;   // Y axis is our axis of acceleration
		        newTime = event.timestamp;
		        timepassed = (newTime - lastTime) * 0.000000001;
		        vx += accelx * timepassed;
		        x += vx * timepassed;
		        vy += accely * timepassed;
		        y += vy * timepassed;
		        lastTime = newTime;
		           
		    }
		    	
		    map.postInvalidate();
		    
			
		}  
		
	};
	

	// Map view
	class MapView extends View
	{

		public MapView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub

			
		}
		
		
		@Override
		protected void onDraw(Canvas canvas){
			canvas.drawBitmap(mapPic, null, mapDisplayRect, paint);
			//pointerPic = Bitmap.createBitmap(bmp2, 0, 0, bmp2.getWidth(), bmp2.getHeight(), mtx, true);
			canvas.drawBitmap(pointerPic, pointer.position, paint);
		//	canvas.drawBitmap(pointerPic, mtx, paint);
			//canvas.drawColor(R.color.red);
			paint.setColor(getResources().getColor(R.color.red));
			paint.setTextSize(40);
		    //canvas.drawCircle(380, 84, 6, paint);
		    canvas.drawCircle(380, 240, 6, paint);
		    canvas.drawCircle(516, 240, 6, paint);
		    canvas.drawText("azimuth: "+pointer.azimuth_angle, 0, 300, paint);
		    canvas.drawText("Lat: "+latitude, 0, 600, paint);
		    canvas.drawText("Long: "+longitude, 0, 700, paint);
		    canvas.drawText("Velocity X:"+vx, 0, 400, paint);
		    canvas.drawText("Position X: "+x, 0, 500, paint);
		    canvas.drawText("Linear x: "+accelx, 0, 800, paint);
		    canvas.drawText("Velovity Y: "+vy, 0, 200, paint);
		    canvas.drawText("Position Y: "+y, 0, 100, paint);
		    canvas.drawText("Linear y: "+accely, 0, 900, paint);
		    //canvas.drawText("Bearing:"+bearing, 0, 800, paint);
		}

		public void handleScroll(float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			
		}
		
		public void getRefPoint() {
			pointer.point.x = 380;
			pointer.point.y = 240;
			this.invalidate();
		}

	}
	
	class MapViewGroup extends ViewGroup 
	{
		MapView map;
		Button getRefPointBtn;

		public MapViewGroup(Context context, MapView mapView) {
			super(context);
			// TODO Auto-generated constructor stub
			// Get map vieww 
			map = mapView;
			
			// Ref Point Btn
			getRefPointBtn = new Button(context);
			getRefPointBtn.setHeight(100);
			getRefPointBtn.setWidth(200);
			getRefPointBtn.setText("Get Ref");
			getRefPointBtn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, 
                    LayoutParams.WRAP_CONTENT));
			getRefPointBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					map.getRefPoint();
				}
				
			});
			
			this.addView(map);
			this.addView(getRefPointBtn);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			// TODO Auto-generated method stub
			
			map.layout(l, t, r, b);
			getRefPointBtn.layout(l, t, r/6, b/6);
			
		}
		
	}
	

}
