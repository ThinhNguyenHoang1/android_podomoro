package com.example.poromodo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.example.poromodo.databinding.ActivityMainBinding
import com.example.poromodo.model.AppDatabase
import com.example.poromodo.ui.home.HomeFragment
import com.example.poromodo.ui.settings.SettingsFragment
import com.example.poromodo.ui.stats.StatFragment
/* TODO:
    Fix the first time loading the code. (Cause: current timeLeft is default from 25 MINUTE instead
    of the current configured value)

    Fix the Dialog not showing up

 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "podomoro_app.db"
        ).build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val homeFragment = HomeFragment()
        val statFragment = StatFragment()
        val settingsFragment = SettingsFragment()

        setCurrentFragment(homeFragment)
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.miHome -> setCurrentFragment(homeFragment)
                R.id.miStat -> setCurrentFragment(statFragment)
                R.id.miSettings -> setCurrentFragment(settingsFragment)

            }
            true
        }
    }
    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragmentMain, fragment)
            commit()
        }
    }

}