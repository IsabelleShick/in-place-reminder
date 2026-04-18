package com.example.inplacereminder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatSpinner;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectSpinner extends AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {
    private String[] items;
    private boolean[] checkedItems;
    private MultiSelectSpinnerListener listener;

    public interface MultiSelectSpinnerListener {
        void onItemsSelected(boolean[] selected);
    }

    public MultiSelectSpinner(Context context) {
        super(context);
    }

    public MultiSelectSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiSelectSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (which < checkedItems.length) {
            checkedItems[which] = isChecked;
        }
    }

    @Override
    public boolean performClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(items, checkedItems, this);
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (listener != null) {
                listener.onItemsSelected(checkedItems);
            }
            updateDisplay();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
        return true;
    }

    public void setItems(String[] items) {
        this.items = items;
        this.checkedItems = new boolean[items.length];
        setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{""}));
    }

    public void setSelection(int index) {
        if (index >= 0 && index < checkedItems.length) {
            checkedItems[index] = true;
            updateDisplay();
        }
    }

    public void setSelection(boolean[] selection) {
        if (selection != null && selection.length == checkedItems.length) {
            System.arraycopy(selection, 0, checkedItems, 0, selection.length);
            updateDisplay();
        }
    }

    public boolean[] getCheckedItemPositions() {
        return checkedItems;
    }

    public List<String> getSelectedItems() {
        List<String> selectedItems = new ArrayList<>();
        if (items != null && checkedItems != null) {
            for (int i = 0; i < items.length; i++) {
                if (checkedItems[i]) {
                    selectedItems.add(items[i]);
                }
            }
        }
        return selectedItems;
    }

    public void setListener(MultiSelectSpinnerListener listener) {
        this.listener = listener;
    }

    private void updateDisplay() {
        List<String> selectedItems = getSelectedItems();
        if (selectedItems.isEmpty()) {
            setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"Select days"}));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedItems.size(); i++) {
                sb.append(selectedItems.get(i).substring(0, 3)); // Show short names: Sun, Mon, etc.
                if (i < selectedItems.size() - 1) {
                    sb.append(", ");
                }
            }
            setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{sb.toString()}));
        }
    }

    public void clearSelection() {
        for (int i = 0; i < checkedItems.length; i++) {
            checkedItems[i] = false;
        }
        updateDisplay();
    }
}

