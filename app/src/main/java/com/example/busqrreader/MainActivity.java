package com.example.busqrreader;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    CodeScanner codeScanner;
    CodeScannerView scannerView;
    LinkList linkList;
    private DatabaseReference mDatabase;
    TripDetails tripDetails;
    MediaPlayer mediaPlayer;

    static boolean val;

    private static final int TIME_INTERVAL = 2000;
    private long mBackPressed;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbarMain);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle("Bus Scanner");
        setSupportActionBar(toolbar);

        linkList = new LinkList();

        scannerView = findViewById(R.id.scanner);

        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {

                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        CountDownTimer timer = new CountDownTimer(4000,4000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                String id = result.getText();

                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Passanger");

                                Query checkInspector = reference.orderByChild("pid").equalTo(id);

                                checkInspector.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            setTrue();
                                            double balance = getCurrentAmount(dataSnapshot);
                                            if(balance<100){
                                                Toast.makeText(getApplicationContext(),"Not enough balance",Toast.LENGTH_LONG).show();
                                                setFalse();
                                            }
                                        } else {
                                            setFalse();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onFinish() {
                                String id = result.getText();
                                if (val) {
                                    Link link = linkList.deleteLink(id);

                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                                    LocalDateTime now = LocalDateTime.now();
                                    String nowTime = dtf.format(now);

                                    String location = getLocation();

                                    if (link == null) {
                                        String date = java.time.LocalDate.now().toString();

                                        Link newLink = new Link(id, nowTime, location, date);
                                        linkList.insertFirst(newLink);

                                        soundWelcome();
                                    } else {
                                        mDatabase = FirebaseDatabase.getInstance().getReference();

                                        double amt;
                                        if (location.equals(link.departure)) {
                                            amt = 12.00;
                                        } else {
                                            amt = getAmount();
                                        }

                                        tripDetails = new TripDetails();

                                        tripDetails.setPid(link.id);
                                        tripDetails.setStartLocation(link.departure);
                                        tripDetails.setEndLocation(location);
                                        tripDetails.setStartTime(link.startTime);
                                        tripDetails.setEndTime(nowTime);
                                        tripDetails.setAmount(Double.toString(amt));
                                        tripDetails.setDate(link.date);

                                        mDatabase.child("TripDetails").push().setValue(tripDetails);

                                        soundGoodbye();

                                        clearAll(tripDetails);
                                    }
                                } else {
                                    soundError();
                                }
                            }
                        };
                        timer.start();
                    }
                });

            }
        });

        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                codeScanner.startPreview();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        requestForCamera();
    }

    private void requestForCamera() {
        Dexter.withActivity(this).withPermission(Manifest.permission.CAMERA).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                codeScanner.startPreview();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                Toast.makeText(getApplicationContext(), "Camera Permission is Required.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                token.continuePermissionRequest();

            }
        }).check();
    }

    public void clearAll(TripDetails tripDetails) {
        tripDetails.setPid("");
        tripDetails.setStartLocation("");
        tripDetails.setEndLocation("");
        tripDetails.setStartTime("");
        tripDetails.setEndTime("");
        tripDetails.setDate("");
        tripDetails.setAmount("");
    }

    public void soundWelcome() {
        mediaPlayer = null;
        mediaPlayer = MediaPlayer.create(this, R.raw.sound_welcome);
        mediaPlayer.start();
    }

    public void soundGoodbye() {
        mediaPlayer = null;
        mediaPlayer = MediaPlayer.create(this, R.raw.sound_goodbye);
        mediaPlayer.start();
    }

    public void soundError() {
        mediaPlayer = null;
        mediaPlayer = MediaPlayer.create(this, R.raw.sound_error);
        mediaPlayer.start();
    }

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        } else {
            Toast.makeText(getBaseContext(), "Click two times to close an activity", Toast.LENGTH_SHORT).show();
        }
        mBackPressed = System.currentTimeMillis();
    }

    private void setTrue() {
        val = true;
    }

    private void setFalse() {
        val = false;
    }

    private double getCurrentAmount(DataSnapshot dataSnapshot){

        double amt = 0.0;

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            String amount = ds.child("amount").getValue(String.class);
            amt = Double.parseDouble(amount);
        }

        return amt;
    }

    private String getLocation() {
        List<String> list = new ArrayList<>();

        list.add("Colombo Fort");
        list.add("Kollupitiya");
        list.add("Wellawattha");
        list.add("Maradana");
        list.add("Bambalapitiya");
        list.add("Borella");
        list.add("Dehiwala");
        list.add("Battaramulla");
        list.add("Kotte");
        list.add("Rajagiriya");
        list.add("Kesbewa");
        list.add("Moratuwa");
        list.add("Nugegoda");
        list.add("Mount-Lavinia");
        list.add("Dematagoda");
        list.add("Grandpass");
        list.add("Nawala");
        list.add("Cinnamon Gardens");
        list.add("Mattakuliya");
        list.add("Kochchikade");

        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }

    private double getAmount() {
        List<Double> list = new ArrayList<>();

        list.add(50.00);
        list.add(45.00);
        list.add(75.00);
        list.add(15.00);
        list.add(20.00);
        list.add(18.00);
        list.add(22.00);
        list.add(25.00);
        list.add(20.00);
        list.add(28.00);
        list.add(30.00);
        list.add(32.00);
        list.add(35.00);
        list.add(38.00);
        list.add(40.00);
        list.add(42.00);
        list.add(48.00);
        list.add(55.00);
        list.add(60.00);
        list.add(65.00);
        list.add(70.00);

        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }
}