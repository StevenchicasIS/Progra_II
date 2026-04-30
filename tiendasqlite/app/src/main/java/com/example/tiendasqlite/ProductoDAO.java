package com.example.tiendasqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {
    private BaseDatosHelper dbHelper;

    public ProductoDAO(Context context) {
        dbHelper = new BaseDatosHelper(context);
    }

    // ========== PRODUCTOS ==========

    public long insertar(Producto producto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BaseDatosHelper.CAMPO_CODIGO, producto.getCodigo());
        values.put(BaseDatosHelper.CAMPO_DESCRIPCION, producto.getDescripcion());
        values.put(BaseDatosHelper.CAMPO_MARCA, producto.getMarca());
        values.put(BaseDatosHelper.CAMPO_PRESENTACION, producto.getPresentacion());
        values.put(BaseDatosHelper.CAMPO_PRECIO, producto.getPrecio());
        // Por defecto, el producto está pendiente de sincronizar
        values.put(BaseDatosHelper.CAMPO_SINCRONIZADO, "pendiente");

        long id = db.insert(BaseDatosHelper.TABLA_PRODUCTOS, null, values);
        db.close();
        return id;
    }

    public int actualizar(Producto producto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BaseDatosHelper.CAMPO_CODIGO, producto.getCodigo());
        values.put(BaseDatosHelper.CAMPO_DESCRIPCION, producto.getDescripcion());
        values.put(BaseDatosHelper.CAMPO_MARCA, producto.getMarca());
        values.put(BaseDatosHelper.CAMPO_PRESENTACION, producto.getPresentacion());
        values.put(BaseDatosHelper.CAMPO_PRECIO, producto.getPrecio());

        int result = db.update(BaseDatosHelper.TABLA_PRODUCTOS, values,
                BaseDatosHelper.CAMPO_ID + "=?", new String[]{String.valueOf(producto.getId())});
        db.close();
        return result;
    }

    public void eliminar(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Las fotos se eliminan automáticamente por FOREIGN KEY
        db.delete(BaseDatosHelper.TABLA_PRODUCTOS, BaseDatosHelper.CAMPO_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Producto buscarPorCodigo(String codigo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS, null,
                BaseDatosHelper.CAMPO_CODIGO + "=?", new String[]{codigo}, null, null, null);

        Producto producto = null;
        if (cursor.moveToFirst()) {
            producto = cursorToProducto(cursor);
            producto.setFotos(obtenerFotos(producto.getId()));
        }
        cursor.close();
        db.close();
        return producto;
    }

    public List<Producto> buscar(String texto) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String where = BaseDatosHelper.CAMPO_CODIGO + " LIKE ? OR " +
                BaseDatosHelper.CAMPO_DESCRIPCION + " LIKE ?";
        String[] args = {"%" + texto + "%", "%" + texto + "%"};

        Cursor cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS, null, where, args, null, null, null);
        List<Producto> productos = new ArrayList<>();

        while (cursor.moveToNext()) {
            Producto p = cursorToProducto(cursor);
            p.setFotos(obtenerFotos(p.getId()));
            productos.add(p);
        }
        cursor.close();
        db.close();
        return productos;
    }

    public List<Producto> obtenerTodos() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS, null, null, null, null, null, null);
        List<Producto> productos = new ArrayList<>();

        while (cursor.moveToNext()) {
            Producto p = cursorToProducto(cursor);
            p.setFotos(obtenerFotos(p.getId()));
            productos.add(p);
        }
        cursor.close();
        db.close();
        return productos;
    }

    public Producto obtenerPorId(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS, null,
                BaseDatosHelper.CAMPO_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);

        Producto producto = null;
        if (cursor.moveToFirst()) {
            producto = cursorToProducto(cursor);
            producto.setFotos(obtenerFotos(id));
        }
        cursor.close();
        db.close();
        return producto;
    }

    // ========== MÉTODOS PARA SINCRONIZACIÓN ==========

    /**
     * Obtiene el cloud_id de un producto (ID en CouchDB)
     */
    public String getCloudId(int idLocal) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String cloudId = null;
        try {
            cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS,
                    new String[]{BaseDatosHelper.CAMPO_CLOUD_ID},
                    BaseDatosHelper.CAMPO_ID + "=?",
                    new String[]{String.valueOf(idLocal)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                cloudId = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return cloudId;
    }

    /**
     * Obtiene el cloud_rev de un producto (revisión en CouchDB)
     */
    public String getCloudRev(int idLocal) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String cloudRev = null;
        try {
            cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS,
                    new String[]{BaseDatosHelper.CAMPO_CLOUD_REV},
                    BaseDatosHelper.CAMPO_ID + "=?",
                    new String[]{String.valueOf(idLocal)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                cloudRev = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return cloudRev;
    }

    /**
     * Marca un producto como sincronizado
     */
    public void marcarComoSincronizado(int idLocal, String cloudId, String cloudRev) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BaseDatosHelper.CAMPO_SINCRONIZADO, "sincronizado");
        values.put(BaseDatosHelper.CAMPO_CLOUD_ID, cloudId);
        values.put(BaseDatosHelper.CAMPO_CLOUD_REV, cloudRev);

        db.update(BaseDatosHelper.TABLA_PRODUCTOS, values,
                BaseDatosHelper.CAMPO_ID + "=?", new String[]{String.valueOf(idLocal)});
        db.close();
    }

    /**
     * Obtiene todos los productos pendientes de sincronizar
     */
    public List<Producto> obtenerProductosPendientes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BaseDatosHelper.TABLA_PRODUCTOS, null,
                BaseDatosHelper.CAMPO_SINCRONIZADO + "=? OR " +
                        BaseDatosHelper.CAMPO_SINCRONIZADO + " IS NULL",
                new String[]{"pendiente"}, null, null, null);

        List<Producto> productos = new ArrayList<>();
        while (cursor.moveToNext()) {
            Producto p = cursorToProducto(cursor);
            p.setFotos(obtenerFotos(p.getId()));
            productos.add(p);
        }
        cursor.close();
        db.close();
        return productos;
    }

    /**
     * Actualiza el cloud_id y cloud_rev de un producto
     */
    public void actualizarCloudInfo(int idLocal, String cloudId, String cloudRev) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BaseDatosHelper.CAMPO_CLOUD_ID, cloudId);
        values.put(BaseDatosHelper.CAMPO_CLOUD_REV, cloudRev);
        values.put(BaseDatosHelper.CAMPO_SINCRONIZADO, "sincronizado");

        db.update(BaseDatosHelper.TABLA_PRODUCTOS, values,
                BaseDatosHelper.CAMPO_ID + "=?", new String[]{String.valueOf(idLocal)});
        db.close();
    }

    // ========== FOTOS ==========

    public void guardarFotoProducto(int productoId, byte[] imagen) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BaseDatosHelper.CAMPO_PRODUCTO_ID, productoId);
        values.put(BaseDatosHelper.CAMPO_IMAGEN, imagen);
        db.insert(BaseDatosHelper.TABLA_FOTOS, null, values);
        db.close();
    }

    public List<byte[]> obtenerFotos(int productoId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Ordenar por ID ascendente para mantener el orden
        Cursor cursor = db.query(BaseDatosHelper.TABLA_FOTOS, null,
                BaseDatosHelper.CAMPO_PRODUCTO_ID + "=?", new String[]{String.valueOf(productoId)},
                null, null, BaseDatosHelper.CAMPO_FOTO_ID + " ASC");

        List<byte[]> fotos = new ArrayList<>();
        while (cursor.moveToNext()) {
            fotos.add(cursor.getBlob(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_IMAGEN)));
        }
        cursor.close();
        db.close();
        return fotos;
    }

    public void eliminarFoto(int productoId, int posicion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            List<byte[]> fotosActuales = obtenerFotos(productoId);

            if (posicion >= 0 && posicion < fotosActuales.size()) {
                List<byte[]> nuevasFotos = new ArrayList<>();
                for (int i = 0; i < fotosActuales.size(); i++) {
                    if (i != posicion) {
                        nuevasFotos.add(fotosActuales.get(i));
                    }
                }

                db.delete(BaseDatosHelper.TABLA_FOTOS,
                        BaseDatosHelper.CAMPO_PRODUCTO_ID + "=?",
                        new String[]{String.valueOf(productoId)});

                for (byte[] foto : nuevasFotos) {
                    ContentValues values = new ContentValues();
                    values.put(BaseDatosHelper.CAMPO_PRODUCTO_ID, productoId);
                    values.put(BaseDatosHelper.CAMPO_IMAGEN, foto);
                    db.insert(BaseDatosHelper.TABLA_FOTOS, null, values);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public void eliminarTodasLasFotos(int productoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(BaseDatosHelper.TABLA_FOTOS,
                BaseDatosHelper.CAMPO_PRODUCTO_ID + "=?",
                new String[]{String.valueOf(productoId)});
        db.close();
    }

    private Producto cursorToProducto(Cursor cursor) {
        Producto p = new Producto();
        p.setId(cursor.getInt(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_ID)));
        p.setCodigo(cursor.getString(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_CODIGO)));
        p.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_DESCRIPCION)));
        p.setMarca(cursor.getString(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_MARCA)));
        p.setPresentacion(cursor.getString(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_PRESENTACION)));
        p.setPrecio(cursor.getDouble(cursor.getColumnIndexOrThrow(BaseDatosHelper.CAMPO_PRECIO)));
        return p;
    }
}