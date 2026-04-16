package com.example.precisionlayertesting.features.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.databinding.FragmentGetStartedBinding


class GetStartedActivity : AppCompatActivity() {

    private lateinit var binding: FragmentGetStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOrganization.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnIndividual.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
