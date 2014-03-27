package fyp.hkust.doorposition;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

public class MapDisplayer {
	protected IndoorMap currentMap;
	protected Rect currentDisplayRect;
	protected ArrayList<IndoorMap> mapList;
	
	
	MapDisplayer(Context context){
		mapList = new ArrayList<IndoorMap>();
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.atrium_map);
		Bitmap mapPic = Bitmap.createBitmap(bmp);
		double angleOffset = Double.valueOf(context.getResources().getString(R.string.atrium_map_offset));
		IndoorMap indoorMap = new IndoorMap(mapPic, angleOffset);
		mapList.add(indoorMap);
		
		bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.concourse_1f);
		mapPic = Bitmap.createBitmap(bmp);
		angleOffset = 0;
		indoorMap = new IndoorMap(mapPic, angleOffset);
		mapList.add(indoorMap);
		
		bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.concourse_2f);
		mapPic = Bitmap.createBitmap(bmp);
		angleOffset = 0;
		indoorMap = new IndoorMap(mapPic, angleOffset);
		mapList.add(indoorMap);
		
		currentMap = mapList.get(0);
		currentDisplayRect = new Rect(0,0,currentMap.bitmap.getWidth(),currentMap.bitmap.getHeight());
		Log.d("MAPSIZE","Width:"+currentMap.bitmap.getWidth());
		Log.d("MAPSIZE","Height:"+currentMap.bitmap.getHeight());
	}
	
	protected IndoorMap getCurrentMap(){
		return currentMap;
	}
	
	protected Rect getCurrentDisplayRect() {
		return currentDisplayRect;
	}
	
	protected int getCurrentWidth() {
		return currentMap.bitmap.getWidth();
	}
	
	protected int getCurrentHeight() {
		return currentMap.bitmap.getHeight();
	}
	
	/*
	 * hold on! to be continued
	 */
	class IndoorMap {
		Bitmap bitmap;
		ArrayList<Point> switchingPoint;
		double angleOffset; 
		
		// Initialize map and angle offset
		IndoorMap(Bitmap rawmap, double offset) {
			bitmap = rawmap;
			angleOffset = offset;
			switchingPoint = new ArrayList<Point>();
		}
		
		protected void addSwitchingPoint(Point pt) {
			switchingPoint.add(pt);
		}
		
		protected boolean isSwitchingPoint(Point pt) {
			return switchingPoint.contains(pt);
		}
	}

}
