package com.authsense.bank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import com.google.android.material.slider.Slider;
import com.authsense.bank.adapters.CreditCardAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreditCardFragment extends Fragment {

    private CreditCardAdapter adapter;
    private List<CreditCardAdapter.CardData> allCards;
    private TextView tvLimitValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credit_card, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_credit_cards);
        tvLimitValue = view.findViewById(R.id.tv_limit_value);
        Slider slider = view.findViewById(R.id.limit_slider);

        allCards = getCardList();
        adapter = new CreditCardAdapter(requireContext(), new ArrayList<>(allCards));
        
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        slider.addOnChangeListener((s, value, fromUser) -> {
            int selectedLimit = (int) value;
            tvLimitValue.setText("Limit: ₹" + String.format("%, d", selectedLimit).trim());
            filterCards(selectedLimit);
        });

        // Initial filter
        filterCards((int) slider.getValue());

        return view;
    }

    private void filterCards(int selectedLimit) {
        List<CreditCardAdapter.CardData> filtered = new ArrayList<>();
        for (CreditCardAdapter.CardData card : allCards) {
            // Show cards that can support the selected credit limit
            if (card.numericLimit >= selectedLimit) {
                filtered.add(card);
            }
        }
        adapter.updateList(filtered);
    }

    private List<CreditCardAdapter.CardData> getCardList() {
        List<CreditCardAdapter.CardData> cards = new ArrayList<>();

        cards.add(new CreditCardAdapter.CardData(
                "AuthSense Student", "VISA", "NO FEE",
                "₹0/yr", "Up to ₹50,000", 50000,
                "• 2X rewards on food delivery and cafes\n• Free monthly movie tickets via partners\n• Lifetime free card with no annual charges\n• Instant approval with minimal documentation",
                0xFFFFFFFF, ""
        ));

        cards.add(new CreditCardAdapter.CardData(
                "AuthSense Cashback+", "Mastercard", "CASHBACK",
                "₹999/yr", "Up to ₹3 Lakhs", 300000,
                "• 5% flat cashback on major online retailers\n• 2% cashback on all utility bill payments\n• Monthly cashback directly credited to account\n• No reward points, only real cash savings",
                0xFFFFFFFF, ""
        ));

        cards.add(new CreditCardAdapter.CardData(
                "AuthSense Gold", "Mastercard", "POPULAR",
                "₹2,000/yr", "Up to ₹5 Lakhs", 500000,
                "• 3X rewards on groceries and essentials\n• Flat 1% cashback on all online spends\n• Instant EMI conversion at 0% interest rates\n• Fuel surcharge waiver across all stations",
                0xFFFFFFFF, ""
        ));

        cards.add(new CreditCardAdapter.CardData(
                "AuthSense Platinum", "VISA", "PREMIUM",
                "₹5,000/yr", "Up to ₹10 Lakhs", 1000000,
                "• 5X rewards on dining and travel expenditures\n• Unlimited international airport lounge access\n• Zero foreign transaction fees on all currencies\n• Comprehensive travel insurance worth ₹1 Cr",
                0xFFFFFFFF, ""
        ));

        cards.add(new CreditCardAdapter.CardData(
                "AuthSense Signature", "RuPay", "ELITE",
                "₹10,000/yr", "Up to ₹25 Lakhs", 2500000,
                "• 10X rewards on curated luxury brands\n• Dedicated 24/7 personal concierge service\n• Monthly golf course access worldwide\n• Premium hotel benefits and upgrades",
                0xFFFFFFFF, ""
        ));

        return cards;
    }
}