package tw.brad.apps.maskactivity;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LocationManager lmgr;
    private GoogleMap mMap;
    private MyDBOpenHelper openHelper;
    private SQLiteDatabase database;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        queue = Volley.newRequestQueue(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            init();
        }else{
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    8);
        }



    }
    private MyListener myListener;
    private void init(){
        new Thread(){
            @Override
            public void run() {
                fetchOpendata();
            }
        }.start();

        openHelper = new MyDBOpenHelper(this, "mask", null, 1);
        database = openHelper.getReadableDatabase();

        lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myListener = new MyListener();
        lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, myListener);
    }

    @Override
    public void finish() {
        if (lmgr != null){
            lmgr.removeUpdates(myListener);
        }
        super.finish();
    }

    private double nowLat, nowLng;

    private class MyListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            nowLat = location.getLatitude();
            nowLng = location.getLongitude();
            moveCamera();
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                init();
            }else{
                finish();
            }
        }


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        moveCamera();
    }

    private void moveCamera(){
        if (mMap != null) {
            LatLng here = new LatLng(nowLat, nowLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        }
    }

    private static final int MID = 0;
    private static final int NAME = 1;
    private static final int ADDRESS = 2;
    private static final int TEL = 3;
    private static final int ADULT = 4;
    private static final int CHILD = 5;
    private LinkedList<HashMap<String,String>> data = new LinkedList<>();

    private void fetchOpendata(){
        data.clear();
        // https://data.nhi.gov.tw/resource/mask/maskdata.csv
        try {
            URL url = new URL("https://data.nhi.gov.tw/resource/mask/maskdata.csv");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream in = conn.getInputStream();
            InputStreamReader ireader = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(ireader);
            reader.readLine(); String line = null;
            while ( (line = reader.readLine()) != null){
                String[] fields = line.split(",");
                Log.v("bradlog", fields[NAME] +":" + fields[ADDRESS] +":" +
                        fields[ADULT] + ":" + fields[CHILD]);
                HashMap<String,String> row = new HashMap<>();
                row.put("mid", fields[MID]);
                row.put("name", fields[NAME]);
                row.put("address", fields[ADDRESS]);
                row.put("tel", fields[TEL]);
                row.put("adult", fields[ADULT]);
                row.put("child", fields[CHILD]);
                data.add(row);
            }
            reader.close();
            navData();
        }catch (Exception e){
            Log.v("bradlog", e.toString());
        }

    }

    private void navData(){
        for (int i=0; i<data.size(); i++){
            HashMap<String,String> point = data.get(i);
            final int index = i;
            final String mid = point.get("mid");
            String address = point.get("address");

            if (!address.contains("臺中市")) continue;

            // 先檢查資料庫有沒有該筆資料
            // SELECT * FROM mask WHERE mid = 'xxxx'
            Cursor cursor = database.query("mask", null,
                    "mid = ?", new String[]{mid},
                    null, null, null);
            if (cursor.getCount()>0){
                //
                Log.v("bradlog", "old data");
                cursor.moveToNext();
                double lat = cursor.getDouble(cursor.getColumnIndex("lat"));
                double lng = cursor.getDouble(cursor.getColumnIndex("lng"));
                addMarker(lat,lng);
            }else{
                //
                StringRequest request = new StringRequest(
                        Request.Method.GET,
                        "https://maps.googleapis.com/maps/api/geocode/json?address=" + address + "&key=AIzaSyA4-fi8GzN9qJYGa7WJKlYPpzbgYeg-Zbg",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseAddress(index, mid, response);
                            }
                        },
                        null
                );
                queue.add(request);
            }
        }
    }

    private void parseAddress(int index, String mid, String json){
        Log.v("bradlog", "new data");
        try {
            JSONObject root = new JSONObject(json);
            String status = root.getString("status");
            if (status.equals("OK")){
                JSONArray results = root.getJSONArray("results");
                JSONObject row = results.getJSONObject(0);
                JSONObject geometry = row.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                Double lat = location.getDouble("lat");
                Double lng = location.getDouble("lng");

                // INSERT INTO mask (mid,lat,lng) VALUES (mid,lat,lng)
                ContentValues values = new ContentValues();
                values.put("mid", mid);
                values.put("lat", lat);
                values.put("lng", lng);
                database.insert("mask", null, values);

                addMarker(lat, lng);
            }
        }catch (Exception e){
            Log.v("bradlog", e.toString());
        }
    }

    private void addMarker(final double lat, final double lng){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLng latLng = new LatLng(lat, lng);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng));
            }
        });
    }


}