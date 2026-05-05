package com.authsense.bank;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class AuthActivity extends AppCompatActivity {

    private TextView tabSignIn, tabSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        tabSignIn = findViewById(R.id.tab_signin);
        tabSignUp = findViewById(R.id.tab_signup);

        // Default: show Sign In
        loadFragment(new SignInFragment());
        setActiveTab(tabSignIn, tabSignUp);

        tabSignIn.setOnClickListener(v -> {
            loadFragment(new SignInFragment());
            setActiveTab(tabSignIn, tabSignUp);
        });

        tabSignUp.setOnClickListener(v -> {
            loadFragment(new SignUpFragment());
            setActiveTab(tabSignUp, tabSignIn);
        });
    }

    public void switchToSignUp() {
        loadFragment(new SignUpFragment());
        setActiveTab(tabSignUp, tabSignIn);
    }

    public void switchToSignIn() {
        loadFragment(new SignInFragment());
        setActiveTab(tabSignIn, tabSignUp);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.auth_container, fragment)
                .commit();
    }

    private void setActiveTab(TextView active, TextView inactive) {
        active.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
        active.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent));
        inactive.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
        inactive.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.divider));
    }
}