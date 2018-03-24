package jp.co.mob.nishimura.gpsdistance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.google.android.gms.ads.MobileAds;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        LocationListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private final int REQUEST_PERMISSION = 1000;
    private LatLng mCurrentLng;
    private CurrentLocation mCommonMethod;
    private Marker mMarker;
    private Polyline mPolyline;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private int mCount = 0;

    /*
    * 起動直後の処理
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // OnCreate でロケーションマネージャを取得
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        // AdModの初期化（AP）
        MobileAds.initialize(this, getString(R.string.app_id));



        // AdModの初期化(インタースティシャル)
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // パーミッションのチェック
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        } else {
            locationActivity();
        }

        //現在地ボタン、ズームイン/アウトボタンを有効化
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //リスナーを登録
        mMap.setOnMyLocationButtonClickListener(this);//現在地ボタンタップ
        mMap.setOnMapClickListener(this);//マップタップ

        //航空写真＋地図
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //現在地に移動する（初期表示）
        //OnResumeだと最初の起動でNULLで落ちる
        CurrentLocation cl = new CurrentLocation(locationManager, this, this);
        mCurrentLng = new LatLng(cl.location.getLatitude(), cl.location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLng, 18));

        // AdModの初期化(バナー)
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    /*
    * タップした時の処理
    * 現在地との距離を表示する
    * */
    @Override
    public void onMapClick(LatLng latLng) {

        // AdModの初期化(インタースティシャル)　2回に1回
        if (mCount == 0) {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.");
            }
            mCount++;
        } else if (mCount > 3) {
            mCount = 0;
        }


        //以前のものを削除
        if (mMarker != null) {
            mMarker.remove();
        }
        if (mPolyline != null) {
            mPolyline.remove();
        }

        //マーカー表示
        MarkerOptions options = new MarkerOptions();
        options.position(latLng);
        mMarker = mMap.addMarker(options);

        //現在地のロケーションを再取得
        CurrentLocation cl = new CurrentLocation(locationManager, this, this);
        mCurrentLng = new LatLng(cl.location.getLatitude(), cl.location.getLongitude());

        //現在地からマーカーへ直線を引く
        PolylineOptions straight = new PolylineOptions().add(latLng, mCurrentLng).geodesic(false).color(Color.BLUE).width(3);
        mPolyline = mMap.addPolyline(straight);

        //タップした位置と現在地の距離を測定（メートル）
        double distance ;
        double distanceYard = 0.0;
        //Google Maps Android API Utility Library を使用
        distance = SphericalUtil.computeDistanceBetween(latLng, mCurrentLng);
        if (distance > 0) {
            distanceYard = distance / 0.9144;

        }
        //少数点以下は切り捨てて表示
        Toast.makeText(getApplicationContext(), getString(R.string.distance_m, (int) distanceYard, (int) distance), Toast.LENGTH_LONG).show();
    }

    /*
    * 画面復帰で現在地取得・移動
    * */
    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onMyLocationButtonClick() {

        return false;
    }

    /*
    * GPS設定のチェックと現在地取得
    * */
    private void checkPermission() {
        // 許可されている場合
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationActivity();
        }
        // 拒否していた場合
        else {
            requestPermission();
        }
    }

    /*
    * GPS設定の許可を求める
    * */
    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION);

        } else {
            //GPS設定を許可
            Toast toast = Toast.makeText(this, getString(R.string.gpsenable), Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,},
                    REQUEST_PERMISSION);

        }
    }

    /*
* GPS設定の許可を求めた結果の取得
* */
    @Override
    public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (reqCode == REQUEST_PERMISSION) {
            //
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationActivity();

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this, getString(R.string.not_start_app), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    // Intent でLocation
    private void locationActivity() {
//        Intent intent = new Intent(getApplication(), LocationActivity.class);
//        startActivity(intent);
    }


    //現在地の更新依頼を検知し、反映する
    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
