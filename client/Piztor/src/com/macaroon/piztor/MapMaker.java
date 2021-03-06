package com.macaroon.piztor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.DropBoxManager.Entry;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKOLUpdateElement;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MapMaker {

	// MapView controlling component
	MapView mMapView = null;
	MapController mMapController = null;

	// Default center
	private final static GeoPoint sjtuCenter = new GeoPoint(
			(int) (31.032247 * 1E6), (int) (121.445937 * 1E6));

	// Map layers and items
	MyOverlay mOverlay = null;
	private OverlayItem curItem = null;
	private LocationOverlay mLocationOverlay;
	private ArrayList<OverlayItem> mItems = null;
	private MapInfo preMapInfo = null;
	private MapInfo nowMapInfo = null;
	private Vector<UserInfo> freshManInfo = null;

	// hash from uid to overlay item
	private HashMap<Integer, OverlayItem> hash = null;
	private HashMap<OverlayItem, Integer> markerToInt = null;

	// marker layer
	OverlayItem nowMarker = null;
	static int nowMarkerHour;
	static int nowMarkerMinute;
	static long nowMarkerTimestamp;
	static int newMarkerHour;
	static int newMarkerMinute;
	static long newMarkerTimestamp;
	private int markerIndex;
	private int nowMarkerLevel;

	// Popup component
	PopupOverlay popLay = null;
	private TextView popupText = null;
	private TextView leftText = null;
	private View viewCache = null;
	private View popupInfo = null;
	private View popupLeft = null;
	private View popupRight = null;

	// misc
	private Context context;
	private LocationManager locationManager = null;
	boolean isGPSEnabled;
	private int[] myIcons;
	private Drawable[] myBM;
	private final int iconNum = 9;

	myApp app;

	/**
	 * Constructor
	 */
	public MapMaker(MapView mapView, Context cc, myApp app) {
		this.app = app;
		mMapView = mapView;
		mMapController = mMapView.getController();

		mMapController.setCenter(sjtuCenter);
		mMapController.setZoom(16);
		mMapController.setRotation(-22);
		mMapController.enableClick(true);

		context = cc;
		mLocationOverlay = null;
		mOverlay = null;
	}

	/**
	 * Layer for my location
	 */
	public class LocationOverlay extends MyLocationOverlay {

		public LocationOverlay(MapView mapView) {
			super(mapView);
		}
	}

	/**
	 * Layer for items(other users)
	 */
	public class MyOverlay extends ItemizedOverlay {

		public MyOverlay(Drawable defaultMaker, MapView mapView) {
			super(defaultMaker, mapView);
		}

		@Override
		public boolean onTap(int index) {

			if (nowMarker != null && index == markerIndex) {
				OverlayItem item = getItem(index);
				leftText.setText(nowMarkerHour + "点");
				popupText.setText(nowMarkerMinute + "分");
				Bitmap bitmap[] = { BMapUtil.getBitmapFromView(popupLeft),
						BMapUtil.getBitmapFromView(popupInfo),
						BMapUtil.getBitmapFromView(popupRight), };
				popLay.showPopup(bitmap, item.getPoint(), 32);
			} else {
				OverlayItem item = getItem(index);
				UserInfo tmpInfo = app.mapInfo.getUserInfo(markerToInt.get(item));
				String itemInfo = tmpInfo.company + "连" + tmpInfo.section + "班 " + tmpInfo.nickname;
				popupText.setText(itemInfo);
				Bitmap bitmap = BMapUtil.getBitmapFromView(popupInfo);
				popLay.showPopup(bitmap, item.getPoint(), 32);
			}
			return true;
		}

		@Override
		public boolean onTap(GeoPoint pt, MapView mapView) {

			if (popLay != null) {
				popLay.hidePop();
			}
			return false;
		}
	}

	/**
	 * Initialize location layer
	 */
	public void InitLocationOverlay() {

		mLocationOverlay = new LocationOverlay(mMapView);
		LocationData locationData = new LocationData();
		mLocationOverlay.setData(locationData);
		mMapView.getOverlays().add(mLocationOverlay);
		mLocationOverlay.enableCompass();
		mMapView.refresh();
	}

	/**
	 * Initialize other users layer
	 */
	public void InitMyOverLay() {

		// TODO
		// ///////////////////////////////////////////////////////////////
		hash = new HashMap<Integer, OverlayItem>();
		markerToInt = new HashMap<OverlayItem, Integer>();
		mOverlay = new MyOverlay(context.getResources().getDrawable(
				R.drawable.circle_red), mMapView);
		mMapView.getOverlays().add(mOverlay);
	}

	/**
	 * Initialize popup
	 */
	public void InitPopup() {

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewCache = inflater.inflate(R.layout.custom_text_view, null);
		popupInfo = (View) viewCache.findViewById(R.id.popinfo);
		popupLeft = (View) viewCache.findViewById(R.id.popleft);
		popupRight = (View) viewCache.findViewById(R.id.popright);
		popupText = (TextView) viewCache.findViewById(R.id.textcache);
		leftText = (TextView) viewCache.findViewById(R.id.popleft);

		PopupClickListener popListener = new PopupClickListener() {

			@Override
			public void onClickedPopup(int index) {
				// when the popup is clicked
				if (index == 0) {
					// do nothing
				}
				if (index == 2) {
					// remove current marker if is higher or equal level
					if (app.mapInfo.myInfo.level >= nowMarkerLevel) {
						AlertMaker removeAlert = new AlertMaker(context, MapMaker.this);
						removeAlert.showRemoveMarkerAlert();
					} else {
						Toast.makeText(context, "权限不足", Toast.LENGTH_SHORT).show();
					}
				}
			}
		};

		popLay = new PopupOverlay(mMapView, popListener);
	}

	/**
	 * Initialize touch listener
	 */
	// moved to main

	/**
	 * Initialize map
	 */
	public void InitMap() {

		InitLocationOverlay();
		InitMyOverLay();
		InitPopup();
		myBM = new Drawable[20];
		initMyIcons();
		preMapInfo = null;
		// InitTouchListenr();
	}

	public void initMyIcons() {
		myBM[0] = context.getResources().getDrawable(R.drawable.circle_blue);
		myBM[1] = context.getResources().getDrawable(R.drawable.circle_red);
		myBM[2] = context.getResources().getDrawable(R.drawable.circle_glass);
		myBM[3] = context.getResources().getDrawable(R.drawable.circle_yellow);
		myBM[4] = context.getResources().getDrawable(R.drawable.circle_wood);
		myBM[5] = context.getResources().getDrawable(R.drawable.circle_green);
		myBM[6] = context.getResources().getDrawable(R.drawable.circle_metal);
		myBM[7] = context.getResources().getDrawable(R.drawable.circle_paper);
		myBM[8] = context.getResources().getDrawable(R.drawable.circle_tan);
	}

	public Drawable getGroupIcon(UserInfo userInfo) {
		if (Main.colorMode == Main.show_by_team) {
			if (userInfo.section == app.mapInfo.myInfo.section)
				return myBM[0];
			else
				return myBM[userInfo.section % iconNum + 1];
		} else {
			if (userInfo.sex == app.mapInfo.myInfo.sex) return myBM[0];
			else return myBM[1];
		}
	}

	/**
	 * Update location layer when new location is received
	 */
	public void UpdateLocationOverlay(LocationData locationData,
			boolean hasAnimation) {
		mLocationOverlay.setData(locationData);
		mMapView.refresh();
		if (hasAnimation) {
			mMapController.animateTo(new GeoPoint(
					(int) (locationData.latitude * 1E6),
					(int) (locationData.longitude * 1E6)));
		}
		checkMarkerTime();
	}

	boolean isInvalidLocation(GeoPoint point) {
		if (point == null) return false;
		if (point.getLatitudeE6() / 1E6 > 180 || point.getLatitudeE6() / 1E6 < -180
				|| point.getLongitudeE6() > 180 || point.getLongitudeE6() / 1E6 < -180)
			return false;
		return true;
	}
	
	/**
	 * Update to draw other users
	 */
	public void UpdateMap(MapInfo mapInfo) {
		if (mapInfo == null) {
			if (mOverlay != null && mOverlay.getAllItem().size() != 0) {
				mOverlay.removeAll();
			}
			return;
		}
		freshManInfo = new Vector<UserInfo>();
		
		// first remove all old users that are not here now
		
		Vector<Integer> delList = new Vector<Integer>();
		
		for (java.util.Map.Entry<Integer, OverlayItem> i : hash.entrySet()) {
			if (mapInfo.getUserInfo(i.getKey()) == null) {
				delList.add(i.getKey());
			}
		}
		for (int i : delList) {
			mOverlay.removeItem(hash.get(i));
			markerToInt.remove(hash.get(i));
		}
		for (int i : delList) {
			hash.remove(i);
		}
		
		mMapView.refresh();
		
		preMapInfo = mapInfo.copy();

		// then update and add items
		for (UserInfo i : mapInfo.getVector()) {
			if (i.uid == app.mapInfo.myInfo.uid)
				continue;
			if (hash.containsKey(i.uid) == false) {
				if (isInvalidLocation(i.location)) {
					
				} else {
					curItem = new OverlayItem(i.location, "USERNAME_HERE",
						"USER_SNIPPET_HERE");
					// TODO getDrawable
					// /////////////////////////////
					curItem.setMarker(getGroupIcon(i));
					mOverlay.addItem(curItem);
					hash.put(i.uid, curItem);
					markerToInt.put(curItem, i.uid);
				}
			} else {
				if (isInvalidLocation(i.location)) {
					mOverlay.removeItem(hash.get(i.uid));
					markerToInt.remove(hash.get(i.uid));
					hash.remove(i.uid);
				} else {
					curItem = hash.get(i.uid);
					curItem.setGeoPoint(i.location);
					mOverlay.updateItem(curItem);
				}
			}
		}
		if (mMapView != null) {
			mMapView.refresh();
		}
		checkMarkerTime();
	}
	
	public void receiveMarker(MarkerInfo markerInfo) {
		if (nowMarker != null && markerInfo.level >= nowMarkerLevel) {
			Log.d("marker", "Old marker replaced by marker with higher level!");
			nowMarker.setGeoPoint(markerInfo.markerPoint);

			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(markerInfo.markerTimestamp * 1000);
			Date date = (Date) cal.getTime();
			nowMarkerHour = date.getHours();
			nowMarkerMinute = date.getMinutes();
			nowMarkerTimestamp = markerInfo.markerTimestamp;
			nowMarkerLevel = markerInfo.level;

			mOverlay.updateItem(nowMarker);
			mMapView.refresh();
			mMapController.animateTo(markerInfo.markerPoint);
			Toast toast = Toast.makeText(context,"收到新路标,集合时间:" + nowMarkerHour + ":" + nowMarkerMinute, 5000);
			toast.setGravity(Gravity.TOP, 0, 80);
			toast.show();
			return;
		}
		if (nowMarker == null) {
			Log.d("marker", "New marker created!");
			nowMarker = new OverlayItem(markerInfo.markerPoint, "MARKER_NAME",
					"");
			nowMarker.setMarker(context.getResources().getDrawable(
					R.drawable.marker_red));
			System.out.println(markerInfo.markerPoint.getLatitudeE6() + " "
					+ markerInfo.markerPoint.getLongitudeE6());
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(markerInfo.markerTimestamp * 1000);
			Date date = (Date) cal.getTime();
			nowMarkerHour = date.getHours();
			nowMarkerMinute = date.getMinutes();
			nowMarkerTimestamp = markerInfo.markerTimestamp;
			nowMarkerLevel = markerInfo.level;

			markerIndex = mOverlay.getAllItem().size();
			mOverlay.addItem(nowMarker);
			mMapView.refresh();
			mMapController.animateTo(markerInfo.markerPoint);
			Toast toast = Toast.makeText(context,"收到新路标,集合时间:" + nowMarkerHour + ":" + nowMarkerMinute, 5000);
			toast.setGravity(Gravity.TOP, 0, 80);
			toast.show();
			return;
		}
	}

	void sendMarker() {
		Log.d("marker", "Marker prepare!   " + nowMarkerTimestamp);
		ReqSetMarker req = new ReqSetMarker(app.token, app.username, nowMarker
				.getPoint().getLatitudeE6() / 1e6, nowMarker.getPoint()
				.getLongitudeE6() / 1e6, (int)nowMarkerTimestamp,
				System.currentTimeMillis(), 3000l);

		Log.d("marker", "Marker sent!   " + req.deadline);
		app.transam.send(req);
	}

	/**
	 * Draw a marker
	 */
	public void DrawMarker(GeoPoint markerPoint) {

		if (nowMarker != null && app.mapInfo.myInfo.level >= nowMarkerLevel) {
			nowMarker.setGeoPoint(markerPoint);
			nowMarkerHour = newMarkerHour;
			nowMarkerMinute = newMarkerMinute;
			nowMarkerTimestamp = newMarkerTimestamp;
			nowMarkerLevel = app.mapInfo.myInfo.level;
			
			sendMarker();
			Log.d("marker", "Sent and replace");
			mOverlay.updateItem(nowMarker);
			mMapView.refresh();
			mMapController.animateTo(markerPoint);
			return;
		} else if (nowMarker == null) {
			nowMarker = new OverlayItem(markerPoint, "MARKER_NAME", "");
			nowMarker.setMarker(context.getResources().getDrawable(
					R.drawable.marker_red));
			nowMarkerHour = newMarkerHour;
			nowMarkerMinute = newMarkerMinute;
			nowMarkerTimestamp = newMarkerTimestamp;
			nowMarkerLevel = app.mapInfo.myInfo.level;

			sendMarker();
			Log.d("marker", "Send and new");
			markerIndex = mOverlay.getAllItem().size();
			Log.d("marker", "my new marker created");
			mOverlay.addItem(nowMarker);
			mMapView.refresh();
			mMapController.animateTo(markerPoint);
		}
		Toast toast = Toast.makeText(context,"创建新路标,集合时间:" + nowMarkerHour + ":" + nowMarkerMinute, 5000);
		toast.setGravity(Gravity.TOP, 0, 80);
		toast.show();
	}

	public GeoPoint getMakerLocation() {
		if (nowMarker == null)
			return null;
		else
			return nowMarker.getPoint();
	}

	public void removeMarker() {
		if (nowMarker == null)
			return;
		mOverlay.removeItem(nowMarker);
		nowMarker = null;
		mMapView.refresh();
	}

	public void checkMarkerTime() {
		if (nowMarker != null && nowMarkerTimestamp <= System.currentTimeMillis() / 1000) {
			AlertMaker lateAlert = new AlertMaker(context, this);
			lateAlert.showLateAlert();
			removeMarker();
		}
	}
	
	/**
	 * Remove all other users
	 */
	public void clearOverlay(View view) {

		if (mOverlay != null && mOverlay.getAllItem().size() != 0) {
			mOverlay.removeAll();
			mMapView.refresh();
		}
	}

	/**
	 * Reset other users over lay
	 */
	public void resetOverlay(View view) {

		clearOverlay(null);
		mOverlay.addItem(mItems);
	}
}
