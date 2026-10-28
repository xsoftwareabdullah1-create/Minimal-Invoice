package com.minimalinvoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InvoiceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var instance: InvoiceDatabase? = null

        fun get(context: Context): InvoiceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "minimal-invoice.db"
                ).build().also { instance = it }
            }
    }
}
