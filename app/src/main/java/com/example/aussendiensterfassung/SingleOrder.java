package com.example.aussendiensterfassung;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SingleOrder extends AppCompatActivity {

    protected LinearLayout layoutOverview;
    protected LinearLayout layoutContacts;
    protected BottomNavigationView navView;
    protected String objectAdress;
    protected String orderObjectId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    layoutContacts.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_navi:
                    Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(objectAdress));
                    startActivity(mapsIntent);
                    return true;
                case R.id.navigation_contacts:
                    layoutOverview.setVisibility(View.GONE);
                    layoutContacts.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_edit:
                    Intent switchToEditOrder = new Intent(getApplicationContext(), EditOrder.class);
                    switchToEditOrder.putExtra("orderObjectId", orderObjectId);
                    startActivity(switchToEditOrder);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_order);

        // Get intent
        Intent intent = getIntent();
        orderObjectId = intent.getStringExtra("orderObjectId");

        // Define views and layouts
        layoutOverview = findViewById(R.id.single_order_layout_overview);
        layoutContacts = findViewById(R.id.single_order_layout_contacts);
        navView = findViewById(R.id.single_order_navigation);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        getOrder();
        getArticles();
    }

    // Get all pre-set articles from the order
    protected void getArticles() {
        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Einzelauftrag");
        innerQuery.fromLocalDatastore();
        innerQuery.whereEqualTo("objectId", orderObjectId);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ArtikelAuftrag");
        query.fromLocalDatastore();
        query.whereMatchesQuery("Auftrag", innerQuery);
        query.whereEqualTo("Vorgegeben", true);
        query.include("Artikel");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define position table
                    TableLayout tablePositions = findViewById(R.id.single_order_overview_table_material);

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject materialPosition = resultList.get(i);

                        // Get field contents out of the database
                        int pos = i + 1;
                        double quantity = materialPosition.getDouble("Anzahl");
                        String unit = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Einheit");
                        String name = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Name");

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setContentDescription("dynamicRow");

                        // Print position
                        TextView textPosition = new TextView(getApplicationContext());
                        textPosition.setText(String.valueOf(pos));
                        textPosition.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textPosition);

                        // Print quantity
                        TextView textQuantity = new TextView(getApplicationContext());
                        DecimalFormat f = new DecimalFormat("0.00");
                        textQuantity.setText(f.format(quantity));
                        textQuantity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textQuantity);

                        // Print unit
                        TextView textUnit = new TextView(getApplicationContext());
                        textUnit.setText(unit);
                        textUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textUnit);

                        // Print name
                        TextView textName = new TextView(getApplicationContext());
                        textName.setText(name);
                        textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textName);

                        // Attach table row to table
                        tablePositions.addView(row);
                    }
                } else {
                    Log.d("Single Order Articles", "Error: " + e.getMessage());
                }
            }
        });
    }

    protected void getOrder() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", orderObjectId);
        query.include("Aufzug");
        query.include("Aufzug.Kunde");
        query.include("Gesamtauftrag");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    ParseObject singleOrder = resultList.get(0);
                    DateFormat dateFormat = DateFormat.getDateInstance();

                    // Get and parse database contents
                    int valueOrderId = singleOrder.getInt("Nummer");
                    Date valueDate = Objects.requireNonNull(singleOrder.getParseObject("Gesamtauftrag")).getDate("Datum");
                    String valueStrDate = dateFormat.format(valueDate);
                    String valueCustomer = Objects.requireNonNull(Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Name");
                    String valueCustomerStreet = Objects.requireNonNull(Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Strasse");
                    String valueCustomerCity = Objects.requireNonNull(Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getInt("PLZ") + " " + Objects.requireNonNull(Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Ort");
                    String valueElevatorId = Integer.toString(Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getInt("Nummer"));
                    String valueElevatorStreet = Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getString("Strasse");
                    String valueElevatorCity = Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getInt("PLZ") + " " + Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getString("Ort");
                    String valueKeySafe = Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getString("Schluesseldepot");
                    Date valueLastMaintenance = Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getDate("LetzteWartung");
                    String valueStrLastMaintenance = dateFormat.format(valueLastMaintenance);
                    ParseObject objectCustomer = Objects.requireNonNull(singleOrder.getParseObject("Aufzug")).getParseObject("Kunde");

                    // Print headline
                    TextView textHeadline = findViewById(R.id.single_order_overview_headline);
                    textHeadline.setText(String.format("%s: %s", getString(R.string.order_number), valueOrderId));

                    // Print conctact persons headline
                    TextView textContacts = findViewById(R.id.single_order_contacts_headline);
                    textContacts.setText(String.format("%s %s\n%s", getString(R.string.order), valueOrderId, getString(R.string.contact_persons)));

                    // Print order data
                    TextView viewData = findViewById(R.id.single_order_overview_text_data);
                    viewData.setText(String.format("%s: %s\n\n%s\n%s\n%s\n\n%s: %s\n\n%s:\n%s\n%s\n\n%s: %s\n", getString(R.string.date), valueStrDate, valueCustomer, valueCustomerStreet, valueCustomerCity, getString(R.string.elevator_id), valueElevatorId, getString(R.string.location), valueElevatorStreet, valueElevatorCity, getString(R.string.key_safe), valueKeySafe));

                    // Print data of last maintenance
                    TextView viewLastMaintenance = findViewById(R.id.single_order_overview_text_last_maintenance);
                    viewLastMaintenance.setText(String.format("\n%s: %s", getString(R.string.last_maintenance), valueStrLastMaintenance));

                    // Set Google Maps link
                    objectAdress = valueElevatorStreet + " " + valueElevatorCity;
                    objectAdress = objectAdress.replace(" ", "+");
                    objectAdress = "https://www.google.com/maps/dir/?api=1&destination=" + objectAdress;

                    // Get contact persons
                    getContacts(objectCustomer);
                } else {
                    Log.d("Single Order", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Get contact persons for the customer who owns the elevator
    protected void getContacts(ParseObject customer) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Ansprechpartner");
        query.fromLocalDatastore();
        query.whereEqualTo("Kunde", customer);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define table layout
                    TableLayout tableContacts = findViewById(R.id.single_order_contacts_table);

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject person = resultList.get(i);

                        // Get database contents
                        final String name = person.getString("Name");
                        final int phone = person.getInt("Telefon");
                        final String strPhone = Integer.toString(phone);

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams( TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setGravity(Gravity.CENTER_VERTICAL);

                        // Print call button
                        ImageButton callButton = new ImageButton(getApplicationContext());
                        callButton.setLayoutParams(new TableRow.LayoutParams( TableRow.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        callButton.setImageResource(R.drawable.ic_contact_phone_black_48dp);
                        row.addView(callButton);

                        callButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent phoneIntent = new Intent();
                                phoneIntent.setAction(Intent.ACTION_DIAL);
                                phoneIntent.setData(Uri.parse("tel:" + strPhone));
                                startActivity(phoneIntent);
                            }
                        });

                        // Print name and phone number
                        TextView textDescription = new TextView(getApplicationContext());
                        textDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        textDescription.setTypeface(textDescription.getTypeface(), Typeface.BOLD);
                        textDescription.setText(String.format("%s\n%s", name, strPhone));
                        row.addView(textDescription);

                        // Add row to table
                        tableContacts.addView(row);
                    }
                } else {
                    Log.d("Single Order Contact Persons", "Error: " + e.getMessage());
                }
            }
        });
    }
}
