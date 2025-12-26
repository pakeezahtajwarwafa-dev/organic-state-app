//package com.example.organicstate.data.local
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import com.example.organicstate.data.model.CartItem
//
//@Database(entities = [CartItem::class], version = 1, exportSchema = false)
//abstract class CartDatabase : RoomDatabase() {
//
//    abstract fun cartDao(): CartDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: CartDatabase? = null
//
//        fun getDatabase(context: Context): CartDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    CartDatabase::class.java,
//                    "cart_database"
//                ).build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}