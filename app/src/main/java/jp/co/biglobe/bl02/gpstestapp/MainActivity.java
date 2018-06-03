package jp.co.biglobe.bl02.gpstestapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    final static String TAG = "GPSTEST";

    private LocationManager mLocationManager;
    private Location GPSLocation = null;
    private Location NetworkLocation = null;
    private LocationListener mGPSLocationListener = null;
    private GpsStatus.Listener mGPSStatusListener = null;
    private LocationListener mNetworkLocationListener = null;

    String APIkey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide navigation bar
        //this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        }
        else{
            GPSLocationStart();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,}, 1000);
        }
        else{
            NetworkLocationStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLocationManager.removeGpsStatusListener(mGPSStatusListener);
        mLocationManager.removeUpdates(mGPSLocationListener);
        mLocationManager.removeUpdates(mNetworkLocationListener);
    }

    private void GPSLocationStart(){
        Log.d(TAG,"GPSlocationStart()");

        // LocationManager インスタンス生成
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        if (mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "GPS location manager Enabled");
        } else {
            // GPSを設定するように促す
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            Log.d(TAG, "not gpsEnable, startActivity");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            Log.d(TAG, "GPS checkSelfPermission false");
            return;
        }

        mGPSLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String p = location.getProvider();
                if ( ! p.equals(LocationManager.GPS_PROVIDER)){
                    Toast toast = Toast.makeText(MainActivity.this,
                            "Illegal provider=" + p, Toast.LENGTH_LONG);
                    Log.e(TAG, "GPS_PROVIDER listner return=" + p);
                }
                // 緯度経度
                String str = "GPS Lt:"
                        + String.format("%.8f", location.getLatitude())
                        + ",Ln:" + String.format("%.8f" ,location.getLongitude())
                        + "," + String.format("%.0f", location.getAccuracy()) + "m";

                // GNSS数
                Bundle extra = location.getExtras();
                if( extra != null ) {
                    int satellites = extra.getInt("satellites");
                    str += ",n:" + String.valueOf(satellites);
                } else {
                    str += ",n:null";
                }
                TextView textView = (TextView) findViewById(R.id.textView1);
                textView.setText(str);
                Log.d(TAG, str);

                GPSLocation = location;

                showWebView();
            }

            @Override
            public void onStatusChanged(String s, int status, Bundle bundle) {
                Toast toast;
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "GPS_PROVIDER LocationProvider.AVAILABLE");
                        toast = Toast.makeText(MainActivity.this,
                                "GPS_PROVIDER AVAILABLE", Toast.LENGTH_LONG);
                        break;
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.d(TAG, "GPS_PROVIDER LocationProvider.OUT_OF_SERVICE");
                        toast = Toast.makeText(MainActivity.this,
                                "GPS_Provider OUT_OF_SERVICE", Toast.LENGTH_LONG);
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "GPS_PROVIDER LocationProvider.TEMPORARILY_UNAVAILABLE");
                        toast = Toast.makeText(MainActivity.this,
                                "GPS_Provider TEMPORARILY_UNAVAILABLE", Toast.LENGTH_LONG);
                        break;
                }
            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        mGPSStatusListener = new GpsStatus.Listener()
        {
            public void onGpsStatusChanged( int event ) {
                // http://developer.android.com/reference/android/location/GpsSatellite.html
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
                    Log.d(TAG, "GPS checkSelfPermission false");
                    return;
                }

                Iterable<GpsSatellite> satellites = mLocationManager.getGpsStatus(null).getSatellites();
                int sat_used = 0;
                int sat_total = 0; // GpsStaus.getMaxSatellites() の返り値が謎
                String str = "PRN : Azimuth : Elevation : S/N\n";
                for( GpsSatellite sat : satellites ) {
                    if (sat.getSnr() != 0) {
                        if (sat.usedInFix()) {
                            str += "*";
                            sat_total++;
                            sat_used++;
                        } else {
                            str += " ";
                            sat_total++;
                        }
                        str += String.format("%2d", (int) sat.getPrn()) + " : ";
                        str += String.format("%7d", (int) sat.getAzimuth()) + " : ";
                        str += String.format("%9d", (int) sat.getElevation()) + " : ";
                        str += String.format("%4.1f", sat.getSnr()) + "\n";
                    }
                }
                str += String.format( "satellites %d/%d\n" , sat_used, sat_total );
                Log.d(TAG, str);
            }
        };

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 1, mGPSLocationListener);
        Log.d(TAG, "GPS_PROVIDER start");
        Log.d(TAG, "LoactionUpdates time=300ms, distance=1m");
        mLocationManager.addGpsStatusListener( mGPSStatusListener );
    }

    private void NetworkLocationStart(){
        Log.d(TAG,"NetworkLocationStart()");

        // LocationManager インスタンス生成
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        if (mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.d(TAG, "Network location manager Enabled");
        } else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            Log.d(TAG, "not network provider Enable, startActivity");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,}, 1000);
            Log.d(TAG, "Network checkSelfPermission false");
            return;
        }

        mNetworkLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 緯度経度の表示
                String str = "Net Lt:"
                        + String.format("%.8f", location.getLatitude())
                        + ",Ln:" + String.format("%.8f" ,location.getLongitude())
                        + "," + String.format("%.0f", location.getAccuracy()) + "m";

                TextView textView = (TextView) findViewById(R.id.textView2);
                textView.setText(str);
                NetworkLocation = location;
                Log.d(TAG, str);

                showWebView();
            }

            @Override
            public void onStatusChanged(String s, int status, Bundle bundle) {
                Toast toast;
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "NETWORK_PROVIDER LocationProvider.AVAILABLE");
                        toast = Toast.makeText(MainActivity.this,
                                "NETWORK_PROVIDER AVAILABLE", Toast.LENGTH_LONG);
                        break;
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.d(TAG, "NETWORK_PROVIDER LocationProvider.OUT_OF_SERVICE");
                        toast = Toast.makeText(MainActivity.this,
                                "NETWORK_Provider OUT_OF_SERVICE", Toast.LENGTH_LONG);
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "NETWORK_PROVIDER LocationProvider.TEMPORARILY_UNAVAILABLE");
                        toast = Toast.makeText(MainActivity.this,
                                "NETWORK_Provider TEMPORARILY_UNAVAILABLE", Toast.LENGTH_LONG);
                        break;
                }
            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        Log.d(TAG, "Network_PROVIDER start");
        Log.d(TAG, "LoactionUpdates time=300ms, distance=1m");
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
                , 300, 1, mNetworkLocationListener);
    }

    private void showWebView(){
        // 地図表示
        WebView webview = (WebView)findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        String marker1 = null, marker2 = null;

        if (GPSLocation != null) {
            try {
                marker1 = "markers=" + URLEncoder.encode("size:mid|color:blue|label:G|"
                        + GPSLocation.getLatitude() + "," + GPSLocation.getLongitude(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (NetworkLocation != null) {
            try {
                marker2 = "markers=" + URLEncoder.encode("size:mid|color:yellow|label:N|"
                        + NetworkLocation.getLatitude() + "," + NetworkLocation.getLongitude(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        int x = 300;
        int y = 290;
        if (x > 640)
            x = 640;
        if (y > 640)
            y = 640;

        String url = "https://maps.googleapis.com/maps/api/staticmap?key=" + APIkey + "&size=" + x + "x" + y;
        if (marker1 != null)
            url = url + "&" + marker1;
        if (marker2 != null)
            url = url + "&" + marker2;
        url = url + "&zoom=19";
//        Log.d(TAG, "URL=" + url);
        webview.loadUrl(url);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[]permissions, int[] grantResults) {
        if (requestCode == 1000) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"checkSelfPermission true");

                if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION))
                    GPSLocationStart();

                if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION))
                    NetworkLocationStart();

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this,
                        "これ以上なにもできません", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

}
