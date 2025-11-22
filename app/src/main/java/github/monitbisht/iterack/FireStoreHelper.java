package github.monitbisht.iterack;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FireStoreHelper {

    // Firestore instance
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    // TEMPORARY: Dummy User ID (replace later with FirebaseAuth UID)
    private static final String DUMMY_UID = "testUser123";

    // Callback interface for Firestore async responses
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // ADD Task (save inside user's tasks subcollection)
    public static void addTask(Tasks task, FirestoreCallback<Void> cb) {

        db.collection("users")
                .document(DUMMY_UID)
                .collection("tasks")
                .add(task)
                .addOnSuccessListener(doc -> {

                    // Save ID inside model
                    task.setTaskId(doc.getId());

                    // Save ID inside Firestore document
                    doc.update("taskId", doc.getId());

                    cb.onSuccess(null);
                })
                .addOnFailureListener(cb::onError);
    }

    // FETCH all tasks of this user
    public static void getAllTasks(FirestoreCallback<ArrayList<Tasks>> cb) {

        db.collection("users")
                .document(DUMMY_UID)
                .collection("tasks")
                .get()
                .addOnSuccessListener(query -> {

                    ArrayList<Tasks> list = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        Tasks task = doc.toObject(Tasks.class);
                        list.add(task);
                    }

                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // UPDATE a task
    public static void updateTask(Tasks task, FirestoreCallback<Void> cb) {

        db.collection("users")
                .document(DUMMY_UID)
                .collection("tasks")
                .document(task.getTaskId())
                .set(task)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // DELETE a task
    public static void deleteTask(String taskId, FirestoreCallback<Void> cb) {

        db.collection("users")
                .document(DUMMY_UID)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
