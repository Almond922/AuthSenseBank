package com.authsense.bank;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class SignInFragment extends Fragment {
    private static final String TAG = "AUTH_SENSE_LOGIN";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signin, container, false);

        EditText etEmail = view.findViewById(R.id.et_customer_id);
        EditText etPass = view.findViewById(R.id.et_password);
        TextView btnSignIn = view.findViewById(R.id.btn_signin);

        btnSignIn.setOnClickListener(v -> {
            String inputEmail = etEmail.getText().toString().trim().toLowerCase();
            String inputPass = etPass.getText().toString().trim();

            if (inputEmail.isEmpty() || inputPass.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);

            String savedPass = prefs.getString("pass_" + inputEmail, "");
            String oldPass = prefs.getString("old_pass_" + inputEmail, "");
            boolean isHoneypotActive = prefs.getBoolean("is_honeypot_active_" + inputEmail, false);

            if (!savedPass.isEmpty() && inputPass.equals(savedPass)) {
                // REAL LOGIN SUCCESS
                Log.i(TAG, "✅ Real Login Success: " + inputEmail);
                
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("user_email", inputEmail);
                editor.putBoolean("is_honeypot", false); // Explicitly mark as NOT honeypot
                editor.putBoolean("transaction_blocked", false);
                editor.apply();

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if (!oldPass.isEmpty() && inputPass.equals(oldPass)) {
                // HONEYPOT LOGIN (Attacker using old password)
                Log.w(TAG, "🎭 Honeypot Login: Attacker detected using old password for " + inputEmail);
                
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("user_email", inputEmail);
                editor.putBoolean("is_honeypot", true); // Flag session as fake
                editor.apply();

                Intent intent = new Intent(getActivity(), FakeMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Forgot Password Link Handler
        view.findViewById(R.id.tv_forgot).setOnClickListener(v -> {
            String currentEmail = etEmail.getText().toString().trim();
            Intent intent = new Intent(getActivity(), ResetPasswordActivity.class);
            // Passing URI data to trigger the deep link logic in ResetPasswordActivity
            intent.setData(Uri.parse("authsense://reset?email=" + currentEmail));
            startActivity(intent);
        });

        view.findViewById(R.id.tv_go_signup).setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) ((AuthActivity) getActivity()).switchToSignUp();
        });

        return view;
    }
}
