package com.example.gmailnotifier;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.MessagePart;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailViewActivity extends AppCompatActivity {
    private static final String TAG = "EmailViewActivity";

    private TextView subjectTextView;
    private TextView fromTextView;
    private WebView contentWebView;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_view);

        subjectTextView = findViewById(R.id.email_subject);
        fromTextView = findViewById(R.id.email_from);
        contentWebView = findViewById(R.id.email_content);

        executorService = Executors.newSingleThreadExecutor();

        String messageId = getIntent().getStringExtra("MESSAGE_ID");
        if (messageId != null) {
            loadEmail(messageId);
        }
    }

    private void loadEmail(String messageId) {
        executorService.execute(() -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                if (account == null) return;

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                this,
                                Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly"))
                        .setBackOff(new ExponentialBackOff());
                credential.setSelectedAccount(account.getAccount());

                Gmail gmailService = new Gmail.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential)
                        .setApplicationName("Gmail Notifier")
                        .build();

                // Get the full message
                Message message = gmailService.users().messages().get("me", messageId).execute();

                String subject = "";
                String from = "";

                // Extract headers
                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                    if ("Subject".equals(header.getName())) {
                        subject = header.getValue();
                    } else if ("From".equals(header.getName())) {
                        from = header.getValue();
                    }
                }

                // Extract content
                String content = extractContent(message.getPayload());

                final String finalSubject = subject;
                final String finalFrom = from;
                final String finalContent = content;

                runOnUiThread(() -> {
                    subjectTextView.setText(finalSubject);
                    fromTextView.setText(finalFrom);
                    contentWebView.loadData(finalContent, "text/html", "UTF-8");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading email", e);
            }
        });
    }

    private String extractContent(MessagePart payload) {
        if (payload.getParts() == null) {
            // This is a single part message
            if (payload.getBody().getData() != null) {
                return new String(Base64.decodeBase64(payload.getBody().getData()));
            }
            return "";
        }

        // This is a multipart message
        StringBuilder contentBuilder = new StringBuilder();
        for (MessagePart part : payload.getParts()) {
            if ("text/html".equals(part.getMimeType()) && part.getBody().getData() != null) {
                return new String(Base64.decodeBase64(part.getBody().getData()));
            } else if ("text/plain".equals(part.getMimeType()) && part.getBody().getData() != null) {
                contentBuilder.append(new String(Base64.decodeBase64(part.getBody().getData())));
            } else if (part.getParts() != null) {
                // Recursive call for nested parts
                contentBuilder.append(extractContent(part));
            }
        }

        return contentBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}