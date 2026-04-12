package com.qinghe.liteai.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qinghe.liteai.R;
import com.qinghe.liteai.model.FeatureOption;

import java.util.List;

public class FeatureAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<FeatureOption> items;

    public FeatureAdapter(Context context, List<FeatureOption> items) {
        this.inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FeatureOption getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_feature, parent, false);
        FeatureOption item = getItem(position);
        ((ImageView) view.findViewById(R.id.feature_icon)).setImageResource(item.getIconResId());
        ((TextView) view.findViewById(R.id.feature_title)).setText(item.getTitle());
        return view;
    }
}
