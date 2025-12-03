package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;


import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;
import io.github.monitbisht.iterack.R;


public class ProfileFragment extends Fragment {

    private TextView profileName, profileEmail;
    private CircleImageView profileImage;
    private MaterialButton logoutButton;

    private ImageView editProfileIcon;

    private String currentUserPhotoUrl;
    private String currentUserName;
    private String currentUserEmail;



    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        profileImage = view.findViewById(R.id.profileImage);
        logoutButton = view.findViewById(R.id.logout_button);
        editProfileIcon = view.findViewById(R.id.profile_edit_button);

        return view;

    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //Todo
            }
        });


        loadUserData();
        signOut();


    }

    private void signOut() {

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                // 2. Clear shared preferences (if user data is cached)
                SharedPreferences prefs = requireActivity().getSharedPreferences("userData", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();

                // 3. Redirect to LoginActivity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                requireActivity().finish();

            }
        }
        );

    }

    private void loadUserData() {

        SharedPreferences prefs = requireContext().getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        String cachedUid = prefs.getString("uid", null);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // 1. If UID changed -> clear old user cache
        if (cachedUid != null && !cachedUid.equals(uid)) {
            prefs.edit().clear().apply();
        }

        // Load cached data
        String cachedName = prefs.getString("name", null);
        String cachedPhoto = prefs.getString("photoUrl", null);
        String cachedEmail = prefs.getString("email", null);


        if (cachedName != null) {
            currentUserName = cachedName;     // ← ADD THIS
            profileName.setText(cachedName);
        }

        if (cachedEmail != null) {
            currentUserEmail = cachedEmail;   // ← ADD THIS
            profileEmail.setText(cachedEmail);
        }

        if (cachedPhoto != null) {
            currentUserPhotoUrl = cachedPhoto;   // ← ADD THIS
            Glide.with(requireContext())
                    .load(cachedPhoto)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
        }


        // 2. Fetch the fresh data from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String name = doc.getString("name");
                        String photoUrl = doc.getString("photoUrl");
                        String email = doc.getString("email");
                        String updatedPic = doc.getString("updatedPic");
                        String activePic = doc.getString("activePic");

                        // Update UI
                        if (name != null) {
                            currentUserName = name;
                            profileName.setText(name);
                        }

                        if (email != null) {
                            currentUserEmail = email;
                            profileEmail.setText(email);
                        }

                        if (photoUrl != null) {
                            Glide.with(requireContext())
                                    .load(photoUrl)
                                    .placeholder(R.drawable.profile)
                                    .into(profileImage);
                        }

                        // 3. Save new data + UID in cache
                        prefs.edit()
                                .putString("uid", uid)
                                .putString("name", name)
                                .putString("photoUrl", photoUrl)
                                .putString("email", email)
                                .apply();
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("HomeFragment", "Failed to fetch user data: " + e.getMessage())
                );
    }

}