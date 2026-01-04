package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetDataManager {

    private final FirebaseFirestore db;
    private final String uid;
    private final Context context;

    public ResetDataManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getUid();
    }

    public interface ResetCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void resetAllData(ResetCallback callback) {

        if (uid == null) {
            callback.onFailure(new Exception("User not logged in"));
            return;
        }

        deleteTasks(new ResetCallback() {
            @Override
            public void onSuccess() {
                deleteWeeklyStats(new ResetCallback() {
                    @Override
                    public void onSuccess() {
                        clearLocalPreferences();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }


    // Delete tasks subcollection

    private void deleteTasks(ResetCallback callback) {

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    db.runBatch(batch -> {
                                for (var doc : query.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                            })
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);

                })
                .addOnFailureListener(callback::onFailure);
    }



    // Delete weeklyStats subcollection

    private void deleteWeeklyStats(ResetCallback callback) {

        db.collection("users")
                .document(uid)
                .collection("weeklyStats")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    db.runBatch(batch -> {
                                for (var doc : query.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                            })
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);

                })
                .addOnFailureListener(callback::onFailure);
    }




    // Clear SharedPreferences (cache, lock, insights)

    private void clearLocalPreferences() {

        SharedPreferences prefs;

        prefs = context.getSharedPreferences("TASK_CACHE", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        prefs = context.getSharedPreferences("APP_LOCK_PREFS_" + uid, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        prefs = context.getSharedPreferences("INSIGHTS_CACHE_" + uid, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        prefs = context.getSharedPreferences("WEEKLY_STATS_CACHE_" + uid, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Log.d("ResetAllData", "Local preferences cleared");
    }
}
