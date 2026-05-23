package localisation.emsi.ma.map;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import localisation.emsi.ma.map.databinding.ActivityMapsBinding;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 1) LocationManager permet d'écouter la localisation
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 2) Marker initial (Sydney)
        LatLng sydney = new LatLng(-34.0, 151.0);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        Toast.makeText(getApplicationContext(), "Map Ready", Toast.LENGTH_SHORT).show();

        // 3) Vérifier permission runtime
        boolean permissionGranted =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {
            startLocationUpdates(locationManager);
        } else {
            // 8) Si pas de permission : la demander
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200
            );
        }

        // 9) Mise en place caméra initiale (optionnel)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates(LocationManager locationManager) {
        if (locationManager == null) return;

        // 4) Demander des mises à jour de position
        // NETWORK_PROVIDER: utilise Wi-Fi/4G (souvent disponible en intérieur)
        // On met minDistance à 0 pour faciliter les tests (mis à jour à chaque mouvement détecté)
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,   // minTime = 1 seconde
                0,      // minDistance = 0 mètres pour tester facilement
                new LocationListener() {

                    @Override
                    public void onLocationChanged(Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // 5) Afficher un toast (debug)
                        Toast.makeText(getApplicationContext(),
                                latitude + " " + longitude,
                                Toast.LENGTH_SHORT).show();

                        // 6) Ajouter ou déplacer l'unique marqueur pour la nouvelle position
                        LatLng position = new LatLng(latitude, longitude);
                        if (currentMarker == null) {
                            currentMarker = mMap.addMarker(new MarkerOptions().position(position).title("Position actuelle"));
                        } else {
                            currentMarker.setPosition(position);
                        }

                        // 7) Zoomer et centrer sur cette position avec animation fluide
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        // Optionnel (déprécié sur certaines versions d'Android)
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        // Provider activé (réseau/GPS)
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        // Si le provider est désactivé, proposer d'activer le GPS
                        buildAlertMessageNoGps();
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée", Toast.LENGTH_SHORT).show();
                // Re-déclencher la logique (relancer map)
                if (mMap != null) {
                    onMapReady(mMap);
                }
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_LONG).show();
            }
        }
    }
}
