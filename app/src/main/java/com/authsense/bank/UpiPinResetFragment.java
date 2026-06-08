package com.authsense.bank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class UpiPinResetFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upi_pin_reset, container, false);

        EditText etLastDigits = view.findViewById(R.id.et_last_digits);
        EditText etExpiry = view.findViewById(R.id.et_expiry);
        EditText etCvv = view.findViewById(R.id.et_cvv);
        Button btnVerify = view.findViewById(R.id.btn_verify_card);

        LinearLayout llPinSection = view.findViewById(R.id.ll_pin_section);
        EditText etNewPin = view.findViewById(R.id.et_new_upi_pin);
        EditText etConfirmPin = view.findViewById(R.id.et_confirm_upi_pin);
        Button btnSetPin = view.findViewById(R.id.btn_set_pin);

        btnVerify.setOnClickListener(v -> {
            String digits = etLastDigits.getText().toString();
            String expiry = etExpiry.getText().toString();
            String cvv = etCvv.getText().toString();

            if (digits.length() == 6 && !expiry.isEmpty() && cvv.length() == 3) {
                Toast.makeText(getContext(), "Card Verified! Please set your new UPI PIN.", Toast.LENGTH_SHORT).show();
                llPinSection.setVisibility(View.VISIBLE);
                btnVerify.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Please enter valid card details", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetPin.setOnClickListener(v -> {
            String pin = etNewPin.getText().toString();
            String confirm = etConfirmPin.getText().toString();

            if (pin.length() == 6 && pin.equals(confirm)) {
                Toast.makeText(getContext(), "UPI PIN Reset Successful!", Toast.LENGTH_LONG).show();
                if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "PINs do not match or are invalid", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
