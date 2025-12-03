package io.github.monitbisht.iterack;

import android.util.Log;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple helper that calls the Firebase AI generative model using
 * the Java compatibility futures wrapper. Adjust model id as needed.
 */
public class AiHelper {

    private static final String TAG = "AiHelper";

    private final GenerativeModelFutures model;
    private final ExecutorService executor;

    public AiHelper() {
        // Change model id if you want a different one
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        model = GenerativeModelFutures.from(ai);
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Send weekly JSON and ask the model to return a strict JSON object.
     * Result forwarded to the provided callback.
     */
    public void generateWeeklySummary(String jsonData, InsightFragment.AiCallback callback) {
        if (jsonData == null) jsonData = "{}";

        String promptText =
                "You are an AI productivity analyst. Analyze ONLY the JSON below:\n\n"
                        + jsonData + "\n\n"

                        + "Use these rules:\n"
                        + "- current_week_* fields describe THIS week's activity.\n"
                        + "- previous_week_* fields describe LAST week's activity.\n"
                        + "- Identify weekday activity trends based on daily counts.\n"
                        + "- Use streak.current_streak and streak.longest_streak to describe consistency.\n\n"

                        + "Weekly Summary MUST follow this structure:\n"
                        + "1. Start with THIS week's activity.\n"
                        + "2. Then compare with LAST week, mentioning partial previous week if applicable.\n"
                        + "3. Then highlight one improvement area in a motivating tone.\n\n"

                        + "Return ONLY a JSON object with EXACT keys:\n"
                        + "1. productivity_score (0–100)\n"
                        + "2. productivity_tip (2–3 words)\n"
                        + "3. weekly_summary (2–3 sentences, following the structure above)\n"
                        + "4. weekly_tip (3–6 bullet points, each starting with '•', all inside ONE string)\n"
                        + "5. conclusion (short motivating line)\n\n"

                        + "Rules:\n"
                        + "- NEVER add text outside the JSON.\n"
                        + "- NEVER invent numbers.\n"
                        + "- weekly_tip MUST be a single string with newline-separated bullet points.\n"
                        + "- Keep tone motivating but honest about areas to improve.\n";



        Content prompt = new Content.Builder()
                .addText(promptText)
                .build();

        try {
            ListenableFuture<GenerateContentResponse> future = model.generateContent(prompt);

            Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String text = "";
                    try {
                        text = result.getText();
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to read model text", ex);
                    }

                    // Defensive: if model returned plain text with JSON embedded, try to extract JSON substring
                    String jsonOut = extractJsonSubstring(text);
                    if (jsonOut == null) jsonOut = text;

                    Log.d(TAG, "AI raw output: " + text);
                    callback.onResult(jsonOut);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "AI call failed", t);
                    callback.onError(t.toString());
                }
            }, executor);

        } catch (Exception e) {
            Log.e(TAG, "generateWeeklySummary exception", e);
            callback.onError(e.toString());
        }
    }

    // Attempt to extract the first {...} block from arbitrary text.
    private String extractJsonSubstring(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return null;
    }
}
