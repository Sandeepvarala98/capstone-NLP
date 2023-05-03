package com.example.speechtoimage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.graphics.Bitmap
import org.json.JSONObject

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "database_d19.db", null, 1) {
    //context.applicationInfo.dataDir + "/databases/"
    override fun onCreate(db: SQLiteDatabase) {
        // Define the schema of your database here
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS images (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "type TEXT,"+
                    "image TEXT)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS menu (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "menu_text TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here
    }

    fun SaveData(name: String, type:String, image: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("type",type)
        values.put("image",image)
        db.insert("images", null, values)
        db.close()
    }
    fun SaveMenu(name: String,menu:String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("menu_text",menu)
        db.insert("menu", null, values)
        db.close()
    }
    fun UpdateMenu(name: String,menu:String) {
        val db = this.writableDatabase
        val values = ContentValues()
//        values.put("name", name)
        values.put("menu_text",menu)
        //db.insert("images", null, values)
        db.update("menu", values, "name = ?", arrayOf(name))
        db.close()
    }

}