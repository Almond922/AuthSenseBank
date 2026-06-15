package com.authsense.bank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class KycUpdateFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kyc_update, container, false);

        EditText etAddress = view.findViewById(R.id.et_kyc_address);
        EditText etContact = view.findViewById(R.id.et_kyc_contact);
        Button btnSubmit = view.findViewById(R.id.btn_submit_kyc);
        View btnUpload = view.findViewById(R.id.btn_upload_id);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);

        btnUpload.setOnClickListener(v -> {
            if (isHoneypot && getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).logAttackerBehavior("📂 Clicked Upload ID Proof");
            }
            Toast.makeText(getContext(), "Opening File Picker...", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            String contact = etContact.getText().toString().trim();

            if (isHoneypot && getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).logAttackerBehavior("🎯 KYC SUBMIT ATTEMPT: [Address: " + address + ", Contact: " + contact + "]");
            }

            Toast.makeText(getContext(), "KYC Request Submitted! We will notify you after verification.", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
