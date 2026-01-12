package io.github.monitbisht.iterack;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FireStoreHelper {

    private static FireStoreHelper instance;
    private FirebaseFirestore db;

    // Callback Interface to handle async Firebase results
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }


    // Private Constructor
    private FireStoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // Get Single Instance
    public static FireStoreHelper getInstance() {
        if (instance == null) {
            instance = new FireStoreHelper();
        }
        return instance;
    }

    // Get Current User ID
    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // CRUD OPERATIONS

    // CREATE: Add a new task to Firestore
    public void addTask(Tasks task, FirestoreCallback<Void> cb) {

        String uid = getUid();
        if (uid == null) {
            cb.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .add(task)
                .addOnSuccessListener(doc -> {
                    // Update the task object with the generated ID
                    task.setTaskId(doc.getId());
                    doc.update("taskId", doc.getId());
                    cb.onSuccess(null);
                })
                .addOnFailureListener(cb::onError);
    }

    // READ: Get all tasks for the current user
    public void getAllTasks(FirestoreCallback<ArrayList<Tasks>> cb) {

        String uid = getUid();
        if (uid == null) {
            cb.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(query -> {

                    ArrayList<Tasks> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(doc.toObject(Tasks.class));
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // UPDATE: Modify an existing task
    public void updateTask(Tasks task, FirestoreCallback<Void> cb) {

        String uid = getUid();
        if (uid == null) {
            cb.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.getTaskId())
                .set(task) // Overwrite document
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // DELETE: Remove a task
    public void deleteTask(String taskId, FirestoreCallback<Void> cb) {

        String uid = getUid();
        if (uid == null) {
            cb.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // STATS & ANALYTICS

    // Save weekly productivity stats
    public void saveWeeklyStats(String uid, Map<String, Object> data, FirestoreCallback<Boolean> callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("stats")
                .document("weeklyStats")
                .set(data, SetOptions.merge()) // Merge updates instead of overwriting
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(callback::onError);
    }

    // Retrieve weekly stats
    public void getWeeklyStats(String uid, FirestoreCallback<Map<String, Object>> callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("stats")
                .document("weeklyStats")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(doc.getData());
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    // Helper: Blocking Call for WorkManager (waits for async result)
    public ArrayList<Tasks> getAllTasksBlocking() throws Exception {
        String uid = getUid();
        if (uid == null) throw new Exception("User not logged in");

        ArrayList<Tasks> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(doc.toObject(Tasks.class));
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    latch.countDown();
                });

        latch.await(10, TimeUnit.SECONDS); // Timeout after 10s
        return list;
    }

}