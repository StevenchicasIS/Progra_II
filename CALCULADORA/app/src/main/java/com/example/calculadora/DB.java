package com.example.calculadora;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class DB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "amigos";
    private static final int DATABASE_VERSION = 1;
    private static final String SQLdb = "CREATE TABLE amigos (idAmigo INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, direccion TEXT, telefono TEXT, email TEXT, dui TEXT, urlFoto TEXT)";

    // CORRECCIÓN 1: Constructor simplificado
    public DB(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQLdb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Para actualizar la base de datos
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS amigos");
        onCreate(sqLiteDatabase);
    }

    public String administrar_amigos(String accion, String[] datos) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            String mensaje = "ok";
            String sql = "";

            switch (accion) {
                case "nuevo":
                    // CORRECCIÓN 2: Índices del array (empiezan en 0)
                    sql = "INSERT INTO amigos(nombre, direccion, telefono, email, dui, urlFoto) VALUES(" +
                            "'" + datos[0] + "'," +  // nombre
                            "'" + datos[1] + "'," +  // direccion
                            "'" + datos[2] + "'," +  // telefono
                            "'" + datos[3] + "'," +  // email
                            "'" + datos[4] + "'," +  // dui
                            "'" + datos[5] + "')";   // urlFoto
                    break;

                case "modificar":
                    sql = "UPDATE amigos SET " +
                            "nombre='" + datos[1] + "'," +
                            "direccion='" + datos[2] + "'," +
                            "telefono='" + datos[3] + "'," +
                            "email='" + datos[4] + "'," +
                            "dui='" + datos[5] + "'," +
                            "urlFoto='" + datos[6] + "' " +
                            "WHERE idAmigo='" + datos[0] + "'";
                    break;

                case "eliminar":
                    sql = "DELETE FROM amigos WHERE idAmigo='" + datos[0] + "'";
                    break;
            }

            if (!sql.isEmpty()) {
                db.execSQL(sql);
            }

            return mensaje;

        } catch (Exception e) {
            return e.getMessage();
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    public Cursor lista_amigos() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM amigos", null);
    }
}