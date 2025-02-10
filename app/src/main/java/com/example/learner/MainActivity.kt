package com.example.learner

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
        loadTransactionsFromDatabase()  // Load saved transactions
    }

    // Load transactions and last 3 months' expenditure
    private fun loadTransactionsFromDatabase() {
        transactionList.clear()

        // Fetch last 3 months' total spending
        val monthlyTotals = dbHelper.getLastThreeMonthsSpending()
        for ((month, total) in monthlyTotals) {
            val formattedTotal = "📅 $month | Spent: ₹%.2f".format(total)
            transactionList.add(formattedTotal)
        }

        // Load latest month transactions below the summary
        transactionList.addAll(dbHelper.getLatestMonthTransactions())

        adapter.notifyDataSetChanged()
    }

    // Extract transaction details from SMS
    private fun extractTransaction(message: String): Triple<String, String, String>? {
        val regex = """debited by (\d+(\.\d+)?) .*? trf to ([A-Z ]+).*?Ref No\. (\w+)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = regex.find(message)

        return matchResult?.let {
            val amount = it.groupValues[1]  // Extract amount
            val receiver = it.groupValues[3]  // Extract receiver name
            val refNo = it.groupValues[4]  // Extract Ref No.
            Triple(amount, receiver, refNo)
        }
    }

    // Check & Request SMS Permission
    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            readBankTransactions()
        }
    }

    // Handle SMS Permission Request
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                readBankTransactions()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Read Transactions from SMS
    private fun readBankTransactions() {
        val cursor: Cursor? = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null, null, null, "date DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val sender = it.getString(it.getColumnIndexOrThrow("address"))
                val message = it.getString(it.getColumnIndexOrThrow("body"))
                val timestamp = it.getLong(it.getColumnIndexOrThrow("date"))
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

                if (isBankSms(sender, message)) {
                    val transactionDetails = extractTransaction(message)
                    if (transactionDetails != null) {
                        val (amount, receiver, refNo) = transactionDetails
                        dbHelper.insertTransaction(amount, receiver, formattedDate, refNo)  // Insert with Ref No.
                    }
                }
            }
        }

        loadTransactionsFromDatabase()
    }

    // Identify Bank Messages
    private fun isBankSms(sender: String?, message: String): Boolean {
        return sender?.lowercase()?.contains("paytm") == true ||
                sender?.lowercase()?.contains("gpay") == true ||
                sender?.lowercase()?.contains("phonepe") == true ||
                message.lowercase().contains("debited") ||
                (message.lowercase().contains("a/c") && message.lowercase().contains("trf to"))
    }
}