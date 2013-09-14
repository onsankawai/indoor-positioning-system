package fyp.hkust.doorposition;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class MapSectionFragment extends Fragment{

	private static final int MAP_X_MAX = 800;
	private static final int MAP_Y_MAX = 600;
	private int scrollX = 0;
	private int scrollY = 0;

	private double latitude = 0;
	private double longitude = 0;
	private double bearing = 0;
	
	private SensorManager mSensorManager;
	private Sensor mOrientation;
	
	float azimuth_angle;
    float pitch_angle;
    float roll_angle;
	
	MapView map;
	Bitmap mapPic;
	Bitmap pointerPic;
	Paint paint;
	Rect mapDisplayRect;
	LocationManager locationManager;
	
	public MapSectionFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//View rootView = inflater.inflate(R.layout.map,container, false);

		
		//TextView titleTextView = (TextView) rootView.findViewById(R.id.view_map_title);
		//titleTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
		//titleTextView.setText(R.string.title_map);
		//titleTextView.setTextColor(getResources().getColor(R.color.light_grey));

		// sensor init
		mSensorManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	    mSensorManager.registerListener(sensorListener, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
		
		// location manager init
		locationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		
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
		
		mapDisplayRect = new Rect(0,0,width,height-242);
		// Create bitmap - MAP
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.atrium_map);
		mapPic = Bitmap.createBitmap(bmp);
		
		// Create bitmap - pointer
		bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pointer);
	    pointerPic = Bitmap.createBitmap(bmp);
		
		map = new MapView(this.getActivity());
		
		return map;
		
	}

	
	protected LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			Log.d("IndoorDebug", "latitude:" + location.getLatitude());
			Log.d("IndoorDebug", "longitude:" + location.getLongitude());
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			bearing = location.getBearing();
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
	
	protected SensorEventListener sensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			azimuth_angle = event.values[0];
		    pitch_angle = event.values[1];
		    roll_angle = event.values[2];
		    map.postInvalidate();
			
		}
		
	};
	
	class MapView extends View
	{

		public MapView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		
		
		@Override
		protected void onDraw(Canvas canvas){
			canvas.drawBitmap(mapPic, null, mapDisplayRect, paint);
			//canvas.drawBitmap(pointer, null, new Rect(), paint);
			//canvas.drawColor(R.color.red);
			paint.setColor(getResources().getColor(R.color.red));
			paint.setTextSize(40);
		    canvas.drawCircle(380, 84, 6, paint);
		    canvas.drawCircle(380, 240, 6, paint);
		    canvas.drawCircle(516, 240, 6, paint);
		    canvas.drawText("azimuth: "+azimuth_angle, 0, 300, paint);
		    canvas.drawText("pitch: "+pitch_angle, 0, 400, paint);
		    canvas.drawText("roll:"+roll_angle, 0, 500, paint);
		    canvas.drawText("Lat: "+latitude, 0, 600, paint);
		    canvas.drawText("Long: "+longitude, 0, 700, paint);
		    canvas.drawText("Bearing:"+bearing, 0, 800, paint);
		}

		public void handleScroll(float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
