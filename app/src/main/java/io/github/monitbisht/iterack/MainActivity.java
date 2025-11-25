package io.github.monitbisht.iterack;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {

    BottomNavigationView navigationView;
    FrameLayout frame;

    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        navigationView = findViewById(R.id.bottom_navigation);
        frame = findViewById(R.id.frame_layout);
        fab = findViewById(R.id.fab_view);

        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.home_tab) {
                    // home fragment
                    FragmentManager fragmentManager1 = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction1 = fragmentManager1.beginTransaction();

                    HomeFragment homeFragment = new HomeFragment();
                    fragmentTransaction1.replace(R.id.frame_layout, homeFragment);

                    fragmentTransaction1.commit();

                } else if (itemId == R.id.today_tab) {
                    //today fragment
                    FragmentManager fragmentManager2 = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction2 = fragmentManager2.beginTransaction();

                    PlannerFragment plannerFragment = new PlannerFragment();
                    fragmentTransaction2.replace(R.id.frame_layout, plannerFragment);

                    fragmentTransaction2.commit();

                } else if (itemId == R.id.insights_tab) {
                    //insight fragment

                    FragmentManager fragmentManager3 = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction3 = fragmentManager3.beginTransaction();

                    InsightFragment insightFragment = new InsightFragment();
                    fragmentTransaction3.replace(R.id.frame_layout, insightFragment);

                    fragmentTransaction3.commit();

                } else {
                    //profile fragment

                    FragmentManager fragmentManager4 = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction4 = fragmentManager4.beginTransaction();

                    ProfileFragment profileFragment = new ProfileFragment();
                    fragmentTransaction4.replace(R.id.frame_layout, profileFragment);

                    fragmentTransaction4.commit();
                }

             return true;

            }
        });
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, new HomeFragment())
                .commit();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_animation,R.anim.exit_animation);
            }
        });

    }

}


