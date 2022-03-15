package com.example.mememanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MyDatabse";
    private static final String TABLE_TXT = "ImageToText";
    private static final String KEY_PATH = "path";
    private static final String KEY_TEXT = "text";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_TXT + "("
                + KEY_PATH + " TEXT PRIMARY KEY," + KEY_TEXT + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TXT);
        onCreate(db);
    }

    void addText(ImageTextData data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATH, data.getImgPath()); // Contact Name
        values.put(KEY_TEXT, data.getContainsText()); // Contact Phone
        db.insertWithOnConflict(TABLE_TXT, null, values,SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    ImageTextData getImageTextData(String path) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TXT, new String[] { KEY_PATH,
                        KEY_TEXT }, KEY_PATH + "=?",
                new String[] { path }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        ImageTextData contact = new ImageTextData(cursor.getString(0),
                cursor.getString(1));
        return contact;
    }

    // code to get all contacts in a list view
    public List<ImageTextData> getAllData() {
        List<ImageTextData> contactList = new ArrayList<ImageTextData>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_TXT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ImageTextData contact = new ImageTextData();
                contact.setImgPath(cursor.getString(0));
                contact.setContainsText(cursor.getString(1));
                // Adding contact to list
                contactList.add(contact);
            } while (cursor.moveToNext());
        }

        return contactList;
    }

    public int getContactsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_TXT;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

}