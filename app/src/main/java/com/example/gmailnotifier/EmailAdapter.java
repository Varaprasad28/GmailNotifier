package com.example.gmailnotifier;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {
    private List<EmailItem> emailList;
    private EmailClickListener listener;

    public interface EmailClickListener {
        void onEmailClicked(EmailItem email);
    }

    public EmailAdapter(List<EmailItem> emailList, EmailClickListener listener) {
        this.emailList = emailList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        EmailItem email = emailList.get(position);
        holder.subjectText.setText(email.getSubject());
        holder.senderText.setText(email.getSender());
        holder.dateText.setText(email.getDate());
        holder.snippetText.setText(email.getSnippet());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmailClicked(email);
            }
        });
    }

    @Override
    public int getItemCount() {
        return emailList.size();
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView subjectText;
        TextView senderText;
        TextView dateText;
        TextView snippetText;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectText = itemView.findViewById(R.id.text_subject);
            senderText = itemView.findViewById(R.id.text_sender);
            dateText = itemView.findViewById(R.id.text_date);
            snippetText = itemView.findViewById(R.id.text_snippet);
        }
    }
}