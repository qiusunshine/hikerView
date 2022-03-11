/*
 * Copyright (C) 2018 Light Team Software
 *
 * This file is part of ModPE IDE.
 *
 * ModPE IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModPE IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ahmadaghazadeh.editor.document.suggestions;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ahmadaghazadeh.editor.R;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author Trần Lê Duy
 */
public class SuggestionAdapter extends ArrayAdapter<SuggestionItem> {

    private LayoutInflater inflater;
    private ArrayList<SuggestionItem> clone;
    private ArrayList<SuggestionItem> suggestion;

    private Set<String> dynamicNames;

    @LayoutRes
    private int resourceID;

    private Filter suggestionFilter = new Filter() {

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            if (resultValue == null) {
                return "";
            }
            SuggestionItem item = ((SuggestionItem) resultValue);
            switch (item.getName()) {
                case "fori":
                    return "for (let i = 0; i < arr.length; i++){\n" +
                            "    \n" +
                            "}\n";
                case "forof":
                    return "for (let it of arr){\n" +
                            "    \n" +
                            "}\n";
                case "forin":
                    return "for (let i in arr){\n" +
                            "    \n" +
                            "}\n";
                case "pushit":
                    return "push({\n" +
                            "    title: ,\n" +
                            "    url: ,\n" +
                            "    col_type: \"\",\n" +
                            "    desc: \"\",\n" +
                            "    pic_url: \"\"\n" +
                            "});\n";
                case "pushLine":
                    return "push({\n" +
                            "    col_type: \"line_blank\"\n" +
                            "});\n";
                case "pushBigBlock":
                    return "push({\n" +
                            "    col_type: \"big_blank_block\"\n" +
                            "});\n";
                case "pushRich":
                    return "push({\n" +
                            "    title: ,\n" +
                            "    col_type: \"rich_text\",\n" +
                            "    extra: {\n" +
                            "        textSize: 18,\n" +
                            "        click: true\n" +
                            "    }\n" +
                            "});\n";
                case "js:init":
                    return "js:\n" +
                            "var d = [];\n\n" +
                            "setResult(d);";
                case "varPd":
                    return "let url = parseDom(, \"\");\n";
                case "varPdfh":
                    return "let a = pdfh(, \"\");\n";
                case "varPdfa":
                    return "let arr = pdfa(, \"\");\n";
                case "varjson":
                case "json:parse2":
                    return "var data = JSON.parse(getResCode());\n";
                case "json:parse":
                    return "JSON.parse()";
                case "json:stringify":
                    return "JSON.stringify()";
                case "initConfig":
                    return "initConfig({\n});\n";
            }
            return item.getName();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            suggestion.clear();
            if (constraint != null) {
                for (SuggestionItem item : clone) {
                    if (item.compareTo(constraint.toString()) == 0) {
                        suggestion.add(item);
                    }
                }
                if (dynamicNames != null) {
                    for (String item : dynamicNames) {
                        if (compareTo(item, constraint.toString()) == 0) {
                            suggestion.add(new SuggestionItem(SuggestionType.TYPE_VARIABLE, item));
                        }
                    }
                }
                filterResults.values = suggestion;
                filterResults.count = suggestion.size();
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<SuggestionItem> filteredList = (ArrayList<SuggestionItem>) results.values;
            clear();
            if (filteredList != null && filteredList.size() > 0) {
                addAll(filteredList);
            }
            notifyDataSetChanged();
        }
    };

    private int compareTo(String o2, String o) {
        String s = o.toLowerCase();
        return o2.toLowerCase().startsWith(s) ? 0 : -1;
    }

    public SuggestionAdapter(@NonNull Context context,
                             @LayoutRes int resource, @NonNull ArrayList<SuggestionItem> objects) {
        super(context, resource, objects);
        inflater = LayoutInflater.from(context);
        clone = (ArrayList<SuggestionItem>) objects.clone();
        suggestion = new ArrayList<>();
        resourceID = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(resourceID, null);
        }
        final SuggestionItem item = getItem(position);
        assert item != null;
        TextView desc = convertView.findViewById(R.id.suggestion_type);
        if(item.getType() == SuggestionType.TYPE_KEYWORD){
            desc.setText("K");
            desc.setTextColor(0xffEE4000);
        } else if(item.getType() == SuggestionType.TYPE_VARIABLE){
            desc.setText("V");
            desc.setTextColor(0xffEE7600);
        } else if(item.getType() == SuggestionType.TYPE_METHOD){
            desc.setText("M");
            desc.setTextColor(0xff00A2FF);
        } else {
            desc.setText( "H");
            desc.setTextColor(0xffA8C023);
        }
        TextView suggestionTitle = convertView.findViewById(R.id.suggestion_title);

        suggestionTitle.setText(item.getName());
        //Смена типа сделана для будущих обновлений
        //на данный момент ничего особенного не представляет.
        switch (item.getType()) {
            case SuggestionType.TYPE_METHOD: //Method
            case SuggestionType.TYPE_KEYWORD: //Keyword
            case SuggestionType.TYPE_VARIABLE: //Variable
                suggestionTitle.setTypeface(Typeface.MONOSPACE);
                break;
        }
        return convertView;
    }

    /*public ArrayList<SuggestionItem> getAllItems() {
        return clone;
    }

    public void clearAllData() {
        super.clear();
        clone.clear();
    }

    public void addData(@NonNull Collection<? extends SuggestionItem> collection) {
        addAll(collection);
        clone.addAll(collection);
    }*/

    @NonNull
    @Override
    public Filter getFilter() {
        return suggestionFilter;
    }

    public Set<String> getDynamicNames() {
        return dynamicNames;
    }

    public void setDynamicNames(Set<String> dynamicNames) {
        this.dynamicNames = dynamicNames;
    }
}
