package com.example.learner

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var dbHelper: TransactionDatabaseHelper
    private val transactionList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        dbHelper = TransactionDatabaseHelper(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, transactionList)
        listView.adapter = adapter

        checkSmsPermission()
        loadTransactionsFromDatabase()  // Load saved transactions from DB
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            readBankTransactions()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                readBankTransactions()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun readBankTransactions() {
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
                    if (transactionDetails != null) {
                        val (amount, receiver) = transactionDetails
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        dbHelper.insertTransaction(amount, receiver, date)  // Save in DB
                    }
                }
            }
        }

        loadTransactionsFromDatabase()  // Refresh UI with latest data
    }

    private fun loadTransactionsFromDatabase() {
        transactionList.clear()
        transactionList.addAll(dbHelper.getLatestMonthTransactions())
        adapter.notifyDataSetChanged()
    }

    private fun extractTransaction(message: String): Pair<String, String>? {
        val regex = """debited by (\d+(\.\d+)?) .*? trf to ([A-Z ]+)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = regex.find(message)

        return matchResult?.let {
            val amount = it.groupValues[1]  // Extract amount
            val receiver = it.groupValues[3]  // Extract receiver name
            Pair(amount, receiver)
        }
    }

    private fun isBankSms(sender: String?, message: String): Boolean {
        return sender?.lowercase()?.contains("paytm") == true ||
                sender?.lowercase()?.contains("gpay") == true ||
                sender?.lowercase()?.contains("phonepe") == true ||
                message.lowercase().contains("debited") ||
                (message.lowercase().contains("a/c") && message.lowercase().contains("trf to"))
    }
}
