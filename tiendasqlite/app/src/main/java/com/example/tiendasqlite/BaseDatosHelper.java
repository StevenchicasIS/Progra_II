package com.example.tiendasqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BaseDatosHelper extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "tienda.db";
    private static final int VERSION_BD = 3; // Incrementado a 3 para la actualización

    // Tabla productos
    public static final String TABLA_PRODUCTOS = "productos";
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_CODIGO = "codigo";
    public static final String CAMPO_DESCRIPCION = "descripcion";
    public static final String CAMPO_MARCA = "marca";
    public static final String CAMPO_PRESENTACION = "presentacion";
    public static final String CAMPO_PRECIO = "precio";

    // NUEVAS COLUMNAS PARA SINCRONIZACIÓN CON COUCHDB
    public static final String CAMPO_SINCRONIZADO = "sincronizado";
    public static final String CAMPO_CLOUD_ID = "cloud_id";
    public static final String CAMPO_CLOUD_REV = "cloud_rev";

    // Tabla fotos
    public static final String TABLA_FOTOS = "fotos";
    public static final String CAMPO_FOTO_ID = "foto_id";
    public static final String CAMPO_PRODUCTO_ID = "producto_id";
    public static final String CAMPO_IMAGEN = "imagen";

    public BaseDatosHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla de productos con nuevas columnas
        String crearTablaProductos = "CREATE TABLE " + TABLA_PRODUCTOS + " (" +
                CAMPO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CAMPO_CODIGO + " TEXT UNIQUE, " +
                CAMPO_DESCRIPCION + " TEXT, " +
                CAMPO_MARCA + " TEXT, " +
                CAMPO_PRESENTACION + " TEXT, " +
                CAMPO_PRECIO + " REAL, " +
                CAMPO_SINCRONIZADO + " TEXT DEFAULT 'pendiente', " +
                CAMPO_CLOUD_ID + " TEXT, " +
                CAMPO_CLOUD_REV + " TEXT)";
        db.execSQL(crearTablaProductos);

        // Tabla de fotos
        String crearTablaFotos = "CREATE TABLE " + TABLA_FOTOS + " (" +
                CAMPO_FOTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CAMPO_PRODUCTO_ID + " INTEGER, " +
                CAMPO_IMAGEN + " BLOB, " +
                "FOREIGN KEY(" + CAMPO_PRODUCTO_ID + ") REFERENCES " +
                TABLA_PRODUCTOS + "(" + CAMPO_ID + ") ON DELETE CASCADE)";
        db.execSQL(crearTablaFotos);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Agregar nuevas columnas para sincronización
            try {
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_SINCRONIZADO + " TEXT DEFAULT 'pendiente'");
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_CLOUD_ID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_CLOUD_REV + " TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Si es una versión anterior, recrear tablas
            db.execSQL("DROP TABLE IF EXISTS " + TABLA_FOTOS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLA_PRODUCTOS);
            onCreate(db);
        }
    }
}