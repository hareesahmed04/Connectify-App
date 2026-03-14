package com.example.connectifychattingapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager.widget.ViewPager;
import com.example.connectifychattingapp.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Set the status bar background to black
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        // Ensure the icons are white/light so they are visible on the black background
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        // 2. Navigation Bar Setup (Black background, white icons)
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        windowInsetsController.setAppearanceLightNavigationBars(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish(); // Important: This removes MainActivity from the backstack
            return;
        }

        binding.viewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));

        binding.btNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.nav_home) {
                    binding.viewPager.setCurrentItem(0);
                } else if (id == R.id.nav_contact) {
                    binding.viewPager.setCurrentItem(1);
                } else if (id == R.id.nav_call) {
                    binding.viewPager.setCurrentItem(2);
                }
                return true;
            }
        });
        // --- INCOMING CALL LISTENER ---
        database.getReference().child("calls").child(auth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String status = snapshot.child("status").getValue(String.class);

                            // Only trigger if the status is "ringing"
                            if ("ringing".equals(status)) {
                                String callerName = snapshot.child("callerName").getValue(String.class);
                                String callerId = snapshot.child("callerId").getValue(String.class);
                                String channelId = snapshot.child("channelId").getValue(String.class);
                                String type = snapshot.child("type").getValue(String.class);
                                String profilePic = snapshot.child("callerPic").getValue(String.class);

                                // Launch the appropriate incoming screen
                                launchIncomingScreen(type, callerName, callerId, channelId, profilePic);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }
        @Override
        public void onPageSelected(int position) {
            binding.btNav.getMenu().getItem(position).setChecked(true);
        }
        @Override
        public void onPageScrollStateChanged(int state) {
        }
    });
}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            // Handle settings
            Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout) {
            auth.signOut();
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        // Check if ViewPager is currently on any page other than the first one (Chat)
        if (binding.viewPager.getCurrentItem() != 0) {
            // Switch to the first tab (Chat Fragment)
            binding.viewPager.setCurrentItem(0);
        } else {
            // If already on Chat Fragment, proceed with default back action (exit app)
            super.onBackPressed();
        }
    }
    private void launchIncomingScreen(String type, String name, String id, String channel, String profilePic) {
        Intent intent;
        if ("video".equals(type)) {
            intent = new Intent(MainActivity.this, VideoCallIncomingActivity.class);
        } else {
            intent = new Intent(MainActivity.this, AudioIncomingActivity.class);
        }
        intent.putExtra("callerName", name);
        intent.putExtra("callerId", id);
        intent.putExtra("channelId", channel);
        intent.putExtra("profilePic", profilePic);

        // This flag is critical for launching from a background listener
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}

