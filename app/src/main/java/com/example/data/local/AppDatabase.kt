package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MilkProductEntity::class,
        CustomerEntity::class,
        DailyMilkEntryEntity::class,
        PaymentEntity::class,
        MonthlyBillEntity::class,
        BillAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun milkProductDao(): MilkProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun dailyMilkEntryDao(): DailyMilkEntryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun monthlyBillDao(): MonthlyBillDao

    companion object {
        const val DATABASE_NAME = "snehal_milk_manager.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            lateinit var db: AppDatabase
            db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(sqliteDb: SupportSQLiteDatabase) {
                        super.onCreate(sqliteDb)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultProducts(db.milkProductDao())
                        }
                    }
                })
                .build()
            return db
        }

        suspend fun seedDefaultProducts(productDao: MilkProductDao) {
            if (productDao.count() > 0) return
            productDao.insertAll(
                listOf(
                    MilkProductEntity(
                        name = "Full Cream Milk",
                        packetSizeMl = 500,
                        pricePerPacket = 33.0,
                        isActive = true,
                        description = "Rich 6% fat full cream dairy packet"
                    ),
                    MilkProductEntity(
                        name = "Standard Milk",
                        packetSizeMl = 500,
                        pricePerPacket = 30.0,
                        isActive = true,
                        description = "Daily 4.5% fat standard fresh milk packet"
                    ),
                    MilkProductEntity(
                        name = "Toned Milk",
                        packetSizeMl = 500,
                        pricePerPacket = 27.0,
                        isActive = true,
                        description = "Light 3% fat toned milk packet"
                    ),
                    MilkProductEntity(
                        name = "Cow Milk",
                        packetSizeMl = 500,
                        pricePerPacket = 28.0,
                        isActive = true,
                        description = "Fresh pure cow milk packet"
                    ),
                    MilkProductEntity(
                        name = "Full Cream Milk (1L)",
                        packetSizeMl = 1000,
                        pricePerPacket = 66.0,
                        isActive = true,
                        description = "Family 1 litre full cream milk packet"
                    )
                )
            )
        }
    }
}
