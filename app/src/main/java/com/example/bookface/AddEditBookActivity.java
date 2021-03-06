package com.example.bookface;

import android.content.ActivityNotFoundException;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class is the activity that is used to Add/Edit Books into the system
 */
public class    AddEditBookActivity extends AppCompatActivity implements View.OnClickListener {

    // View declarations
    private TextView scan;
    private TextView back;
    private EditText isbn;
    private EditText author;
    private EditText title;
    private EditText description;
    private Button confirm;
    private FloatingActionButton cameraButton;
    private FloatingActionButton galleryButton;
    private FloatingActionButton deleteButton;
    private ImageView imageView;

    // Constant values that will be used later for Camera stuff
    static final int REQUEST_IMAGE_CAPTURE = 101;
    static final int RESULT_LOAD_IMAGE = 1;

    // Variable declarations
    private Book book;
    String localIsbn, localAuthors, localDescription, localTitle, localImage, localUsername;

    FirebaseAuth mFirebaseAuth;

    private RequestQueue mRequestQueue;
    private static  final  String BASE_URL="https://www.googleapis.com/books/v1/volumes?q=";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_book);

        // Views retrieval
        scan = findViewById(R.id.scanBookButton);
        back = findViewById(R.id.backAddEditBookButton);
        isbn = findViewById(R.id.editISBN);
        author = findViewById(R.id.editName);
        title = findViewById(R.id.editTitle);
        description = findViewById(R.id.editDescription);
        confirm = findViewById(R.id.addEditBookConfirm);
        cameraButton = findViewById(R.id.cameraImage);
        galleryButton = findViewById(R.id.galleryImage);
        deleteButton = findViewById(R.id.deleteImage);
        imageView = findViewById(R.id.addEditImageView);

        // Setting the values
        mRequestQueue = Volley.newRequestQueue(this);
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Setting up the onClickListener for scan button
        scan.setOnClickListener(this);

        // Check for the passed book
        Bundle b = getIntent().getExtras();

        // Setting up the onClickListener for back button
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setting up the onClickListener for camera button
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                    Toast.makeText(getApplicationContext(), "Camera is unavailable", Toast.LENGTH_LONG);
                }
            }
        });

        // Setting up the onClickListener for gallery button
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        // Setting up the onClickListener for delete button
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(b != null){
                    String bookId = (String) b.get("Book");

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String docPath = "books/"+bookId;

                    DocumentReference docRef = db.document(docPath);

                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Map bookData = document.getData();
                                    bookData.put("imageUrl", "");
                                    docRef.set(bookData);
                                }
                            } else {

                            }
                        }
                    });
                }
                imageView.setImageBitmap(null);
            }
        });


        if (b != null) {
            // If passed then retrieve the book and set the fields of its value
            String bookId = (String) b.get("Book");
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Firebase document listener
            final DocumentReference docRef = db.collection("books").document(bookId);
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error == null && value.exists() && value != null) {
                        author.setText(value.getString("author"));
                        description.setText(value.getString("description"));
                        title.setText(value.getString("title"));
                        isbn.setText(value.getString("isbn"));
                        String imgUrl = value.getString("imageUrl");
                        if(!imgUrl.equals(""))
                            Picasso.with(AddEditBookActivity.this).load(imgUrl).into(imageView);
                    }
                }
            });
        }

        // Setting up the onClickListener for confirm button
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                this.writeDB();
            }

            /**
             * This method is adding a new book to db. It also handles book Edit feature
             */
            private void writeDB() {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                if(isbn.getText().toString().length()==0)
                    isbn.setError("FIELD CANNOT BE EMPTY");
                else if(author.getText().toString().length()==0)
                    author.setError("FIELD CANNOT BE EMPTY");
                else if(title.getText().toString().length()==0)
                    title.setError("FIELD CANNOT BE EMPTY");
                else {
                    localIsbn  =isbn.getText().toString();
                    localAuthors = author.getText().toString();
                    localTitle = title.getText().toString();
                    localDescription = description.getText().toString();

                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    mFirebaseAuth = FirebaseAuth.getInstance();


                    String localUsername1 = mFirebaseAuth.getCurrentUser().getDisplayName();

                    StorageReference storageRef = storage.getReferenceFromUrl("gs://bookface-cmput301f20t12.appspot.com");
                    StorageReference mountainsRef = storageRef.child(localIsbn+localUsername1);
                    StorageReference mountainImagesRef = storageRef.child("images/"+localIsbn+localUsername1);

                    mountainsRef.getName().equals(mountainImagesRef.getName());    // true
                    mountainsRef.getPath().equals(mountainImagesRef.getPath());    // false

                    imageView.setDrawingCacheEnabled(true);
                    imageView.buildDrawingCache();
                    Bitmap bitmap = imageView.getDrawingCache();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data1 = baos.toByteArray();

                    UploadTask uploadTask = mountainsRef.putBytes(data1);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                            taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>()
                            {
                                @Override
                                public void onSuccess(Uri downloadUrl)
                                {
                                    localImage = downloadUrl.toString();

                                    mFirebaseAuth = FirebaseAuth.getInstance();
                                    FirebaseUser userInstance = mFirebaseAuth.getCurrentUser();

                                    localUsername = userInstance.getDisplayName();

                                    // Make the instance of the book
                                    book = new Book(localTitle, localAuthors, localIsbn, localDescription,
                                            "Available", localUsername, "Null", localImage);

                                    db.collection("books")
//                                            .document(book.getISBN()+localUsername).set(book).addOnSuccessListener(new OnSuccessListener<Void>()
                                            .document(book.getISBN()+localUsername).set(book)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            String docPath = "users/".concat(localUsername);
                                            DocumentReference docRef = db.document(docPath);
                                            if (b == null) {
                                                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                Map userData = document.getData();

                                                                if (userData != null) {

                                                                    final String TAG = "Completeion Message";
                                                                    ArrayList<String> myBookList =
                                                                            (ArrayList<String>) document.get("booksOwned");
                                                                    myBookList.add(book.getISBN() + localUsername);

                                                                    docRef.update("booksOwned", myBookList)
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {
                                                                                    Log.d(TAG,
                                                                                "DocumentSnapshot successfully updated!");
                                                                                    Toast.makeText(
                                                                                            AddEditBookActivity.this,
                                                                                            "Book Added",
                                                                                            Toast.LENGTH_SHORT).show();
                                                                                    Intent toMyBooks = new
                                                                                            Intent(
                                                                                            AddEditBookActivity.this,
                                                                                            MyBooks.class);
                                                                                    startActivity(toMyBooks);
                                                                                }
                                                                            })
                                                                            .addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Log.w(TAG, "Error updating document", e);
                                                                                }
                                                                            });
                                                                }
                                                            } else {
                                                                System.out.println("DOC does not exist");
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                            else{
                                                Intent toMyBooks = new Intent(AddEditBookActivity.this,
                                                        MyBooks.class);
                                                startActivity(toMyBooks);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(AddEditBookActivity.this, "Error Adding Book",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });
    }




    /**
     * This method is used to handle the onClick to create a scan object of the scan class
     * @param v
     * This is the View Object
     */
    @Override
    public void onClick(View v) {
        Scan scanObj = new Scan(AddEditBookActivity.this);
        scanObj.scanCode();
    }

    /**
     * This is used to overwrite onActivityResult in order to get the image from the scan, camera and gallery calls
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String code = result.getContents();
            if (code != null) {
                this.extractContents(code);
            }
        }

        else if (isbn.getText().toString().length() == 0)
            Toast.makeText(AddEditBookActivity.this, "Scan Book First!", Toast.LENGTH_SHORT).show();

        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }

        else if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri imageUri = data.getData();
            Bitmap image = null;
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(image);
        }
    }

    /**
     * This method parses the input stringified json from web and then extracts data from it
     * @param key
     * This is the url
     * @param code
     * intent result from which contents need to be extracted out
     */
    public void parseJson(String key, String code) {
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, key.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray items = response.getJSONArray("items");
                            int flag = 0;
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                JSONObject volumeInfo = item.getJSONObject("volumeInfo");
                                JSONArray identifiers = volumeInfo.getJSONArray("industryIdentifiers");
                                for (int j = 0; j < identifiers.length(); j++) {
                                    try {
                                        String identifier = identifiers.getJSONObject(j).getString("identifier");
                                        if (code.equals(identifier)) {
                                            flag = 1;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        // TODO
                                        // Handle the exception
                                        System.out.println("There was an exception raised.");
                                    }
                                }
                                if (flag == 1) {
                                    isbn.setText(code);
                                    title.setText(volumeInfo.getString("title"));
                                    description.setText(volumeInfo.getString("description"));
                                    JSONArray authors = volumeInfo.getJSONArray("authors");
                                    String authorsStr = "";
                                    for (int j = 0; j < authors.length(); j++) {
                                        if(j!=0)
                                            authorsStr = authorsStr + ", ";
                                        authorsStr = authorsStr + authors.getString(j);
                                    }
                                    author.setText(authorsStr);
                                    break;
                                }
                            }
                            if(flag ==0){
                                Toast.makeText(AddEditBookActivity.this, "Book Not Found, Add Manually!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(AddEditBookActivity.this, "Book Not Found, Add Manually!",
                                    Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                            Log.e("TAG" , e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mRequestQueue.add(request);
    }

    /**
     * This method extract the contents out of the Intent result string
     * @param code
     * This is the Intent result string from which the contents need to be extracted out
     */
    public void extractContents(String code) {
        Uri uri=Uri.parse(BASE_URL+code);
        Uri.Builder builder = uri.buildUpon();
        this.parseJson(builder.toString(), code);
    }

}
