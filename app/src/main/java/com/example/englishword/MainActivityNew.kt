package com.example.englishword

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.englishword.fragment.CustomGroupsFragment
import com.example.englishword.fragment.SettingsFragment
import com.example.englishword.fragment.WordListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivityNew : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(WordListFragment())
        }

        // Setup bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_words -> {
                    loadFragment(WordListFragment())
                    true
                }
                R.id.nav_groups -> {
                    loadFragment(CustomGroupsFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
