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

public class AddBeneficiaryFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_beneficiary, container, false);

        EditText etAcc = view.findViewById(R.id.et_beneficiary_acc);
        EditText etIfsc = view.findViewById(R.id.et_beneficiary_ifsc);
        Button btnVerify = view.findViewById(R.id.btn_verify_beneficiary);
        LinearLayout llOtp = view.findViewById(R.id.ll_otp_section);
        EditText etOtp = view.findViewById(R.id.et_otp);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_beneficiary);

        btnVerify.setOnClickListener(v -> {
            String acc = etAcc.getText().toString().trim();
            String ifsc = etIfsc.getText().toString().trim();

            if (acc.isEmpty() || ifsc.isEmpty()) {
                Toast.makeText(getContext(), "Please enter account number and IFSC", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Details Verified. Please enter OTP.", Toast.LENGTH_SHORT).show();
                llOtp.setVisibility(View.VISIBLE);
                btnVerify.setEnabled(false);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6) {
                Toast.makeText(getContext(), "Beneficiary added successfully! Activation in 30 mins.", Toast.LENGTH_LONG).show();
                if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
