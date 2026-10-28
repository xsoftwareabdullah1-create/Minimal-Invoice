package com.minimalinvoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): InvoiceEntity?

    @Query("SELECT COALESCE(MAX(invoiceNumber), 0) + 1 FROM invoices")
    suspend fun nextInvoiceNumber(): Int

    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET paid = :paid WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)
}
