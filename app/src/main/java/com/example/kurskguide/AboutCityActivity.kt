package com.example.kurskguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class AboutCityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_city)

        title = "О городе Курск"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.tvCityInfo).text = """
            Курск — город в России, административный центр Курской области. 
            
            История города началась в 1032 году, когда была основана Курская крепость для защиты южных рубежей Руси.
            
            Население: около 440 000 человек
            
            Курск известен:
            • Курской магнитной аномалией
            • Курской битвой 1943 года
            • Коренной пустынью и чудотворной иконой
            • Развитой промышленностью
            
            Климат умеренно континентальный, с тёплым летом и умеренно холодной зимой.
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}