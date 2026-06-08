package com.authsense.bank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class KycUpdateFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kyc_update, container, false);

        Button btnSubmit = view.findViewById(R.id.btn_submit_kyc);
        View btnUpload = view.findViewById(R.id.btn_upload_id);

        btnUpload.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening File Picker...", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(getContext(), "KYC Request Submitted! We will notify you after verification.", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
