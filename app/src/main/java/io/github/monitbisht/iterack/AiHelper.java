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
 * Simple helper that calls the Firebase AI generative model.
 */
public class AiHelper {

    private static final String TAG = "AiHelper";

    private final GenerativeModelFutures model;
    private final ExecutorService executor;

    public AiHelper() {
        // Initialize Firebase AI Model (Gemini 2.5 Flash)
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        model = GenerativeModelFutures.from(ai);
        executor = Executors.newSingleThreadExecutor();
    }

    // Generates a weekly productivity summary using the provided JSON data
    public void generateWeeklySummary(String jsonData, InsightFragment.AiCallback callback) {
        if (jsonData == null) jsonData = "{}";

        // Construct the strict Prompt for the AI
        // Rules enforce specific JSON keys and analysis logic for week-over-week comparison
        String promptText =
                "You are an AI productivity analyst. Analyze ONLY the JSON below:\n\n"
                        + jsonData + "\n\n"

                        + "GLOBAL ZERO-DATA RULE (HIGHEST PRIORITY):\n"
                        + "- If total completed tasks = 0 AND total missed tasks = 0:\n"
                        + "  • Set productivity_score to EXACTLY 0.\n"
                        + "  • Do NOT generate analytical summaries or comparisons.\n"
                        + "  • Weekly summary must be a short onboarding message (2–3 sentences).\n"
                        + "  • Generate EXACTLY 4 generic Smart Tips.\n\n"

                        + "Context awareness rules:\n"
                        + "- Adjust tone silently for new users.\n"
                        + "- If total completed tasks ≤ 2, avoid comparisons and statistics.\n"
                        + "- Full week-to-week comparison is allowed only when data is sufficient.\n"
                        + "- Treat the week as starting on Sunday.\n"
                        + "- Avoid strict week-over-week productivity comparisons in the early days of a new week (Sunday–Tuesday).\n"
                        + "- Allow firm comparisons only toward the end of the week (Friday or Saturday).\n\n"


                        + "Data interpretation rules:\n"
                        + "- current_week_* fields describe THIS week's activity.\n"
                        + "- previous_week_* fields describe LAST week's activity.\n"
                        + "- NEVER invent numbers, trends, or percentages.\n\n"

                        + "Weekly Summary rules:\n"
                        + "- If zero-data rule applies, write a welcoming onboarding summary.\n"
                        + "- Otherwise, write ONE paragraph of 4–5 sentences.\n"
                        + "- Do NOT mention missed tasks when completed tasks ≤ 2.\n"
                        + "- Include at most ONE statistic only if meaningful.\n"
                        + "- Do NOT repeat Smart Tips in the summary.\n"
                        + "- Keep tone calm and neutral.\n\n"

                        + "Smart Tips rules:\n"
                        + "- Always show 4–5 bullet points.\n"
                        + "- Prefer PERSONAL tips when data exists.\n"
                        + "- If personal tips < 4, fill remaining with GENERIC tips.\n"
                        + "- If zero-data rule applies, use ONLY generic tips.\n"
                        + "- NEVER show more than one problem-diagnosis tip.\n\n"

                        + "GENERIC Smart Tips (use these for zero or low data):\n"
                        + "• Set one small, achievable task for tomorrow.\n"
                        + "• Break larger goals into tiny, manageable steps.\n"
                        + "• Use short focus sessions (like Pomodoro) to get started.\n"
                        + "• Review your tasks each morning to set a clear focus.\n\n"

                        + "Allowed PERSONAL Smart Tips (use only when data supports it):\n"
                        + "• You often miss Health tasks. Break them into 10–15 minute steps.\n"
                        + "• You’re more consistent on weekdays. Plan important tasks there.\n"
                        + "• Most tasks are completed on a few days. Spreading them out may reduce pressure.\n"
                        + "• Your task load may be too ambitious. Reducing daily tasks could improve completion.\n\n"

                        + "Return ONLY a JSON object with EXACT keys:\n"
                        + "1. productivity_score (0–100)\n"
                        + "2. productivity_tip (2–3 words)\n"
                        + "3. weekly_summary\n"
                        + "4. weekly_tip (4–5 bullet points, ONE string, each starting with '•')\n"
                        + "5. conclusion (short, neutral line)\n\n"

                        + "Final rules:\n"
                        + "- NEVER add text outside the JSON.\n"
                        + "- NEVER use emojis.\n"
                        + "- Avoid stating obvious or empty facts.\n";


        Content prompt = new Content.Builder()
                .addText(promptText)
                .build();

        try {
            // Call the AI model asynchronously
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

                    // Extract JSON from the raw text response
                    // (Removes any unwanted text the AI might send before or after the data)
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

    // Helper Method: Finds the actual JSON data inside the text response
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