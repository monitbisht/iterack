package io.github.monitbisht.iterack;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class AppLockEnableFragment extends Fragment {

    private AppCompatButton turnOnButton;
    private TextView maybeLaterText;

    public AppLockEnableFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate UI layout
        View view = inflater.inflate(R.layout.fragment_app_lock_enable, container, false);
        turnOnButton = view.findViewById(R.id.turn_on_app_lock_button);
        maybeLaterText = view.findViewById(R.id.maybe_later_textView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // User chose to Enable -> Go to Unlock Method Selection screen
        turnOnButton.setOnClickListener(v -> {
            FragmentManager manager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.frame_layout, new UnlockMethodsFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // User chose Later -> Return to Profile screen
        maybeLaterText.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new ProfileFragment())
                    .commit();
        });

    }
}