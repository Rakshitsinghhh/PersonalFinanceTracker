package com.example.learner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class TransactionDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "transactions.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "transactions"
        private const val COLUMN_ID = "id"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_RECEIVER = "receiver"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_AMOUNT TEXT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_DATE TEXT
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // ✅ Insert Transaction
    fun insertTransaction(amount: String, receiver: String, date: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_RECEIVER, receiver)
            put(COLUMN_DATE, date)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    // ✅ Get Latest Month Transactions
    fun getLatestMonthTransactions(): List<String> {
        val db = readableDatabase
        val latestMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_DATE LIKE '$latestMonth%' ORDER BY $COLUMN_DATE DESC"
        val cursor = db.rawQuery(query, null)

        val transactions = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                val amount = it.getString(it.getColumnIndexOrThrow(COLUMN_AMOUNT))
                val receiver = it.getString(it.getColumnIndexOrThrow(COLUMN_RECEIVER))
                transactions.add("Receiver: $receiver\nAmount: ₹$amount")
            }
        }
        db.close()
        return transactions
    }
}
