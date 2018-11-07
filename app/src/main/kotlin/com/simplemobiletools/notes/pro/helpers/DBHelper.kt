package com.simplemobiletools.notes.pro.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteOpenHelper
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.notes.pro.R
import com.simplemobiletools.notes.pro.models.Note
import com.simplemobiletools.notes.pro.models.Widget
import java.util.*

class DBHelper private constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, DB_NAME, null, DB_VERSION) {
    private val mDb = writableDatabase

    companion object {
        private const val DB_NAME = "notes_old.db"
        private const val DB_VERSION = 4
        private const val NOTES_TABLE_NAME = "notes"
        private const val WIDGETS_TABLE_NAME = "widgets"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_VALUE = "value"
        private const val COL_TYPE = "type"
        private const val COL_PATH = "path"

        private const val COL_WIDGET_ID = "widget_id"
        private const val COL_NOTE_ID = "note_id"

        fun newInstance(context: Context) = DBHelper(context)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $NOTES_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TITLE TEXT UNIQUE, $COL_VALUE TEXT, $COL_TYPE INTEGER DEFAULT 0, $COL_PATH TEXT)")
        db.execSQL("CREATE TABLE $WIDGETS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_WIDGET_ID INTEGER DEFAULT 0, $COL_NOTE_ID INTEGER DEFAULT 0)")
        insertFirstNote(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun insertFirstNote(db: SQLiteDatabase) {
        val generalNote = mContext.resources.getString(R.string.general_note)
        val note = Note(1, generalNote, "", TYPE_NOTE)
        insertNote(note, db)
    }

    private fun insertNote(note: Note, db: SQLiteDatabase) {
        val values = fillNoteContentValues(note)
        db.insert(NOTES_TABLE_NAME, null, values)
    }

    fun insertNote(note: Note): Int {
        val values = fillNoteContentValues(note)
        return mDb.insertWithOnConflict(NOTES_TABLE_NAME, null, values, CONFLICT_IGNORE).toInt()
    }

    fun insertWidget(widget: Widget): Int {
        val values = fillWidgetContentValues(widget)
        return mDb.insertWithOnConflict(WIDGETS_TABLE_NAME, null, values, CONFLICT_IGNORE).toInt()
    }

    private fun fillNoteContentValues(note: Note): ContentValues {
        return ContentValues().apply {
            put(COL_TITLE, note.title)
            put(COL_VALUE, note.value)
            put(COL_PATH, note.path)
            put(COL_TYPE, TYPE_NOTE)
        }
    }

    private fun fillWidgetContentValues(widget: Widget): ContentValues {
        return ContentValues().apply {
            put(COL_WIDGET_ID, widget.widgetId)
            put(COL_NOTE_ID, widget.noteId)
        }
    }

    fun deleteNote(id: Int) {
        mDb.delete(NOTES_TABLE_NAME, "$COL_ID = $id", null)
        mDb.delete(WIDGETS_TABLE_NAME, "$COL_NOTE_ID = $id", null)
    }

    fun doesNoteTitleExist(title: String): Boolean {
        val cols = arrayOf(COL_ID)
        val selection = "$COL_TITLE = ? COLLATE NOCASE"
        val selectionArgs = arrayOf(title)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(NOTES_TABLE_NAME, cols, selection, selectionArgs, null, null, null)
            return cursor.count == 1
        } finally {
            cursor?.close()
        }
    }

    fun getNoteId(path: String): Int {
        val cols = arrayOf(COL_ID)
        val selection = "$COL_PATH = ?"
        val selectionArgs = arrayOf(path)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(NOTES_TABLE_NAME, cols, selection, selectionArgs, null, null, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getIntValue(COL_ID)
            }
        } finally {
            cursor?.close()
        }
        return 0
    }

    fun updateNote(note: Note) {
        val values = fillNoteContentValues(note)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(note.id.toString())
        mDb.update(NOTES_TABLE_NAME, values, selection, selectionArgs)
    }

    fun getWidgets(): ArrayList<Widget> {
        val widgets = ArrayList<Widget>()
        val cols = arrayOf(COL_WIDGET_ID, COL_NOTE_ID)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(WIDGETS_TABLE_NAME, cols, null, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val widgetId = cursor.getIntValue(COL_WIDGET_ID)
                    val noteId = cursor.getIntValue(COL_NOTE_ID)
                    val widget = Widget(0, widgetId, noteId)
                    widgets.add(widget)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        return widgets
    }
}