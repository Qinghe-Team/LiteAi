package com.qinghe.liteai.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.qinghe.liteai.R;
import com.qinghe.liteai.model.Conversation;
import com.qinghe.liteai.util.DateTimeUtils;

import java.util.List;

public class ConversationAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private List<Conversation> items;

    public ConversationAdapter(Context context, List<Conversation> items) {
        this.inflater = LayoutInflater.from(context);
        this.items = items;
    }

    public void submit(List<Conversation> conversations) {
        this.items = conversations;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Conversation getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_conversation, parent, false);
        Conversation conversation = getItem(position);
        ((TextView) view.findViewById(R.id.conversation_title)).setText(conversation.getTitle());
        ((TextView) view.findViewById(R.id.conversation_time)).setText(parent.getContext().getString(R.string.label_last_message_time, DateTimeUtils.formatDisplayTime(conversation.getLastMessageAt())));
        return view;
    }
}
