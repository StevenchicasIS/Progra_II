package com.example.tiendasqlite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import de.hdodenhof.circleimageview.CircleImageView;

public class UtilidadImagenes {

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    public static Bitmap uriToBitmap(Uri uri, android.content.ContentResolver resolver) {
        try {
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                return Bitmap.createScaledBitmap(bitmap, 512, 512, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void cargarImagenRedondeada(CircleImageView imageView, byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                return;
            }
        }
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }
}