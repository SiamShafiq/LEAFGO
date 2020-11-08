package com.example.leaf;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {


    private GoogleMap map;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;

    private static final int M_MAX_ENTRIES = 20;

    private static String API_KEY = "AIzaSyD9SaBFDamjQ41ZHtP7MvCGk2qPaT-i2dE";
    private static final String TAG = MapsActivity.class.getSimpleName();

    PlaceDetails nearbyPlaces;
    private PlacesClient placesClient;


    private static final String URL_FORMAT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
            "location=%s,%s&radius=%s&type=restaurant&key=" + API_KEY;

    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Places.initialize(getApplicationContext(), API_KEY);
        placesClient = Places.createClient(this);

        Button goButton = (Button) findViewById(R.id.buttonMaps);
        goButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateLocationUI();
                getDeviceLocation();
            }
        });

    }



    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
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
        map = googleMap;

        updateLocationUI();
        getDeviceLocation();

//        map.setOnPoiClickListener(this);
    }

    public void placeMarker(Location lastlocal){
        LatLng local = new LatLng(lastlocal.getLatitude(), lastlocal.getLongitude());
        Marker vancouver = map.addMarker(
                new MarkerOptions()
                        .position(local)
                        .title("Vancouver")
                        .snippet("Population: 4,137,400"));
    }

    public void getPlaceDetails(String placeID){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://maps.googleapis.com/maps/api/place/details/json?placeid=" +
                placeID + "&key=AIzaSyD9SaBFDamjQ41ZHtP7MvCGk2qPaT-i2dE";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        PlaceDetails placeDetails = GSON.fromJson(response, PlaceDetails.class);
                        makeToast("1");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast("That didn't work!");
            }
        });
        queue.add(stringRequest);
    }

    public void markCurrentPlace(){
        if (map == null) {
            return;
        }

        if (locationPermissionGranted) {
            // Use fields to define the data types to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                    Place.Field.LAT_LNG, Place.Field.ID);

            // Use the builder to create a FindCurrentPlaceRequest.
            FindCurrentPlaceRequest request =
                    FindCurrentPlaceRequest.newInstance(placeFields);

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final
            Task<FindCurrentPlaceResponse> placeResult =
                    placesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener (new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.
                        int count;
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        boolean visited = false;
                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            visited = nearbyPlaces.setVisited(placeLikelihood.getPlace().getId());

                            i++;
                            if (i > (count - 1) || visited) {
                                break;
                            }
                        }
                    }
                    else {
                        Log.e(TAG, "Exception: %s", task.getException());
                    }
                }
            });
        }
    }


    public void RestaurantLoader(String url){
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loadNearbyRestaurants(lastKnownLocation, response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast("That didn't work!");
            }
        });
        queue.add(stringRequest);
    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getDeviceLocation() {

        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));

//                                placeMarker(lastKnownLocation);

                                String url = String.format(URL_FORMAT, lastKnownLocation.getLatitude(),
                                lastKnownLocation.getLongitude(), 1000);
                                RestaurantLoader(url);

                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void loadNearbyRestaurants(Location lastKnownLocation, String placeDetails) {
        try {
            if(nearbyPlaces == null){
                nearbyPlaces = GSON.fromJson(placeDetails, PlaceDetails.class);
                Log.i(placeDetails, placeDetails);
            }

        } catch (Exception e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }

        markCurrentPlace();
        map.clear();
        for (PlaceDetails.Result place: nearbyPlaces.results) {

            displayPlace(place);
        }
    }

    private void displayPlace(PlaceDetails.Result place) {
        String name = place.name;
        String rating = Double.toString(place.rating);
        double lat = place.geometry.location.lat;
        double lng = place.geometry.location.lng;

        BitmapDescriptor visitedMarker = place.visited ? BitmapDescriptorFactory.defaultMarker()
                : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(name)
                .snippet( "Rating: " + rating)
                .icon(visitedMarker);

        map.addMarker(markerOptions);
    }

    public void makeToast(String whatToSay){
        Toast.makeText(this, whatToSay,
                Toast.LENGTH_SHORT).show();
    }

    public void makeToast(double whatToSay){
        String result = Double.toString(whatToSay);
        Toast.makeText(this, result,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPoiClick(PointOfInterest poi) {
//        String id = poi.placeId;
//        getPlaceDetails(id);

        /*
        Toast.makeText(this, "Clicked: " +
                        poi.name + "\nPlace ID:" + poi.placeId +
                        "\nLatitude:" + poi.latLng.latitude +
                        " Longitude:" + poi.latLng.longitude,
                Toast.LENGTH_SHORT).show();
                */
    }
}