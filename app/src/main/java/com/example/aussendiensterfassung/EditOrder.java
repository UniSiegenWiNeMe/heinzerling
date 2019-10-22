package com.example.aussendiensterfassung;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION;

public class EditOrder extends AppCompatActivity {

    LinearLayout layoutMaterial;
    LinearLayout layoutMechanics;
    LinearLayout layoutServices;
    ScrollView layoutOverview;
    BottomNavigationView navView;

    ParseObject order;
    String      orderObjectId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.edit_order_nav_material:
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.VISIBLE);
                    getMaterial();
                    return true;
                case R.id.edit_order_nav_mechanics:
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.VISIBLE);
                    getMechanics();
                    return true;
                case R.id.edit_order_nav_services:
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.VISIBLE);
                    getServices();
                    return true;
                case R.id.edit_order_nav_overview:
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.VISIBLE);
                    saveMaterial();
                    saveMechanics();
                    saveServices();
                    getOverview();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_order);

        // Catch OrderID and define OrderID and order ParseObject
        Intent intent = getIntent();
        orderObjectId = intent.getStringExtra("orderObjectId");

        ParseQuery<ParseObject> queryOrder = ParseQuery.getQuery("Einzelauftrag");
        queryOrder.whereEqualTo("objectId", orderObjectId);
        queryOrder.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    order = resultList.get(0);
                } else {
                    Log.d("Edit Order", "Error: " + e.getMessage());
                }
            }
        });

        // Define layouts
        layoutMaterial = findViewById(R.id.edit_order_material_layout);
        layoutMechanics = findViewById(R.id.edit_order_mechanics_layout);
        layoutServices = findViewById(R.id.edit_order_services_layout);
        layoutOverview = findViewById(R.id.edit_order_overview_layout);
        navView = findViewById(R.id.edit_order_navigation);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Initialize buttons
        Button buttonSaveMaterial = findViewById(R.id.edit_order_material_button_save);
        buttonSaveMaterial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMaterial();
            }
        });

        Button buttonSaveMechanics = findViewById(R.id.edit_order_mechanics_button_save);
        buttonSaveMechanics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMechanics();
            }
        });

        Button buttonSaveServices = findViewById(R.id.edit_order_services_button_save);
        buttonSaveServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveServices();
            }
        });

        // Fill user input fields with exisiting data
        getMaterial();
    }

    // Synchronize the material list: SERVER >> USER-INPUT-FIELDS
    protected void getMaterial() {
        // Find previous attached articles
        ParseQuery queryOld = ParseQuery.getQuery("ArtikelAuftrag");
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.include("Artikel");
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size() && i < 8; i++) {
                        // Define fields
                        String inputQtyId = "edit_order_material_input_qty_" + i;
                        String inputArtId = "edit_order_material_input_art_" + i;
                        int inputQtyRes = getResources().getIdentifier(inputQtyId, "id", getPackageName());
                        int inputArtRes = getResources().getIdentifier(inputArtId, "id", getPackageName());

                        EditText fieldQuantity  = findViewById(inputQtyRes);
                        EditText fieldArticle   = findViewById(inputArtRes);

                        // Define position object
                        ParseObject oldPosition = resultList.get(i);

                        // Define field contents
                        Double valueQuantity    = oldPosition.getDouble("Anzahl");
                        String valueArticle     = oldPosition.getParseObject("Artikel").getString("Name");

                        // Set field contents
                        DecimalFormat f = new DecimalFormat("0.00");
                        fieldQuantity.setText(f.format(valueQuantity));
                        fieldArticle.setText(valueArticle);
                    }
                } else {
                    Log.d("AttachArticleQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize the material list: USER-INPUTS >> SERVER.
    protected void saveMaterial() {
        // Find and delete existing positions
        ParseQuery queryOld = ParseQuery.getQuery("ArtikelAuftrag");
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject oldPosition = resultList.get(i);
                        oldPosition.deleteEventually();
                    }
                } else {
                    Log.d("GetArticleQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Save positions
        for (int i=0; i<8; i++) {
            // Get user inputs
            String inputQtyId = "edit_order_material_input_qty_" + i;
            String inputArtId = "edit_order_material_input_art_" + i;
            int inputQtyRes = getResources().getIdentifier(inputQtyId, "id", getPackageName());
            int inputArtRes = getResources().getIdentifier(inputArtId, "id", getPackageName());

            EditText fieldQuantity        = findViewById(inputQtyRes);
            EditText fieldArticle         = findViewById(inputArtRes);
            final String valueStrQuantity = fieldQuantity.getText().toString().replace(",", ".");
            final String valueArticle     = fieldArticle.getText().toString();
            final double valueQuantity;

            // Check if field is empty or quantity is 0 or quantity is not a number
            boolean entryOk = false;

            if (valueStrQuantity == "" || !valueStrQuantity.matches("-?\\d+(\\.\\d+)?") || valueArticle.matches("")) {
                Log.d("AttachArticleCheck", "Info: Entry in line " + i + " is not correct, will not save article.");
            } else {
                if (Double.valueOf(valueStrQuantity) > 0) {
                    entryOk = true;
                } else {
                    Log.d("AttachArticleCheck", "Info: Quantity in line " + i + " is 0, will not save article.");
                }
            }

            if (entryOk) { // User input is okay, proceed
                // Convert value quantity
                valueQuantity = Double.valueOf(valueStrQuantity);

                // Check if article exists
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Artikel");
                query.whereEqualTo("Name", valueArticle);
                query.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) {
                            if (resultList.size() == 0) { // Article does not exist yet, create it
                                ParseObject newArticle = new ParseObject("Artikel");
                                newArticle.put("Name", valueArticle);
                                newArticle.put("Einheit", "stk");
                                newArticle.saveEventually();
                            }

                            // Attach article to order
                            ParseQuery queryArticle = ParseQuery.getQuery("Artikel");
                            queryArticle.whereEqualTo("Name", valueArticle);
                            queryArticle.findInBackground(new FindCallback<ParseObject>() {
                                public void done(List<ParseObject> resultList, ParseException e) {
                                    if (e == null) {
                                        if (resultList.size() == 0) {
                                            // No matching article > Error while creating in previous step
                                            Log.d("AttachArticleQuery", "Error: No matching article, even after creating it.");
                                        } else {
                                            // Get matching article and attach it to order
                                            ParseObject article = resultList.get(0);
                                            ParseObject newPosition = new ParseObject("ArtikelAuftrag");
                                            newPosition.put("Anzahl", valueQuantity);
                                            newPosition.put("Artikel", article);
                                            newPosition.put("Auftrag", order);
                                            newPosition.put("Vorgegeben", false);
                                            newPosition.saveEventually();
                                        }
                                    } else {
                                        Log.d("AttachArticleQuery", "Error: " + e.getMessage());
                                    }
                                }
                            });
                        } else {
                            Log.d("CreateArticleQuery", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    // Synchronize the mechanics list: SERVER >> USER-INPUT-FIELDS
    protected void getMechanics() {
        // Find previous attached persons
        ParseQuery queryOld = ParseQuery.getQuery("MonteurAuftrag");
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.include("Monteur");
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size() && i < 8; i++) {
                        // Define fields
                        String inputQtyId  = "edit_order_mechanics_input_qty_" + i;
                        String inputNameId = "edit_order_mechanics_input_name_" + i;
                        String inputCatId  = "edit_order_mechanics_input_cat_" + i;
                        int inputQtyRes  = getResources().getIdentifier(inputQtyId, "id", getPackageName());
                        int inputNameRes = getResources().getIdentifier(inputNameId, "id", getPackageName());
                        int inputCatRes  = getResources().getIdentifier(inputCatId, "id", getPackageName());

                        EditText fieldQuantity = findViewById(inputQtyRes);
                        EditText fieldName     = findViewById(inputNameRes);
                        EditText fieldCategory = findViewById(inputCatRes);

                        // Define position object
                        ParseObject oldPosition = resultList.get(i);

                        // Define field contents
                        Double valueQuantity = oldPosition.getDouble("Stunden");
                        String valueName     = oldPosition.getParseObject("Monteur").getString("Name");
                        Double valueCategory = oldPosition.getDouble("Kategorie");

                        // Set field contents
                        DecimalFormat f = new DecimalFormat("0.00");
                        fieldQuantity.setText(f.format(valueQuantity));
                        fieldName.setText(valueName);
                        fieldCategory.setText(f.format(valueCategory));
                    }
                } else {
                    Log.d("GetMechanicQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize the mechanics list: USER-INPUTS >> SERVER.
    protected void saveMechanics() {
        // Find and delete existing positions
        ParseQuery queryOld = ParseQuery.getQuery("MonteurAuftrag");
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject oldPosition = resultList.get(i);
                        oldPosition.deleteEventually();
                    }
                } else {
                    Log.d("AttachMechanicsQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Save positions
        for (int i=0; i<8; i++) {
            // Get user inputs
            String inputQtyId  = "edit_order_mechanics_input_qty_" + i;
            String inputNameId = "edit_order_mechanics_input_name_" + i;
            String inputCatId  = "edit_order_mechanics_input_cat_" + i;
            int inputQtyRes  = getResources().getIdentifier(inputQtyId, "id", getPackageName());
            int inputNameRes = getResources().getIdentifier(inputNameId, "id", getPackageName());
            int inputCatRes  = getResources().getIdentifier(inputCatId, "id", getPackageName());

            EditText fieldQuantity        = findViewById(inputQtyRes);
            EditText fieldName            = findViewById(inputNameRes);
            EditText fieldCategory        = findViewById(inputCatRes);
            final String valueStrQuantity = fieldQuantity.getText().toString().replace(",", ".");
            final String valueName        = fieldName.getText().toString();
            final String valueStrCategory = fieldCategory.getText().toString().replace(",", ".");
            final double valueQuantity;
            final double valueCategory;

            // Check if field is empty or quantity is 0 or quantity is not a number
            boolean entryOk = false;

            if (valueStrQuantity == "" || !valueStrQuantity.matches("-?\\d+(\\.\\d+)?") || !valueStrCategory.matches("-?\\d+(\\.\\d+)?") || valueName.matches("")) {
                Log.d("AttachMechanicsCheck", "Info: Entry in line " + i + " is not correct, will not save mechanic.");
            } else {
                if (Double.valueOf(valueStrQuantity) > 0) {
                    entryOk = true;
                } else {
                    Log.d("AttachMechanicsCheck", "Info: Quantity in line " + i + " is 0, will not save mechanic.");
                }
            }

            if (entryOk) { // User input is okay, proceed
                // Convert value quantity and category
                valueQuantity = Double.valueOf(valueStrQuantity);
                valueCategory = Double.valueOf(valueStrCategory);

                // Attach mechanic to order
                ParseQuery queryMechanic = ParseQuery.getQuery("Monteur");
                queryMechanic.whereEqualTo("Name", valueName);
                queryMechanic.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) {
                            if (resultList.size() == 0) {
                                // No matching mechanic, incorrect user input
                                Log.d("AttachMechanicQuery", "Error: No matching mechanic, will not save it.");
                            } else {
                                // Get matching mechanic and attach it to order
                                ParseObject mechanic = resultList.get(0);
                                ParseObject newPosition = new ParseObject("MonteurAuftrag");
                                newPosition.put("Stunden", valueQuantity);
                                newPosition.put("Monteur", mechanic);
                                newPosition.put("Auftrag", order);
                                newPosition.put("Kategorie", valueCategory);
                                newPosition.put("Vorgegeben", false);
                                newPosition.saveEventually();
                            }
                        } else {
                            Log.d("AttachMechanicQuery", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    // Synchronize work and remarks: SERVER >> USER-INPUT-FIELDS
    protected void getServices() {
        // Find previous attached persons
        ParseQuery queryOld = ParseQuery.getQuery("Einzelauftrag");
        queryOld.whereEqualTo("objectId", orderObjectId);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define fields
                    EditText fieldWork     = findViewById(R.id.edit_order_services_input_work);
                    EditText fieldRemarks  = findViewById(R.id.edit_order_services_input_remarks);
                    CheckBox fieldFinished = findViewById(R.id.edit_order_services_input_finished);

                    // Define position object
                    ParseObject orderObject = resultList.get(0);

                    // Define field contents
                    String valueWork      = orderObject.getString("Arbeiten");
                    String valueRemarks   = orderObject.getString("Bemerkungen");
                    Boolean valueFinished = orderObject.getBoolean("Abgeschlossen");

                    // Set field contents
                    fieldWork.setText(valueWork);
                    fieldRemarks.setText(valueRemarks);
                    fieldFinished.setChecked(valueFinished);
                } else {
                    Log.d("GetServicesQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize work and remarks: USER-INPUTS >> SERVER.
    protected void saveServices() {
        // Get user inputs
        EditText fieldWork     = findViewById(R.id.edit_order_services_input_work);
        EditText fieldRemarks  = findViewById(R.id.edit_order_services_input_remarks);
        CheckBox fieldFinished = findViewById(R.id.edit_order_services_input_finished);

        final String valueWork      = fieldWork.getText().toString();
        final String valueRemarks   = fieldRemarks.getText().toString();
        final Boolean valueFinished = fieldFinished.isChecked();

        // Save user intputs to database
        ParseQuery queryOrder = ParseQuery.getQuery("Einzelauftrag");
        queryOrder.whereEqualTo("objectId", orderObjectId);
        queryOrder.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    ParseObject orderObject = resultList.get(0);
                    orderObject.put("Arbeiten", valueWork);
                    orderObject.put("Bemerkungen", valueRemarks);
                    orderObject.put("Abgeschlossen", valueFinished);
                    orderObject.saveEventually();
                } else {
                    Log.d("SaveServicesQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Show positions on the final order overview
    protected void getPositions() {
        // Delete old lines
        ArrayList<View> dynamicRows = new ArrayList<>();
        View rootView = findViewById(R.id.edit_order_overview_layout);
        rootView.findViewsWithText(dynamicRows, "dynamicRow", FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

        for (int i=0; i<dynamicRows.size(); i++) {
            View obsoleteRow = dynamicRows.get(i);
            ((ViewGroup)obsoleteRow.getParent()).removeView(obsoleteRow);
        }

        // Get material
        ParseQuery<ParseObject> queryMaterial = ParseQuery.getQuery("ArtikelAuftrag");
        queryMaterial.whereEqualTo("Auftrag", order);
        queryMaterial.include("Artikel");

        queryMaterial.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define position table
                    TableLayout tablePositions = findViewById(R.id.edit_order_overview_table_material);

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject materialPosition = resultList.get(i);

                        // Get field contents out of the database
                        int pos = i + 1;
                        double quantity = materialPosition.getDouble("Anzahl");
                        String unit = materialPosition.getParseObject("Artikel").getString("Einheit");
                        String name = materialPosition.getParseObject("Artikel").getString("Name");

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setContentDescription("dynamicRow");

                        // Print position
                        TextView textPosition = new TextView(getApplicationContext());
                        textPosition.setText(Integer.toString(pos));
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
                    Log.d("GetFinalMaterialQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Get mechanics
        ParseQuery<ParseObject> queryMechanics = ParseQuery.getQuery("MonteurAuftrag");
        queryMechanics.whereEqualTo("Auftrag", order);
        queryMechanics.include("Monteur");

        queryMechanics.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define position table
                    TableLayout tablePositions = findViewById(R.id.edit_order_overview_table_mechanics);

                    if (resultList.size() > 0) {
                        tablePositions.setVisibility(View.VISIBLE);
                    } else {
                        tablePositions.setVisibility(View.GONE);
                    }

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject mechanicPosition = resultList.get(i);

                        // Get field contents out of the database
                        int pos = i + 1;
                        double quantity = mechanicPosition.getDouble("Stunden");
                        String name = mechanicPosition.getParseObject("Monteur").getString("Name");

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setContentDescription("dynamicRow");

                        // Print position
                        TextView textPosition = new TextView(getApplicationContext());
                        textPosition.setText(Integer.toString(pos));
                        textPosition.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textPosition);

                        // Print quantity
                        TextView textQuantity = new TextView(getApplicationContext());
                        DecimalFormat f = new DecimalFormat("0.00");
                        textQuantity.setText(f.format(quantity) + " Std.");
                        textQuantity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textQuantity);

                        // Print name
                        TextView textName = new TextView(getApplicationContext());
                        textName.setText(name);
                        textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textName);

                        // Attach table row to table
                        tablePositions.addView(row);
                    }
                } else {
                    Log.d("GetFinalMechanicsQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Show the final order overview
    protected void getOverview() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.whereEqualTo("objectId", orderObjectId);
        query.include("Aufzug");
        query.include("Aufzug.Kunde");
        query.include("Gesamtauftrag");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    ParseObject finalOrder = resultList.get(0);

                    // Get and parse database contents
                    int valueOrderId = finalOrder.getInt("Nummer");
                    Date valueDate = finalOrder.getParseObject("Gesamtauftrag").getDate("Datum");
                    String valueStrDate = valueDate.getDate() + "." + (valueDate.getMonth()+1) + "." + (valueDate.getYear()+1900);
                    String valueCustomer = finalOrder.getParseObject("Aufzug").getParseObject("Kunde").getString("Name");
                    String valueCustomerStreet = finalOrder.getParseObject("Aufzug").getParseObject("Kunde").getString("Strasse");
                    String valueCustomerCity = finalOrder.getParseObject("Aufzug").getParseObject("Kunde").getInt("PLZ") + " " + finalOrder.getParseObject("Aufzug").getParseObject("Kunde").getString("Ort");
                    String valueElevatorId = Integer.toString(finalOrder.getParseObject("Aufzug").getInt("Nummer"));
                    String valueElevatorStreet = finalOrder.getParseObject("Aufzug").getString("Strasse");
                    String valueElevatorCity = finalOrder.getParseObject("Aufzug").getInt("PLZ") + " " + finalOrder.getParseObject("Aufzug").getString("Ort");
                    String valueKeySafe = finalOrder.getParseObject("Aufzug").getString("Schluesseldepot");
                    Date valueLastMaintenance = finalOrder.getParseObject("Aufzug").getDate("LetzteWartung");
                    String valueStrLastMaintenance = valueLastMaintenance.getDate() + "." + (valueLastMaintenance.getMonth()+1) + "." + (valueLastMaintenance.getYear()+1900);
                    String valueWork = finalOrder.getString("Arbeiten");
                    String valueRemarks = finalOrder.getString("Bemerkungen");


                    // Print headline
                    TextView textHeadline = findViewById(R.id.edit_order_overview_headline);
                    textHeadline.setText("Auftrag Nummer " + valueOrderId);

                    // Print order data
                    TextView viewData = findViewById(R.id.edit_order_overview_text_data);
                    viewData.setText("Datum: "+ valueStrDate + "\n\n" + valueCustomer + "\n" + valueCustomerStreet + "\n" + valueCustomerCity + "\n\nAufzugnummer: "+ valueElevatorId + "\n\n" + "Standort:\n" + valueElevatorStreet + "\n" + valueElevatorCity + "\n\nSchlüsseldepot: " + valueKeySafe + "\n");

                    // Print data of last maintenance
                    TextView viewLastMaintenance = findViewById(R.id.edit_order_overview_text_lastmaintenance);
                    viewLastMaintenance.setText("\nLetzte Wartung: " + valueStrLastMaintenance);

                    // Print work
                    TextView viewWork = findViewById(R.id.edit_order_overview_text_work);
                    if (!valueWork.matches("")) {
                        viewWork.setVisibility(View.VISIBLE);
                        viewWork.setText("\nAusgeführte Arbeiten: " + valueWork);
                    } else {
                        viewWork.setVisibility(View.GONE);
                    }

                    // Print remarks
                    TextView viewRemarks = findViewById(R.id.edit_order_overview_text_remarks);
                    if (!valueRemarks.matches("")) {
                        viewRemarks.setVisibility(View.VISIBLE);
                        viewRemarks.setText("\nBemerkungen: " + valueRemarks + "\n\n\n");
                    } else {
                        viewRemarks.setVisibility(View.GONE);
                    }
                } else {
                    Log.d("GetFinalOverviewQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Get positions (material + mechanics)
        getPositions();
    }
}