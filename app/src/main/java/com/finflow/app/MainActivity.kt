package com.finflow.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Update last-active timestamp so auto-logout timer resets on each app open
        getSharedPreferences("finflow_prefs", MODE_PRIVATE)
            .edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add_expense)
        fab.setOnClickListener {
            navController.navigate(R.id.addExpenseFragment)
        }
    }
}
