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
        private const val COLUMN_REF_NO = "ref_no"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_AMOUNT TEXT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_DATE TEXT,
                $COLUMN_REF_NO TEXT UNIQUE
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertTransaction(amount: String, receiver: String, date: String, refNo: String) {
        val db = writableDatabase

        // Check if transaction already exists (by Ref No.)
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_REF_NO = ?", arrayOf(refNo))
        if (cursor.count > 0) {
            cursor.close()
            db.close()
            return  // Skip duplicate entry
        }
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_RECEIVER, receiver)
            put(COLUMN_DATE, date)  // Ensure date is stored in 'YYYY-MM-DD' format
            put(COLUMN_REF_NO, refNo) // Store unique reference number
        }

        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    // Get last 3 months' expenditure
    fun getLastThreeMonthsSpending(): List<Pair<String, Double>> {
        val db = readableDatabase
        val resultList = mutableListOf<Pair<String, Double>>()

        val cursor = db.rawQuery(
            """
            SELECT strftime('%Y-%m', $COLUMN_DATE) AS month, 
                   SUM(CAST($COLUMN_AMOUNT AS REAL)) 
            FROM $TABLE_NAME 
            WHERE date($COLUMN_DATE) >= date('now', '-3 months') 
            GROUP BY month 
            ORDER BY month DESC
            """,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val month = it.getString(0)  // Extract month
                val total = it.getDouble(1)  // Extract total amount
                resultList.add(Pair(month, total))
            }
        }

        db.close()
        return resultList  // Returns list of (Month, Total Spent)
    }

    // Get latest month transactions
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
                val date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE))
                transactions.add("📅 $date | Receiver: $receiver | Amount: ₹$amount")
            }
        }
        db.close()
        return transactions
    }
}