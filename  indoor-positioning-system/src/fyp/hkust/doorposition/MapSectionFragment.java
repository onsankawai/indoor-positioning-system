package fyp.hkust.doorposition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class MapSectionFragment extends Fragment{

	private float[] actual_orientation = new float[3];
	private final double stepSize = 0.7;
	private double xmoved = 0;
	private double ymoved = 0;
	float[] mRotationMatrix = new float[16];
	private List<float[]> mAzHist = new ArrayList<float[]>();
	private int mAzHistIndex;
	private int mHistoryMaxLength = 10;
	private float mAz = Float.NaN;
	private SensorManager mAccManager;
	private Sensor mAccelerometer; 

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
	int mSteps = 0;
    
	MapView map;
	MapViewGroup mapViewGroup;
	MapPointer pointer;
	Bitmap mapPic;
	Bitmap pointerPic;
	Paint paint;
	Rect mapDisplayRect;
	//Bitmap bmp2;
	LocationManager locationManager;
	WifiManager mWifiManager;
	WifiDataReceiver mReceiver;
	MapDisplayer mapDisplayer;
	
	String serverStatusMsg;
	final String SERVER_URL = "http://hkust-fyp-ece-td-1-13.appspot.com/update";
	
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
		
		/*
		mapDisplayRect = new Rect(0,0,mapWidth,mapHeight);
		// Create bitmap - MAP
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.atrium_map);
		mapPic = Bitmap.createBitmap(bmp);
		*/
		
		mapDisplayer = new MapDisplayer(this.getActivity());
		
		// Create bitmap - pointer
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pointer);
		pointerPic = Bitmap.createScaledBitmap(bmp, 40, 40,true);
		//pointerPic = Bitmap.createBitmap(bmp);
		
		map = new MapView(this.getActivity());
		mapViewGroup = new MapViewGroup(this.getActivity(),map);
		
		// Create Pointer
		pointer = new MapPointer(gridSize);
		//pointer.computeCoordinate(pointer.refLat2, pointer.refLon2);
		//pointerDisplayRect = new Rect(0,0,40,40);
		
		mWifiManager = (WifiManager)this.getActivity().getSystemService(Context.WIFI_SERVICE);
		mReceiver = new WifiDataReceiver();
		this.getActivity().registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
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
		// 32*32 sq. pixel for N4 phone screen and 768 * 1024 map, 24 * 32 grids
		gridSize = 32;
    }
    
    private StepDisplayer.Listener mStepListener = new StepDisplayer.Listener() {
        public void stepsChanged(int value) {
        		// Calculate distance moved
	    	  	//  distance += stepSize;
               xmoved = stepSize * Math.cos(-mAz);
               ymoved = stepSize * Math.sin(-mAz);
               pointer.computePedometer(xmoved, ymoved, mapDisplayer.currentDisplayRect);
               pointer.decayCredibilityPerStep();
               mWifiManager.startScan();
               mSteps++;
               
	    	  	// set rotation and x,y-translation
			    Matrix mtx = new Matrix();
				mtx.postRotate((float) -(pointer.azimuth_angle + 90), pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				mtx.postTranslate(-pointer.point.x, -pointer.point.y);
				mtx.postTranslate(pointerPic.getWidth()/2, pointerPic.getHeight()/2);
				//pointer.position.set(mtx);
				//mapDisplayer.xformMatrix.set(mtx);
				
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

		        
		        float[] I = new float[16];

		        SensorManager.getRotationMatrix(mRotationMatrix, I, accValues, geomagnetic);		        
		        SensorManager.getOrientation(mRotationMatrix, actual_orientation);
		        
		        
		        //if(setAzHist()) {
		        setAzHist();
		        	LowPassFilter.smoothValues(mAzHist, mAzHistIndex-1);
	    			//mAz = median(mAzHist);
		        	mAz = (mAzHist.get(mAzHistIndex-1))[0];
	    			double newAngle = (360 + Math.toDegrees(mAz)) % 360;
		    	  	pointer.azimuth_angle = Math.abs(pointer.azimuth_angle - newAngle) < 2 ? pointer.azimuth_angle : newAngle;
	    			//pointer.azimuth_angle = newAngle;
		    	  	// set rotation and x,y-translation
				    Matrix mtx = new Matrix();
					mtx.postRotate((float) -(pointer.azimuth_angle + 90), pointerPic.getWidth()/2, pointerPic.getHeight()/2);
					mtx.postTranslate(-pointer.point.x, -pointer.point.y);
					mtx.postTranslate(pointerPic.getWidth()/2, pointerPic.getHeight()/2);
					
					//pointer.position.set(mtx);
					//mapDisplayer.xformMatrix.set(mtx);
		       // }
		        
		        //withoutmedian = (360 + Math.toDegrees(actual_orientation[0])) % 360;
				
		    }
		    	
		    map.postInvalidate();
		    
			
		}  
		
	};
	
	
	private boolean setAzHist()
	{
		
	    float[] hist = actual_orientation;
	    mAzHistIndex %= mHistoryMaxLength;
	    if (mAzHist.size() == mHistoryMaxLength)
	    {
	        mAzHist.remove(mAzHistIndex);
	    }   
	    mAzHist.add(mAzHistIndex++, hist);
	    
	    return (mAzHistIndex == mHistoryMaxLength);
	}


	public float median(List<float[]> values)
	{
	    float[] result = new float[values.size()];
	    int len = result.length;
	    int i = 0;
	    for (float[] value : values)
	    {
	            result[i] = value[0];
	            i++;
	    }

	    float temp;
	    int min;
	    for(int k=0;k<len-1;k++)
	    {
		    min = k;
		    for(int l=k+1;l<len;l++)
		    {
			    if(result[min]>result[l])
			    min = l;
			 }
		    if(min!=k)
		    {
		    	temp = result[min];
			    result[min] = result[k];
			    result[k] = temp;
			}
	    }
	    
	    if (len%2 == 1)
	    	return result[len/2];
	    else
	    	return (float) ((result[len/2-1]+result[len/2])/2.0);
	}
	
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
			//canvas.setMatrix(null);
			canvas.translate(this.getRight()/2 - pointer.point.x, this.getBottom()/2 - pointer.point.y);
			canvas.rotate((float)-(pointer.azimuth_angle - 90), pointer.point.x, pointer.point.y);
			canvas.drawBitmap(mapDisplayer.getCurrentMap().bitmap, null, mapDisplayer.getCurrentDisplayRect(), paint);
			drawGrid(canvas);
			//canvas.drawBitmap(pointerPic, null, pointer.getPointerDisplayRect(), paint);
			//canvas.drawBitmap(pointerPic, pointer.position, paint);

			paint.setColor(getResources().getColor(R.color.red));
			paint.setTextSize(40);
		    //canvas.drawCircle(380, 84, 6, paint);
		    canvas.drawCircle(pointer.point.x, pointer.point.y, 6, paint);
		    canvas.drawCircle(this.getRight()/2, this.getBottom()/2, 6, paint);
		    canvas.drawCircle(14*32+4, 15*32, 6, paint);
		    canvas.drawCircle(16*32-2, 15*32, 6, paint);
		    canvas.drawText("x: "+this.getRight()/2, 0, 300, paint);
		    canvas.drawText("y: "+this.getBottom()/2, 0, 400, paint);
		    //canvas.drawCircle(516, 256, 6, paint);
		    /*
		    canvas.drawText("azimuth: "+pointer.azimuth_angle, 0, 300, paint);
		    canvas.drawText("Step: "+mSteps, 0, 600, paint);
		    canvas.drawText("x: "+xmoved, 0, 700, paint);
		    canvas.drawText("y: "+ymoved, 0, 400, paint);
		    canvas.drawText("rssi: "+mReceiver.rssi, 0, 700, paint);
		    canvas.drawText("cred: "+pointer.credibility, 0, 400, paint);
		    canvas.drawText("radian;"+mAz, 0, 500, paint);
		    canvas.drawText("Server Msg;"+serverStatusMsg, 0, 200, paint);
			*/
		    //canvas.concat(mapDisplayer.xformMatrix);
		    //canvas.rotate((float)(pointer.azimuth_angle - 90), pointer.getCenter().x, pointer.getCenter().y);
		    canvas.setMatrix(null);
		    //canvas.drawBitmap(pointerPic, null, pointer.getPointerDisplayRect(), paint);
		    canvas.drawText("azimuth: "+pointer.azimuth_angle, 0, 700, paint);
		    canvas.drawText("Step: "+mSteps, 0, 600, paint);
		    
		}

		public void handleScroll(float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			
		}
		
		private void drawGrid(Canvas canvas) {
			int horiGrids = mapDisplayer.getCurrentWidth()/gridSize;
			int vertGrids = mapDisplayer.getCurrentHeight()/gridSize;
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
			pointer.setPoint(480, 192);
			pointer.credibility = 1.0;
			pointer.isRSSILocating = true;
			this.invalidate();
		}

	}
	
	class MapViewGroup extends ViewGroup 
	{
		MapView map;
		Button getRefPointBtn;
		ImageView mPointer;
	//	Button calibrateBtn;

		public MapViewGroup(Context context, MapView mapView) {
			super(context);
			// TODO Auto-generated constructor stub
			// Get map vieww 
			map = mapView;
			mPointer = new ImageView(context);
			mPointer.setImageBitmap(pointerPic);
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
			this.addView(mPointer);
	//		this.addView(calibrateBtn);
		}

		
		// Set position of children, (left, top right bottom)
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			// TODO Auto-generated method stub
			
			map.layout(l, t, r, b);
			getRefPointBtn.layout(l, t, r/6, b/6);
			mPointer.layout(r/2-20, b/2-20, r/2+20, b/2+20);
		//	calibrateBtn.layout(l+500, t+850, r, b);
			
		}
		
		
	}
	
	class WifiDataReceiver extends BroadcastReceiver {
		String rssi;
		
		public void onReceive(Context c, Intent intent) {
            List<ScanResult> mScanResults = mWifiManager.getScanResults();
            // Sort the list according to rssi levels in desc order
            Collections.sort(mScanResults, new Comparator<ScanResult>(){

				@Override
				public int compare(ScanResult lhs, ScanResult rhs) {
					// TODO Auto-generated method stub
					return (lhs.level < rhs.level ? 1 : (lhs.level == rhs.level ? 0 : -1));
				}
            	
            });
            rssi = "";
            
            //double signalStrength = 0;
            int count = 0;
            for(ScanResult results : mScanResults){
            	if(count > 3)
            		break;
            	
            	if( results.SSID.equals("sMobileNet") ) {
            		if( rssi.equals(""))
            			rssi += results.BSSID+"!"+results.level;
            		else
            			rssi += "|"+results.BSSID+"!"+results.level;
            		
	            	Log.d("WIFITEST","BSSID:"+results.BSSID);
	            	Log.d("WIFITEST","SSID:"+results.SSID);
	            	Log.d("WIFITEST","frequency:"+results.frequency);
	            	Log.d("WIFITEST","Level:"+results.level);
	            	
            	}
            	count++;
            }
            //rssi = (int)Math.log10(signalStrength);
            if (pointer.isRSSILocating) {
            	new HTTPRequestTask().execute(prepareQueryString(SERVER_URL));
            }
        }
        
        protected String prepareQueryString(String url){
		    if(!url.endsWith("?"))
		        url += "?";

		    List<NameValuePair> params = new LinkedList<NameValuePair>();

		  
		    params.add(new BasicNameValuePair("map_name", "atrium"));
	        params.add(new BasicNameValuePair("xcoor", String.valueOf(pointer.gridPoint.x)));
	        params.add(new BasicNameValuePair("ycoor", String.valueOf(pointer.gridPoint.y)));
		
	        params.add(new BasicNameValuePair("rssi", rssi));
	        params.add(new BasicNameValuePair("credibility",String.valueOf(pointer.credibility)));

		    String paramString = URLEncodedUtils.format(params, "utf-8");

		    url += paramString;
		    return url;
		}
    }
	
	// AsyncTask for updating the MapPointer locations
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
		            //	Log.d("IndoorDebug", "Response status:OK");
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
		        Log.d("IndoorDebug", "Response Msg:"+responseString);
		        return responseString;
		    }

		    @Override
		    protected void onPostExecute(String result) {
		        super.onPostExecute(result);
		        //Do anything with response..
		        try {
		        	if(result != null) {
						JSONObject jsonObj = new JSONObject(result);
						serverStatusMsg = jsonObj.getString("status");
						if (serverStatusMsg.equals("MOBILE_UPDATE") ) {
							pointer.setGridPoint(jsonObj.getInt("xcoor"), jsonObj.getInt("ycoor"));
							pointer.credibility = jsonObj.getDouble("credibility");
							
						}
		        	}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        map.postInvalidate();
		    }
		    
		    
		}

}
