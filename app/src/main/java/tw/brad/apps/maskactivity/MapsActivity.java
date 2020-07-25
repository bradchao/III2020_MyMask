package tw.brad.apps.maskactivity;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LocationManager lmgr;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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

}