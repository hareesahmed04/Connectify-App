package com.example.connectifychattingapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.example.connectifychattingapp.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseAuth auth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        auth = FirebaseAuth.getInstance();

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
}