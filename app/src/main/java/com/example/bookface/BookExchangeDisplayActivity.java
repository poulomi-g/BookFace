package com.example.bookface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This activity handles the display of the request once accepted
 */
public class BookExchangeDisplayActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Declare variables
    Geocoder geo, geoStartUp;
    GoogleMap gMap;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser userInstance;
    String owner, author, borrower, title, status, isbn, imgUrl;
    String requestId, rStatus, borrowerId, bookId;
    String currentUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_exchange_display);

        mFirebaseAuth = FirebaseAuth.getInstance();
        userInstance = mFirebaseAuth.getCurrentUser();

        Button btnTop = findViewById(R.id.myBooksOrMyRequestsButton);
        Button btnBottom = findViewById(R.id.scanBookButton);

        TextView textAuthor = (TextView) findViewById(R.id.authorNameText);
        TextView textTitle = (TextView) findViewById(R.id.titleText);
        TextView textIsbn = (TextView) findViewById(R.id.isbnText);
        TextView textStatus = (TextView) findViewById(R.id.statusText);
        ImageView image = (ImageView) findViewById(R.id.imageView);

        if (userInstance != null){
            currentUser = (String) userInstance.getDisplayName();

            // Retrieve the request passed through the intent
            Bundle b = getIntent().getExtras();
            if (b!= null) {
                requestId = (String) b.get("REQUEST_ID");
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();


            DocumentReference docRefRequest = db.collection("requests").document(requestId);
            docRefRequest.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error == null && value.exists() && value != null) {
                        docRefRequest.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Map requestData = document.getData();
                                        DocumentReference bookRef = (DocumentReference) requestData.get("bookid");
                                        bookRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot value = task.getResult();
                                                    if (value.exists()) {
//
                                                        Map bookData = value.getData();
                                                        owner = bookData.get("ownerUsername").toString();
                                                        author = bookData.get("author").toString();
                                                        isbn = value.get("isbn").toString();
                                                        status = value.get("status").toString();
                                                        title = value.get("title").toString();
                                                        imgUrl = value.get("imageUrl").toString();
                                                        System.out.println("Owner: "+owner+" ++++++++++++++++");
                                                        System.out.println("ISBN: "+isbn+" ++++++++++++++++");

                                                        if (value.get("borrowerUserName") == null) {
                                                            borrower = "No current borrower";
                                                        }
                                                        else {
                                                            borrower = value.get("borrowerUserName").toString();
                                                        }

                                                        textAuthor.setText(author);
                                                        textIsbn.setText(isbn);
                                                        textStatus.setText(status);
                                                        textTitle.setText(Html.fromHtml("<b>" + title + "</b>"));
                                                        if (!imgUrl.equals("")) {
                                                            Picasso.with(getApplicationContext()).load(imgUrl).into(image);
                                                        }

                                                        // TODO
                                                        if (owner.equals(currentUser)) {
                                                            btnTop.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View view) {
                                                                    // Call MyBooks Activity
                                                                    Intent toMyBooks = new Intent(BookExchangeDisplayActivity.this, MyBooks.class);
                                                                    startActivity(toMyBooks);
                                                                }
                                                            });

                                                            btnBottom.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View view) {
                                                                    // Handle the book scan to handover
                                                                    Scan scanObj = new Scan(BookExchangeDisplayActivity.this);
                                                                    scanObj.scanCode();
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            btnTop.setText("My Requests");
                                                            if (status.equals("borrowed".toLowerCase())) {
                                                                btnBottom.setText("Scan to Return");
                                                            }
                                                            else {
                                                                btnBottom.setText("Scan to Borrow");
                                                            }
                                                            btnTop.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View view) {
                                                                    // Call MyRequests Activity
                                                                }
                                                            });

                                                            btnBottom.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View view) {
                                                                    Scan scanObj = new Scan(BookExchangeDisplayActivity.this);
                                                                    scanObj.scanCode();
                                                                }
                                                            });

                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String code = result.getContents();
            if (code != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference docRefRequest = db.collection("requests").document(requestId);
                if (owner.equals(currentUser)){
                    docRefRequest.update("exchangeowner", isbn);
                }
                else{
                    docRefRequest.update("exchangeborrower", isbn);
                }
                docRefRequest.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null) {
                                Map requestData = document.getData();
                                DocumentReference bookref = (DocumentReference) requestData.get("bookid");
                                if(requestData.get("exchangeowner") == requestData.get("exchangeborrower")){
                                    if(status.toLowerCase() == "accepted"){
                                        bookref.update("status", "Borrowed");
                                    }
                                    else if(status.toLowerCase() == "borrowed"){
                                        bookref.update("status", "Available");
                                    }
                                }
                            } else {

                            }
                        } else {
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        Location currentLocation = null;
        TextView textAddress = (TextView) findViewById(R.id.addressText);

        // Retrieve current location from the firebase
        // Code built upon: https://firebaseopensource.com/projects/firebase/geofire-android/
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("path/to/geofire");
        GeoFire geoFire = new GeoFire(ref);

        geoFire.getLocation(requestId, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (location != null) {
                    currentLocation.setLatitude(location.latitude);
                    currentLocation.setLongitude(location.longitude);
                    System.out.println(String.format("The location for key %s is [%f,%f]", key, location.latitude, location.longitude));
                }
                else {
                    System.out.println(String.format("There is no location for key %s in GeoFire", key));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("There was an error getting the GeoFire location: " + databaseError);
            }
        });

        // Go to the location address
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        gMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

        // Display the address
        List<Address> addresses = null;
        geoStartUp = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geoStartUp.getFromLocation(latLng.latitude, latLng.longitude, 1);
            // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0);
            textAddress.setText(address);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null) {
            // Insert marker at the location
            gMap.addMarker(new MarkerOptions().position(latLng));
        }
    }
}