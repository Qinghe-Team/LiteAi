package com.qinghe.liteai.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.qinghe.liteai.R;
import com.qinghe.liteai.model.AiModelConfig;

import java.util.List;

public class ModelAdapter extends BaseAdapter {
    public interface SelectionListener {
        void onModelSelected(AiModelConfig model);
    }

    public interface EditListener {
        void onEditRequested(AiModelConfig model);
    }

    public interface DeleteListener {
        void onDeleteRequested(AiModelConfig model);
    }

    public interface Listener extends SelectionListener, EditListener, DeleteListener {
    }

    private final LayoutInflater inflater;
    private final SelectionListener selectionListener;
    private final EditListener editListener;
    private final DeleteListener deleteListener;
    private List<AiModelConfig> items;

    public ModelAdapter(Context context, List<AiModelConfig> items, SelectionListener selectionListener, EditListener editListener, DeleteListener deleteListener) {
        this.inflater = LayoutInflater.from(context);
        this.items = items;
        this.selectionListener = selectionListener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public ModelAdapter(Context context, List<AiModelConfig> items, Listener listener) {
        this(context, items, listener, listener, listener);
    }

    public ModelAdapter(Context context, List<AiModelConfig> items, SelectionListener selectionListener, EditListener editListener) {
        this(context, items, selectionListener, editListener, null);
    }

    public void submit(List<AiModelConfig> models) {
        this.items = models;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public AiModelConfig getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_model, parent, false);
        Context context = parent.getContext();
        AiModelConfig model = getItem(position);
        view.setOnClickListener(v -> {
            if (selectionListener != null) {
                selectionListener.onModelSelected(model);
            }
        });
        view.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteRequested(model);
                return true;
            }
            return false;
        });
        ((TextView) view.findViewById(R.id.model_name)).setText(model.getName());
        ((TextView) view.findViewById(R.id.model_mode)).setText(context.getString(R.string.label_api_mode, model.getApiMode()));
        ((TextView) view.findViewById(R.id.model_url)).setText(context.getString(R.string.label_api_url, model.getApiUrl()));
        ((TextView) view.findViewById(R.id.model_code)).setText(context.getString(R.string.label_model_code, model.getModelCode()));
        view.findViewById(R.id.button_edit).setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditRequested(model);
            }
        });
        TextView status = view.findViewById(R.id.model_status);
        status.setText(model.isActive() ? R.string.model_active : R.string.model_inactive);
        int background = model.isActive() ? R.color.md_theme_light_secondaryContainer : R.color.md_theme_light_surface;
        int text = model.isActive() ? R.color.md_theme_light_onSecondaryContainer : R.color.md_theme_light_onSurface;
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
        status.setTextColor(ContextCompat.getColor(context, text));
        return view;
    }
}
