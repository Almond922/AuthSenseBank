package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class RequestFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        String name = prefs.getString("name_" + userEmail, "Guest");

        TextView tvName = view.findViewById(R.id.tv_user_display_name);
        TextView tvDetails = view.findViewById(R.id.tv_user_acc_details);

        tvName.setText(name);
        // Generate a pseudo-random account number based on email hash
        String accNo = String.valueOf(Math.abs(userEmail.hashCode())).substring(0, 10);
        tvDetails.setText("Acc: " + accNo + " | " + userEmail.split("@")[0] + "@authbank");

        return view;
    }
}