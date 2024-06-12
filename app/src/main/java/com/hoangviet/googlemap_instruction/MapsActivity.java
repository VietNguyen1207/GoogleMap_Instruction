package com.hoangviet.googlemap_instruction;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.hoangviet.googlemap_instruction.databinding.ActivityMapsBinding;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;


    // We define a location_permission_code and set it to 100
    public static int LOCATION_PERMISSION_CODE = 100;

    // get userLocation we need FusedLocationProviderClient interface
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat, currentLong;

    // define SearchView select the SearchView from appcompat.widget
    SearchView search_bar;

    // define marker for destination that user search for
    Marker destinationMarker;

    // Define btnCalculate (when click will show the distance between the current location with the search destination)
    Button btnCalculate;
    // txtDistance will responsible for showing the distance between the current location with the search destination (in meters)
    TextView txtDistance;

    LatLng userLocation;

    LatLng getDestination;

    LatLng destinationLatLng;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Button calculate
        btnCalculate = findViewById(R.id.btnCalculation);
        btnCalculate.setOnClickListener(view -> calculateDistanceToDestination());

        txtDistance = findViewById(R.id.txtDistance);

        // get userLocation
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getUserPermission(); // get userPermission -> if allow -> show map and current location with marker.

        // matched defined search_bar with search_bar id.
        search_bar = findViewById(R.id.search_bar);
        search_bar.clearFocus();
        // setOnQueryTextListener for our search_bar (search_bar has been defined above)
        search_bar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String getLocationInput = search_bar.getQuery().toString();
                // query and then toString the input of the location that the user want to search for.
                if (getLocationInput == null) {
                    Toast.makeText(MapsActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                } else  {
                    // use Geocoder from android.location
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    // we will use try catch
                    try {
                        // initialize an addressList from Address provided by android.location
                        List<Address> addressList = geocoder.getFromLocationName(getLocationInput, 1);

                        if (addressList.size() > 0) {
                             getDestination = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());

                            // check if there are any markers that have been placed and remove the old marker to focus on the new marker
                            if (destinationMarker != null) {
                                destinationMarker.remove();
                            }

                            // put a red marker on the destination that the user searched and zoom into it using addMarker to add a marker / MarkerOptions to customize the marker /.
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(getDestination).title(getLocationInput));
                            MarkerOptions markerOptions = new MarkerOptions().position(getDestination).title(getLocationInput);
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(getDestination, 10);
                            mMap.animateCamera(cameraUpdate);
                            destinationMarker = mMap.addMarker(markerOptions);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


    }

    private void calculateDistanceToDestination() {
        // destination is the same input from user when they enter in the search_bar.
        String destination = search_bar.getQuery().toString();

        // destinationLatLng has been defined above
        // then we pass the "destination" to the getDestinationAddress below to do the calculation
        destinationLatLng = getDestinationAddress(destination);
        if (destinationLatLng != null) {
            // define distance as float type = and then calculate is using the calculateDistance method with ( userLocation as our current location and getDestination as our searched location/destination)
            float distance = calculateDistance(userLocation, getDestination);
            txtDistance.setText(" " + distance + " meters");
        } else {
            Toast.makeText(MapsActivity.this, " Cannot define address", Toast.LENGTH_SHORT).show();
        }
    }

    // Pass searched destination to calculate distance between current location
    private LatLng getDestinationAddress(String destAddress) {
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocationName(destAddress, 5);
            if (addressList == null ) {
                return null;
            }
            Address location = addressList.get(0);
            return new LatLng(location.getLatitude(), location.getLongitude());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // This method is responsible for getting the latitude and longitude of the startLocation (our current location) and endLocation (our searched location/ destination)
    private float calculateDistance(LatLng startLatLng, LatLng endLatLng) {
        Location startLocation = new Location("");
        startLocation.setLatitude(startLatLng.latitude);
        startLocation.setLongitude((startLatLng.longitude));

        Location endLocation = new Location("");
        endLocation.setLatitude(endLatLng.latitude);
        endLocation.setLongitude(endLatLng.longitude);
        return startLocation.distanceTo(endLocation) ;
    }

    // Get user permission for accessing their current location.
    private void getUserPermission() {
        // if the
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // return location getUserLocation() if the permissionResult is granted.
            getUserLocation();
        } else {
            // this line of code will request user if user allow us to access to their location and pass location_permission_code into it.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    // this method will help us get the result if the users allow us to get their location.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location Permission has been denied, please allow permission to access location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // this method will get the user location on the map and put the marker on it
    private void getUserLocation() {

        // this is permission check prompt by Android Studio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        //Task from android.gms.tasks
        //Location from android.location
        // After this you have to add a permission check for error to disappear -> permission check will appear above
        Task<Location> task = fusedLocationProviderClient.getLastLocation();

        // addOnSuccessListener for task
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                //define double  currentLat, currentLong above.
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();

                // get location on map based on latitude and longitude
                 userLocation = new LatLng(currentLat, currentLong);

                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, 10);
                mMap.animateCamera(cameraUpdate);
                // 3 lines of code below are used to custom the markers placed on the user current location
                MarkerOptions markerOptions = new MarkerOptions().position(userLocation).title("Your current location");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                mMap.addMarker(markerOptions);


            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // this line of code will enable a button to help you navigate back to your current location ( button will appear on the top right of the screen )
        googleMap.setMyLocationEnabled(true);

        // this line of code will enable button to zoom in and zoom out appear on the bottom right of the map.
        googleMap.getUiSettings().setZoomControlsEnabled(true);



    }
}