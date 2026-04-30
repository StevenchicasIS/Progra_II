package com.example.tiendasqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BaseDatosHelper extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "tienda.db";
    private static final int VERSION_BD = 4; // Incrementar a 4

    // Tabla productos
    public static final String TABLA_PRODUCTOS = "productos";
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_CODIGO = "codigo";
    public static final String CAMPO_DESCRIPCION = "descripcion";
    public static final String CAMPO_MARCA = "marca";
    public static final String CAMPO_PRESENTACION = "presentacion";
    public static final String CAMPO_PRECIO = "precio";

    // NUEVOS CAMPOS
    public static final String CAMPO_COSTO = "costo";
    public static final String CAMPO_GANANCIA = "ganancia";      // Porcentaje de ganancia
    public static final String CAMPO_STOCK = "stock";

    // Campos para sincronización
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
        String crearTablaProductos = "CREATE TABLE " + TABLA_PRODUCTOS + " (" +
                CAMPO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CAMPO_CODIGO + " TEXT UNIQUE, " +
                CAMPO_DESCRIPCION + " TEXT, " +
                CAMPO_MARCA + " TEXT, " +
                CAMPO_PRESENTACION + " TEXT, " +
                CAMPO_PRECIO + " REAL, " +
                CAMPO_COSTO + " REAL DEFAULT 0, " +
                CAMPO_GANANCIA + " REAL DEFAULT 0, " +
                CAMPO_STOCK + " INTEGER DEFAULT 0, " +
                CAMPO_SINCRONIZADO + " TEXT DEFAULT 'pendiente', " +
                CAMPO_CLOUD_ID + " TEXT, " +
                CAMPO_CLOUD_REV + " TEXT)";
        db.execSQL(crearTablaProductos);

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
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_COSTO + " REAL DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_GANANCIA + " REAL DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLA_PRODUCTOS + " ADD COLUMN " + CAMPO_STOCK + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}