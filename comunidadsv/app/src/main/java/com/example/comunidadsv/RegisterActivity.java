package com.example.comunidadsv;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPassword;
    private Button btnRegister;
    private TextView txtLogin;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Referencias
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        // Mostrar/ocultar contraseña
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Ocultar contraseña
                edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view); // Icono ojo cerrado
                isPasswordVisible = false;
            } else {
                // Mostrar contraseña
                edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_camera); // Icono ojo abierto
                isPasswordVisible = true;
            }
            // Mover el cursor al final del texto
            edtPassword.setSelection(edtPassword.getText().length());
        });

        // Registrarse
        btnRegister.setOnClickListener(v -> {
            if (validarCampos()) {
                // Aquí puedes guardar el usuario (SharedPreferences, Firebase, SQLite, etc.)
                // Por ahora solo navegamos al FeedActivity
                Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, FeedActivity.class);
                startActivity(intent);
                finish(); // Opcional: cerrar esta actividad para que no vuelva atrás
            }
        });

        // Ir a Login
        txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private boolean validarCampos() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtName.setError("El nombre es obligatorio");
            edtName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("El correo es obligatorio");
            edtEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Ingrese un correo válido");
            edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("La contraseña es obligatoria");
            edtPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            edtPassword.setError("La contraseña debe tener al menos 6 caracteres");
            edtPassword.requestFocus();
            return false;
        }

        return true;
    }
}