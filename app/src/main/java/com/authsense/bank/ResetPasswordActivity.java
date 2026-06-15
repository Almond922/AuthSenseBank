package com.authsense.bank;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etNewPassword, etConfirmPassword;
    private Button btnSubmit;
    private String emailFromLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        
        Log.d("AUTH_SENSE", "ResetPasswordActivity opened via link");
        Toast.makeText(this, "Opening Password Reset Page...", Toast.LENGTH_SHORT).show();

        etEmail = findViewById(R.id.et_reset_email);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_new_password);
        btnSubmit = findViewById(R.id.btn_submit_reset);

        // Handle Deep Link or Explicit Intent
        Intent intent = getIntent();
        
        // 1. Try to get email from Intent Extras
        if (intent.hasExtra("email")) {
            emailFromLink = intent.getStringExtra("email");
        } 
        
        // 2. Try to get email from URI data
        Uri data = intent.getData();
        if (emailFromLink == null && data != null) {
            emailFromLink = data.getQueryParameter("email");
        }

        if (emailFromLink != null) {
            etEmail.setText(emailFromLink);
        }

        btnSubmit.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();
            String finalEmail = etEmail.getText().toString().trim().toLowerCase();

            if (newPass.isEmpty() || confirmPass.isEmpty() || finalEmail.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("pass_" + finalEmail, newPass);
            editor.putBoolean("blocked_" + finalEmail, false);
            editor.putBoolean("transaction_blocked", false);
            editor.putBoolean("is_honeypot_active_" + finalEmail, false); // Clear honeypot active state
            editor.apply();

            Toast.makeText(this, "Password reset successful! Please Sign In.", Toast.LENGTH_LONG).show();

            Intent loginIntent = new Intent(this, AuthActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        });
    }
}
