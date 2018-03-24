package jp.co.mob.nishimura.gpsdistance;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

/**
 * 受け取った情報から現在地のLocationを返す
 * Created by nishi on 2018/03/24.
 */

public class CurrentLocation {
    public Location location;
    public CurrentLocation(LocationManager locationManager, LocationListener locationListener,Context context) {
        Criteria criteria = new Criteria();
        location = null;
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //現在地の取得
            locationManager.requestLocationUpdates(provider, 0, 0,  locationListener);
            //現在地の経度・緯度の取得
            location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        }
    }
}
