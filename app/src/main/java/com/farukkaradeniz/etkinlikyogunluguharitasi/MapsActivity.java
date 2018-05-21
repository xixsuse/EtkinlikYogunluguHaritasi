package com.farukkaradeniz.etkinlikyogunluguharitasi;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private UiSettings uiSettings;
    private FloatingActionButton showEvent, makeRoute, clearMap;
    private FloatingActionsMenu floatMenu;
    private Polyline route;
    private AlertDialog dialog;
    private View dialogView;
    private Button tamam, baskaTarih;
    private Spinner dates, cities, categories;
    private TextView secilenTarih;
    private DatePickerDialog dateDialog;
    private Calendar cal;
    private SimpleDateFormat sdf;
    private String formatDate, format2Date;
    private FirebaseFirestore firestore;
    private CollectionReference eventRef, placeRef;
    private ArrayList<Event> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        init();
        listeners();

    }

    private void init() {
        events = new ArrayList<>();
        showEvent = findViewById(R.id.action_event);
        makeRoute = findViewById(R.id.action_route);
        clearMap = findViewById(R.id.action_clear);
        floatMenu = findViewById(R.id.float_menu);
        dialogView = getLayoutInflater().inflate(R.layout.event_filter_dialog, null);
        dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Show events")
                .create();
        tamam = dialogView.findViewById(R.id.tamam_button);
        baskaTarih = dialogView.findViewById(R.id.pick_date);
        dates = dialogView.findViewById(R.id.spinner_date);
        dates.setPrompt("Lütfen Tarih Seçiniz");
        cities = dialogView.findViewById(R.id.spinner_cities);
        cities.setPrompt("Lütfen Şehir Seçiniz");
        categories = dialogView.findViewById(R.id.spinner_categories);
        categories.setPrompt("Lütfen Kategori Seçiniz");
        secilenTarih = dialogView.findViewById(R.id.secilen_tarih);
        cal = Calendar.getInstance();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDate = sdf.format(cal.getTime());
        format2Date = "";
        dateDialog = new DatePickerDialog(this, (datePicker, i, i1, i2) -> {
            Log.i(TAG, "init: " + i + "-" + i1 + "-" + i2);
            cal.set(dateDialog.getDatePicker().getYear(),
                    dateDialog.getDatePicker().getMonth(),
                    dateDialog.getDatePicker().getDayOfMonth(),
                    0, 0, 0);
            Date date = cal.getTime();
            formatDate = sdf.format(date);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            format2Date = sdf.format(cal.getTime());
            secilenTarih.setText("Secilen Tarihler: \n" + formatDate + "\n" + format2Date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        firestore = FirebaseFirestore.getInstance();
        eventRef = firestore.collection("events");
        placeRef = firestore.collection("places");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void listeners() {
        makeRoute.setOnClickListener(view -> {
            startActivityForResult(new Intent(this, MakeRouteActivity.class), 11);
        });
        showEvent.setOnClickListener(view -> {
            Toast.makeText(this, "Show Event", Toast.LENGTH_SHORT).show();
            dialog.show();
        });
        clearMap.setOnClickListener(view -> {
            Toast.makeText(this, "Clear the Map", Toast.LENGTH_SHORT).show();
        });
        baskaTarih.setOnClickListener(view -> {
            dateDialog.show();
        });
        dates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String date = (String) adapterView.getItemAtPosition(i);
                Log.i(TAG, "onItemSelected: " + date);
                baskaTarih.setVisibility(date.equals("Başka bir tarih") ? View.VISIBLE : View.GONE);
                if (date.equals("Yarın")) {
                    cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);

                    formatDate = sdf.format(cal.getTime());
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    format2Date = sdf.format(cal.getTime());
                    secilenTarih.setText("Secilen Tarihler: \n" + formatDate + "\n" + format2Date);
                } else if (date.equals("Bu Haftasonu")) {
                    cal = Calendar.getInstance();
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                        haftasonuHesapla();
                    } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Date pazar = cal.getTime();
                        formatDate = sdf.format(pazar);
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        format2Date = sdf.format(cal.getTime());
                        secilenTarih.setText("Secilen Tarihler: \n" + formatDate + "\n" + format2Date);
                    } else {
                        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                        }
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        haftasonuHesapla();
                    }
                } else if (date.equals("Bugün")) {
                    cal = Calendar.getInstance();
                    formatDate = sdf.format(cal.getTime());
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    format2Date = sdf.format(cal.getTime());
                    secilenTarih.setText("Secilen Tarihler: \n" + formatDate + "\n" + format2Date);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        tamam.setOnClickListener(view -> {
            dialog.hide();
            floatMenu.collapse();
            Toast.makeText(this, "Yükleniyor..", Toast.LENGTH_SHORT).show();
            String city = (String) cities.getSelectedItem();
            String category = (String) categories.getSelectedItem();

            Query query = eventRef.whereGreaterThanOrEqualTo("date", formatDate)
                    .whereLessThanOrEqualTo("date", format2Date);
            if (!category.equals("Hepsi")) {
                query = query.whereEqualTo("category.category", category);
            }

            query.get().addOnCompleteListener(task1 -> {
                if (task1.isSuccessful() && task1.isComplete()) {
                    List<DocumentSnapshot> documents1 = task1.getResult().getDocuments();
                    for (DocumentSnapshot documentSnapshot : documents1) {
                        Event event = documentSnapshot.toObject(Event.class);
                        Log.i(TAG, "listeners: event" + event.toString());
                        placeRef.whereEqualTo("city", city)
                                .whereEqualTo("name", event.getPlaceName())
                                .get()
                                .addOnCompleteListener(task -> {
                                    Log.i(TAG, "listeners: task");
                                    if (task.isSuccessful() && task.isComplete()) {
                                        Log.i(TAG, "listeners: here");
                                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                                        for (DocumentSnapshot document : documents) {
                                            Place place = document.toObject(Place.class);
                                            event.setPlace(place);
                                            // todo haritada eklenme kısmı
                                        }
                                    }
                                });
                        events.add(event);
                        Log.i(TAG, "listeners: event:" + event.toString());
                    }
                } else {
                    Toast.makeText(this, "Error getting the data from the database", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.052, 29.001), 12f));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maps_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.traffic) {
            Toast.makeText(this, "Traffic", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: ");
        if (requestCode == 11 && resultCode == RESULT_OK) {
            floatMenu.collapse();
            ArrayList<LatLng> points = data.getParcelableArrayListExtra("points");
            createRoute(points);
            Log.i(TAG, "onActivityResult: Route points size: " + points.size());
        }
    }

    private void createRoute(ArrayList<LatLng> points) {
        if (route != null && route.isVisible()) {
            route.remove();
        }
        PolylineOptions options = new PolylineOptions();
        options.addAll(points);
        options.width(10);
        options.color(Color.BLUE);
        route = mMap.addPolyline(options);
    }

    private void haftasonuHesapla() {
        Date ctesi = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date pazar = cal.getTime();
        formatDate = sdf.format(ctesi);
        format2Date = sdf.format(pazar);
        secilenTarih.setText("Secilen Tarihler: \n" + formatDate + "\n" + format2Date);
    }
}
