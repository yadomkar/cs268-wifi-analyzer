package com.example.cs268.wifianalyzer;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Load extends AppCompatActivity {

    private TextView loadedInfo;
    private TextView savedFiles;
    private static String fileName = "record001";
    private String filePath;
    private final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        // Initialize views
        loadedInfo = findViewById(R.id.loadedInfo);
        savedFiles = findViewById(R.id.savedFiles);
        filePath = getExternalFilesDir("Wifi Strength Results").toString();

        // Generate a list of saved files
        generateSavedFileList();
    }

    private void generateSavedFileList() {
        File directory = new File(filePath + "/");
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder("LIST OF SAVED FILES: \n\n");
            for (File file : files) {
                fileList.append(file.getName()).append("\n");
            }
            savedFiles.setText(fileList.toString());
        } else {
            savedFiles.setText("Saved files will be shown here...");
        }
    }

    public void load(View view) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptsView = layoutInflater.inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        // Initialize input field for the file name
        final EditText userInput = promptsView.findViewById(R.id.editTextDialogUserInput);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    fileName = userInput.getText().toString();
                    String absolutePath = filePath + "/" + fileName;

                    try (FileInputStream fis = new FileInputStream(absolutePath);
                         DataInputStream dataInputStream = new DataInputStream(fis);
                         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream))) {

                        StringBuilder fileContent = new StringBuilder(fileName + "\n\n");
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            fileContent.append(line).append("\n");
                        }
                        loadedInfo.setText(fileContent.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void back(View view) {
        // Use the default back navigation
        super.onBackPressed();
    }
}
