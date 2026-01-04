package io.github.monitbisht.iterack;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportBottomSheet extends BottomSheetDialogFragment {


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottomsheet_export, container, false);

        view.findViewById(R.id.export_pdf).setOnClickListener(v -> handleExport("pdf"));
        view.findViewById(R.id.export_csv).setOnClickListener(v -> handleExport("csv"));
        view.findViewById(R.id.export_json).setOnClickListener(v -> handleExport("json"));
        view.findViewById(R.id.export_txt).setOnClickListener(v -> handleExport("txt"));

        return view;
    }

    private void loadAllTasks(FireStoreHelper.FirestoreCallback<ArrayList<Tasks>> callback) {

        FireStoreHelper.getInstance().getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {

            @Override
            public void onSuccess(ArrayList<Tasks> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
    private void handleExport(String type) {

        loadAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {

            @Override
            public void onSuccess(ArrayList<Tasks> tasks) {

                switch (type) {
                    case "pdf":
                        exportPdf(tasks);
                        break;
                    case "csv":
                        exportCsv(tasks);
                        break;
                    case "json":
                        exportJson(tasks);
                        break;
                    case "txt":
                        exportTxt(tasks);
                        break;
                }

                dismiss();
                Toast.makeText(getContext(),
                        "Exporting as " + type.toUpperCase(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(),
                        "Failed to fetch tasks: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void exportJson(List<Tasks> tasks) {
        try {
            JSONArray arr = new JSONArray();

            for (Tasks t : tasks) {
                JSONObject obj = new JSONObject();
                obj.put("taskId", t.getTaskId());
                obj.put("title", t.getTaskTitle());
                obj.put("description", t.getTaskDescription());
                obj.put("group", t.getTaskGroup());
                obj.put("startDate", formatDate(t.getStartDate()));
                obj.put("endDate", formatDate(t.getEndDate()));
                obj.put("createdOn", formatDate(t.getCreatedOn()));
                obj.put("completionDate", formatDate(t.getCompletionDate()));
                obj.put("status", t.getStatus());
                obj.put("isCompleted", t.isCompleted());

                arr.put(obj);
            }

            File file = new File(requireContext().getExternalFilesDir(null), "tasks.json");

            FileWriter writer = new FileWriter(file);
            writer.write(arr.toString(4));
            writer.close();

            shareFile(file, "application/json");

        } catch (Exception e) {
            Toast.makeText(getContext(), "JSON export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportTxt(List<Tasks> tasks) {
        try {
            File file = new File(requireContext().getExternalFilesDir(null), "tasks.txt");
            FileWriter writer = new FileWriter(file);

            for (Tasks t : tasks) {
                writer.write("Task Title: " + t.getTaskTitle() + "\n");
                writer.write("Description: " + t.getTaskDescription() + "\n");
                writer.write("Group: " + t.getTaskGroup() + "\n");
                writer.write("Start: " + formatDate(t.getStartDate()) + "\n");
                writer.write("End: " + formatDate(t.getEndDate()) + "\n");
                writer.write("Created On: " + formatDate(t.getCreatedOn()) + "\n");
                writer.write("Completed On: " + formatDate(t.getCompletionDate()) + "\n");
                writer.write("Status: " + t.getStatus() + "\n");
                writer.write("Completed: " + t.isCompleted() + "\n");
                writer.write("--------------------------------------------\n\n");
            }

            writer.close();

            shareFile(file, "text/plain");

        } catch (Exception e) {
            Toast.makeText(getContext(), "TXT export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportCsv(List<Tasks> tasks) {
        try {
            File file = new File(requireContext().getExternalFilesDir(null), "tasks.csv");
            FileWriter writer = new FileWriter(file);

            // CSV Header
            writer.write("TaskID,Title,Description,Group,StartDate,EndDate,CreatedOn,CompletionDate,Status,Completed\n");

            for (Tasks t : tasks) {
                writer.write(
                        safe(t.getTaskId()) + "," +
                                quote(t.getTaskTitle()) + "," +
                                quote(t.getTaskDescription()) + "," +
                                safe(t.getTaskGroup()) + "," +
                                safe(formatDate(t.getStartDate())) + "," +
                                safe(formatDate(t.getEndDate())) + "," +
                                safe(formatDate(t.getCreatedOn())) + "," +
                                safe(formatDate(t.getCompletionDate())) + "," +
                                safe(t.getStatus()) + "," +
                                t.isCompleted() + "\n"
                );
            }

            writer.close();

            shareFile(file, "text/csv");

        } catch (Exception e) {
            Toast.makeText(getContext(), "CSV export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPdf(List<Tasks> tasks) {
        try {
            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();

            int pageWidth = 595;  // A4 width
            int pageHeight = 842;
            int y = 60;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            paint.setTextSize(12);

            for (Tasks t : tasks) {

                if (y > pageHeight - 100) {
                    pdf.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 60;
                }

                canvas.drawText("Title: " + t.getTaskTitle(), 40, y, paint); y += 18;
                canvas.drawText("Description: " + t.getTaskDescription(), 40, y, paint); y += 18;
                canvas.drawText("Group: " + t.getTaskGroup(), 40, y, paint); y += 18;
                canvas.drawText("Start: " + formatDate(t.getStartDate()), 40, y, paint); y += 18;
                canvas.drawText("End: " + formatDate(t.getEndDate()),40, y, paint); y += 18;
                canvas.drawText("Created On: " + formatDate(t.getCreatedOn()),40, y, paint); y += 18;
                canvas.drawText("Completed On: " + formatDate(t.getCompletionDate()),40, y, paint); y += 18;
                canvas.drawText("Status: " + t.getStatus(), 40, y, paint); y += 18;
                canvas.drawText("Completed: " + t.isCompleted(), 40, y, paint); y += 28;
            }

            pdf.finishPage(page);

            File file = new File(requireContext().getExternalFilesDir(null), "tasks.pdf");
            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            shareFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(getContext(), "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                "io.github.monitbisht.iterack.fileprovider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Export Tasks"));
    }



    private String quote(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\"", "'") + "\"";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String formatDate(Date date) {
        if (date == null) return "-";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

}
