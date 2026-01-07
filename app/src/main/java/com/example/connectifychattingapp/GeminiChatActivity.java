package com.example.connectifychattingapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeminiChatActivity extends AppCompatActivity {

    // Request Codes
    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 101;
    private static final int REQ_MIC = 102;
    private GenerativeModelFutures model;
    private List<ChatMessage> chatList = new ArrayList<>();
    private GeminiChatAdapter adapter;
    private EditText geminiPrompt;
    private Bitmap selectedBitmap = null;

    // UI Elements
    private RelativeLayout previewContainer;
    private ImageView imagePreview, btnRemoveImage;
    private ImageView btnCamera, btnGallery, btnMic, btnSend, back;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini_chat);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadChatData();

        // 1. Initialize Gemini Model
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY, new GenerationConfig.Builder().build());
        model = GenerativeModelFutures.from(gm);

        // 2. Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.chatRecyclerView);
        adapter = new GeminiChatAdapter(chatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 3. Bind UI Components
        geminiPrompt = findViewById(R.id.GeminiPrompt);
        previewContainer = findViewById(R.id.previewContainer);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        btnCamera = findViewById(R.id.camera);
        btnGallery = findViewById(R.id.media);
        btnMic = findViewById(R.id.mic);
        btnSend = findViewById(R.id.sendPrompt);
        back=findViewById(R.id.back);
        progressBar = findViewById(R.id.geminiProgressBar);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(GeminiChatActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // --- BUTTON CLICK LISTENERS ---
        // MIC BUTTON
        btnMic.setOnClickListener(v -> startVoiceInput());

        // CAMERA BUTTON
        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            }
        });

        // GALLERY BUTTON
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_GALLERY);
        });

        // REMOVE IMAGE PREVIEW
        btnRemoveImage.setOnClickListener(v -> {
            selectedBitmap = null;
            previewContainer.setVisibility(View.GONE);
        });

        // SEND BUTTON
        btnSend.setOnClickListener(v -> {
            String text = geminiPrompt.getText().toString().trim();
            if (!text.isEmpty() || selectedBitmap != null) {
                chatList.add(new ChatMessage(text, true));
                adapter.notifyItemInserted(chatList.size() - 1);
                recyclerView.scrollToPosition(chatList.size() - 1);
                sendMessageToGemini(text, selectedBitmap);
                // Clear input after sending
                geminiPrompt.setText("");
                hideKeyboard();
                // 3. Scroll to bottom
                recyclerView.scrollToPosition(chatList.size() - 1);
                selectedBitmap = null;
                previewContainer.setVisibility(View.GONE);
            }
        });
    }
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help you?");
        try {
            startActivityForResult(intent, REQ_MIC);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "Camera app not found or permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendMessageToGemini(String text, Bitmap image) {
        progressBar.setVisibility(View.VISIBLE);
        Content.Builder contentBuilder = new Content.Builder();
        if (!text.isEmpty()) contentBuilder.addText(text);
        if (image != null) contentBuilder.addImage(image);

        ListenableFuture<GenerateContentResponse> response = model.generateContent(contentBuilder.build());
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String reply = result.getText();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                                    chatList.add(new ChatMessage(reply, false));
                    adapter.notifyItemInserted(chatList.size() - 1);
                });
            }
            @Override public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GeminiChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQ_CAMERA) {
                selectedBitmap = (Bitmap) data.getExtras().get("data");
                showImagePreview();
            } else if (requestCode == REQ_GALLERY) {
                Uri uri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    showImagePreview();
                } catch (IOException e) { e.printStackTrace(); }
            } else if (requestCode == REQ_MIC) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    geminiPrompt.setText(result.get(0));
                }
            }
        }
    }
    private void showImagePreview() {
        if (selectedBitmap != null) {
            imagePreview.setImageBitmap(selectedBitmap);
            previewContainer.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.gemini_chat_clear, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clearGeminiChat) {
            showClearChatDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void saveChatData() {
        SharedPreferences sharedPreferences = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(chatList);
        editor.putString("chat_history", json);
        editor.apply();
    }
    private void loadChatData() {
        SharedPreferences sharedPreferences = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("chat_history", null);
        Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
        chatList = gson.fromJson(json, type);

        if (chatList == null) {
            chatList = new ArrayList<>();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        saveChatData(); // Save chat when user leaves the activity
    }
    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to delete this chat")
                .setPositiveButton("Delete", (dialog, which) -> {
                    chatList.clear();
                    adapter.notifyDataSetChanged();
                    saveChatData(); // Update local storage
                    Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}