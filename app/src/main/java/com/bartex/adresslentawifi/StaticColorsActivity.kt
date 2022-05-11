package com.bartex.adresslentawifi

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bartex.adresslentawifi.databinding.ActivityStaticColorsBinding

class StaticColorsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding:ActivityStaticColorsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticColorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button1.setOnClickListener(this)
        binding.button2.setOnClickListener(this)
        binding.button3.setOnClickListener(this)
        binding.button4.setOnClickListener(this)
        binding.button5.setOnClickListener(this)
        binding.button6.setOnClickListener(this)
        binding.button7.setOnClickListener(this)
        binding.button8.setOnClickListener(this)
        binding.button9.setOnClickListener(this)
        binding.button10.setOnClickListener(this)
        binding.button11.setOnClickListener(this)
        binding.button12.setOnClickListener(this)
        binding.button13.setOnClickListener(this)
        binding.button14.setOnClickListener(this)
        binding.button15.setOnClickListener(this)
        binding.button16.setOnClickListener(this)
        binding.button17.setOnClickListener(this)
        binding.button18.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        val colorCode = p0?.tag as String
        binding.textViewColor.text = colorCode
    }
}