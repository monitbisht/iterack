package io.github.monitbisht.iterack;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FireStoreHelper {

    private static FireStoreHelper instance;
    private FirebaseFirestore db;

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }



    private FireStoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static FireStoreHelper getInstance() {
        if (instance == null) {
            instance = new FireStoreHelper();
        }
        return instance;
    }

    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ADD Task
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
                    task.setTaskId(doc.getId());
                    doc.update("taskId", doc.getId());
                    cb.onSuccess(null);
                })
                .addOnFailureListener(cb::onError);
    }

    // GET Tasks
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

    // UPDATE Task
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
                .set(task)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // DELETE Task
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
}
