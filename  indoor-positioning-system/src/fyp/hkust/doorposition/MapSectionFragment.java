package fyp.hkust.doorposition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

public class MapSectionFragment extends Fragment{

	private int mapWidth = 0;
	private int mapHeight = 0;
	private float accuracy = 0;
	
	private float[] actual_orientation = new float[3];
	private int mSteps;
	private final double stepSize = 0.7;
	private double xmoved = 0;
	private double ymoved = 0;
	private double latitude = 0;
	private double longitude = 0;
	
	double x = 0;     // current position
	double vx = 0;     // current velocity
	double y = 0;     // current y position
	double vy = 0;     // current y velocity
	long lastTime = System.currentTimeMillis();      // previous time, nanoseconds
	long newTime = 0;       // current time
	float accelx = 0;
	float accely = 0;
	float accelz = 0;
	float preAccelx = 0;
	float preAccely = 0;
	float preAccelz = 0;
	float resultAccelx = 0;
	float resultAccely = 0;
	float resultAccelz = 0;
	float filteredx = 0;
	float filteredy = 0;
	double timepassed = 0;
	
	private SensorManager mAccManager;
	private Sensor mAccelerometer; 

	private SensorManager mLinearManager;
	private Sensor mLinear; 
	
	private SensorManager mMagManager;
	private Sensor mMagnetic;
	
	private SensorManager mSensorManager;
    private Sensor mSensor;
    private StepDetector mStepDetector;
    
    private StepDisplayer mStepDisplayer;
    
	double azimuth_angle;
    double pitch_angle;
    double roll_angle;
    float geomagnetic[];
    float accValues[];
    float linearValues[];
    boolean sensorReady;
	int gridSize;
    
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
	
		// init sensors, pedometers
		initSensors();
		
		// init screen grids
		initScreenGrid();
		
		// Create paint object
		paint = new Paint();
		
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
		pointer = new MapPointer(gridSize);
		pointer.computeCoordinate(pointer.refLat2, pointer.refLon2);
		//pointerDisplayRect = new Rect(0,0,40,40);
		
		return mapViewGroup;
		
	}
	
    private void registerDetector() {
        mSensor = mSensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER /*| 
            Sensor.TYPE_MAGNETIC_FIELD | 
            Sensor.TYPE_ORIENTATION*/);
        mSensorManager.registerListener(mStepDetector,
            mSensor,
            SensorManager.SENSOR_DELAY_FASTEST);
    }
    
    private void initSensors() {
    	// magnetic field sensor init
		mMagManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mMagnetic = mMagManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    mMagManager.registerListener(sensorListener, mMagnetic, SensorManager.SENSOR_DELAY_GAME);
		
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
	    
	    // Step Detector init
	    mSensorManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	    mStepDetector = new StepDetector();
	    registerDetector();
	    
	    // Step Displayer Init
	    mStepDisplayer = new StepDisplayer();
        mStepDisplayer.setSteps(0);
        mStepDisplayer.addListener(mStepListener);
        mStepDetector.addStepListener(mStepDisplayer);
    }
      
    // 768 * 1024 for Nexus 4, 32*32 gridSize
    private void initScreenGrid() {
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
		mapHeight = height-160;
		
		// 32*32 sq. pixel for N4 phone screen and 768 * 1024 map, 24 * 32 grids
		gridSize = 32;
    }
    
    private StepDisplayer.Listener mStepListener = new StepDisplayer.Listener() {
        public void stepsChanged(int value) {
        		mSteps = value;

	            // Calculate distance moved
	    	  	//  distance += stepSize;
               xmoved = stepSize * Math.cos(-actual_orientation[0]);
               ymoved = stepSize * Math.sin(-actual_orientation[0]);
               pointer.computePedometer(xmoved, ymoved);
               
	    	  	// set rotation and x,y-translation
			    Matrix mtx = new Matrix();
				mtx.postRotate((float) pointer.azimuth_angle - 90, pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				mtx.postTranslate(pointer.point.x, pointer.point.y);
				mtx.postTranslate(-pointerPic.getWidth()/2, -pointerPic.getHeight()/2);
				pointer.position.set(mtx);
				//pointer.position.setRotate((float) pointer.azimuth_angle - 90, pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				//pointer.position.postTranslate(pointer.point.x, pointer.point.y);
				
		    }
            
            
        
    };	

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
	/*	    if (linearValues != null) {
		        accelx = linearValues[0] * 100 ;   // X axis is our axis of acceleration
		        accely = linearValues[1] * 100 ;   // Y axis is our axis of acceleration
		        accelz = linearValues[2] * 100 ;   // Y axis is our axis of acceleration
		    /*    preAccelx = accelx * kFilteringFactor + preAccelx * (1.0f - kFilteringFactor); 
		        preAccely = accely * kFilteringFactor + preAccely * (1.0f - kFilteringFactor);
		        resultAccelx = accelx - preAccelx;
		        resultAccely = accely - preAccely;   */
/*		        float updateFreq = 30; // match this to your update speed
		        float cutOffFreq = 0.9f;
		        float RC = 1.0f / cutOffFreq;
		        float dt = 1.0f / updateFreq;
		        float filterConstant = RC / (dt + RC);
		        float alpha = filterConstant; 
		        float kAccelerometerMinStep = 0.033f;
		        float kAccelerometerNoiseAttenuation = 3.0f;
	            float d = clamp(Math.abs(norm(preAccelx, preAccely, preAccelz) - norm(accelx, accely, accelz)) 
	            		/ kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
	            alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;
	        

		        preAccelx = (float) (alpha * (preAccelx + accelx - resultAccelx));
		        preAccely = (float) (alpha * (preAccely + accely - resultAccely));
		        preAccelz = (float) (alpha * (preAccelz + accelz - resultAccelz));

		        resultAccelx = accelx;
		        resultAccely = accely;
		        resultAccelz = accelz;
		        newTime = event.timestamp;
		        timepassed = (newTime - lastTime) * 0.000000001;
		        vx += accelx * timepassed;
		        x += vx * timepassed;
		        vy += accely * timepassed;
		        y += vy * timepassed;
		        lastTime = newTime;
		           
		    } */
		    	
		    map.postInvalidate();
		    
			
		}  
		
	};
	
	/*	private void calibration() {
		xNoise = accelx;
		yNoise = accely;
		filteredx = preAccelx;
		filteredy = preAccely;
	}  */
	// Map view
	class MapView extends View
	{
		Paint gridPaint;

		public MapView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			gridPaint = new Paint();
			gridPaint.setColor(getResources().getColor(R.color.light_grey));
			gridPaint.setAlpha(127);
			gridPaint.setStyle(Paint.Style.FILL);
		}
		
		
		@Override
		protected void onDraw(Canvas canvas){
			
			canvas.drawBitmap(mapPic, null, mapDisplayRect, paint);
			drawGrid(canvas);
			canvas.drawBitmap(pointerPic, pointer.position, paint);

			paint.setColor(getResources().getColor(R.color.red));
			paint.setTextSize(40);
		    //canvas.drawCircle(380, 84, 6, paint);
		    canvas.drawCircle(380, 256, 6, paint);
		    canvas.drawCircle(516, 256, 6, paint);
		    canvas.drawText("azimuth: "+pointer.azimuth_angle, 0, 300, paint);
		    canvas.drawText("Step: "+mSteps, 0, 600, paint);
		    //canvas.drawText("x: "+xmoved, 0, 700, paint);
		    //canvas.drawText("y: "+ymoved, 0, 400, paint);
		    canvas.drawText("x: "+pointer.gridPoint.x, 0, 700, paint);
		    canvas.drawText("y: "+pointer.gridPoint.y, 0, 400, paint);
		    canvas.drawText("radian;"+actual_orientation[0], 0, 500, paint);

		}

		public void handleScroll(float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			
		}
		
		private void drawGrid(Canvas canvas) {
			int horiGrids = 768/gridSize;
			int vertGrids = 1024/gridSize;
			for(int i = 1; i < horiGrids; i++) {
				canvas.drawLine(i*gridSize, 0, i*gridSize, 1024, gridPaint);
			}
			for(int j = 1; j < vertGrids; j++) {
				canvas.drawLine(0, j*gridSize, 768, j*gridSize, gridPaint);
			}
			// highlight the current grid
			int gridPosX = pointer.gridPoint.x * gridSize;
			int gridPosY = pointer.gridPoint.y * gridSize;
			canvas.drawRect(gridPosX, 
							gridPosY, 
							gridPosX + gridSize, 
							gridPosY + gridSize, 
							gridPaint);
		}
		
		public void getRefPoint() {
			pointer.setPoint(380, 256);
			this.invalidate();
		}

	}
	
	class MapViewGroup extends ViewGroup 
	{
		MapView map;
		Button getRefPointBtn;
	//	Button calibrateBtn;

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
			
			// Calibrate Button
	/*		calibrateBtn = new Button(context);
			calibrateBtn.setHeight(80);
			calibrateBtn.setWidth(100);
			calibrateBtn.setText("Start");
			calibrateBtn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
			calibrateBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					calibration();
				}
				
			});  */
			
			this.addView(map);
			this.addView(getRefPointBtn);
	//		this.addView(calibrateBtn);
		}

		// Set position of children, (left, top right bottom)
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			// TODO Auto-generated method stub
			
			map.layout(l, t, r, b);
			getRefPointBtn.layout(l, t, r/6, b/6);
		//	calibrateBtn.layout(l+500, t+850, r, b);
			
		}
		
	}
	
	class HTTPRequestTask extends AsyncTask<String, String, String>{

	    @Override
	    protected String doInBackground(String... uri) {
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpResponse response;
	        String responseString = null;
	        try {
	            response = httpclient.execute(new HttpGet(uri[0]));
	            StatusLine statusLine = response.getStatusLine();
	            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                response.getEntity().writeTo(out);
	                out.close();
	                responseString = out.toString();
	            } else{
	                //Closes the connection.
	                response.getEntity().getContent().close();
	                throw new IOException(statusLine.getReasonPhrase());
	            }
	        } catch (ClientProtocolException e) {
	            //TODO Handle problems..
	        } catch (IOException e) {
	            //TODO Handle problems..
	        }
	        return responseString;
	    }

	    @Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);
	        //Do anything with response..
	    }
	}
	

}
