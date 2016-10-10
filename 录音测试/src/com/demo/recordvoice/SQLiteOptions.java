package com.demo.recordvoice;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteOptions extends SQLiteOpenHelper{
	public SQLiteOptions(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	SQLiteDatabase sqLiteDatabase;
	Context context;	
	
	/**
	 * �����ݿ⣬������(int��ID��int����ֵ)
	 * @param databaseName ���ݿ�����
	 * @param table ������
	 */
	public void openDataBase(String databaseName, String table) {
		sqLiteDatabase = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null);
		
		String CREATE_TABLE = "create table if not exists"+ table+ "(_id INTEGER PRIMARY KEY,value INTEGER);";
		sqLiteDatabase.execSQL(CREATE_TABLE);
	}
	
	/**
	 * ���һ������
	 * @param id
	 * @param value
	 * @param name
	 */
	public void addData(int value, String table) {
		ContentValues cv = new ContentValues();
		//cv.put("_id", id);
		cv.put("value", value);
		sqLiteDatabase.insert(table, null, cv);
	}
	
	/**
	 * ɾ��һ������
	 * @param table ���
	 * @param id 
	 */
	public void deleteData(String table, int id) {
		String DELETE_DATA = "DELETE FROM "+table+"WHERE _id=" + id + ";";
		sqLiteDatabase.execSQL(DELETE_DATA);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
