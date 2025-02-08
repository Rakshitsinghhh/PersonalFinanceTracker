package com.example.learner

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.learner.ui.theme.LearnerTheme
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    private lateinit var listView: ListView
    private val transactionList=ArrayList<String>()
    private lateinit var adapter:ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listView=findViewById(R.id.listView)
        adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,transactionList)
        listView.adapter=adapter

        checkSmsPermission()
    }
    private fun checkSmsPermission()
    {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_SMS)
            !=PackageManager.PERMISSION_GRANTED
        )
        {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
        else{
            readBankTransaction()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        {
            isGranted ->
            if(isGranted)
            {
                readBankTransaction()

            }
            else{
                Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show()
            }

        }


    private fun readBankTransaction() {
        val cursor: Cursor? = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null, null, null, "date DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val sender = it.getString(it.getColumnIndexOrThrow("address"))
                val message = it.getString(it.getColumnIndexOrThrow("body"))

                if (isBankSms(sender, message)) {
                    val transactionDetails = extractTransaction(message)
                    if (transactionDetails != "Transaction Not Found") {
                        transactionList.add(transactionDetails)
                    }
                }
            }
            transactionList.reverse()
            adapter.notifyDataSetChanged()
        }
    }


    private fun extractTransaction(message: String): String {
        val regex = """debited by (\d+(\.\d+)?) .*? trf to ([A-Z ]+)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = regex.find(message)

        return matchResult?.let {
            val amount = it.groupValues[1]  // Extract amount
            val receiver = it.groupValues[3]  // Extract receiver name
            "Receiver: $receiver\nAmount: ₹$amount"
        } ?: "Transaction Not Found"
    }




    private fun isBankSms(sender: String?, message: String): Boolean {
        return sender?.lowercase()?.contains("paytm") == true ||
                sender?.lowercase()?.contains("gpay") == true ||
                sender?.lowercase()?.contains("phonepe") == true ||
                message.lowercase().contains("debited")
        message.lowercase().contains("a/c") && message.lowercase().contains("trf to")
    }




    private fun extractTransacation(message: String): String?{
        val regex=Regex("(?:Rs\\.|INR|₹)\\s?([0-9,]+\\.?[0-9]*)")
        val matchResult =regex.find(message)
        return matchResult?.groups?.get(1)?.value?.let { "Transaction: ₹$it" }
    }
}





