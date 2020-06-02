package com.lampa.emotionrecognition;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ClassificationExpandableListAdapter extends BaseExpandableListAdapter {

    private Map<String, List<Pair<String, String>>> mStringListHashMap;
    private String[] mListHeaderGroup;

    public ClassificationExpandableListAdapter(
            Map<String, List<Pair<String, String>>> stringListHashMap) {

        mStringListHashMap = stringListHashMap;
        mListHeaderGroup = mStringListHashMap.keySet().toArray(new String[0]);
    }

    @Override
    public int getGroupCount() {
        return mListHeaderGroup.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mStringListHashMap.get(mListHeaderGroup[groupPosition]).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mListHeaderGroup[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mStringListHashMap.get(mListHeaderGroup[groupPosition]).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.expandable_list_group_classification, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.face_text_view);
        textView.setText(String.valueOf(getGroup(groupPosition)));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.expandable_list_item_classification, parent, false);
        }

        TextView labelTextView = convertView.findViewById(R.id.classification_label_text_view);

        TextView probabilityTextView =
                convertView.findViewById(R.id.classification_probability_text_view);

        Pair<String, String> child = (Pair<String, String>) getChild(groupPosition, childPosition);

        labelTextView.setText(String.valueOf(child.first));
        probabilityTextView.setText(String.valueOf(child.second));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
