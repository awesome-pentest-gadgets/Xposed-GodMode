package com.viewblocker.jrsen.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.adapter.AdapterDataObserver;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.preference.ImageViewPreference;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleDetailsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ALIAS = "alias";
    private static final String KEY_VISIBILITY = "visibility";

    private int index;
    private Drawable icon;
    private CharSequence label;
    private CharSequence packageName;
    private ViewRule viewRule;
    private Bitmap snapshot;
    private AdapterDataObserver<ViewRule> dataObserver;

    public void setIndex(int index) {
        this.index = index;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    public void setViewRule(ViewRule viewRule) {
        this.viewRule = viewRule;
    }

    public void setDataObserver(AdapterDataObserver<ViewRule> dataObserver) {
        this.dataObserver = dataObserver;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_empty);
        androidx.preference.PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = preferenceScreen.getContext();
        Preference headerPreference = new Preference(context);
        headerPreference.setSelectable(false);
        headerPreference.setIcon(icon);
        headerPreference.setTitle(label);
        headerPreference.setSummary(packageName);
        preferenceScreen.addPreference(headerPreference);

        Preference preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_create_time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        preference.setSummary(dateFormat.format(new Date(viewRule.timestamp)));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_generate_version);
        preference.setSummary(String.format(Locale.getDefault(), "%s %s", label, viewRule.matchVersionName));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_activity);
        preference.setSummary(Preconditions.optionDefault(viewRule.activityClass, "None"));
        preferenceScreen.addPreference(preference);

        EditTextPreference aliasEditTextPreference = new EditTextPreference(context);
        aliasEditTextPreference.setKey(KEY_ALIAS);
        aliasEditTextPreference.setTitle(R.string.rule_details_field_alias);
        aliasEditTextPreference.setDialogTitle(R.string.rule_details_set_alias);
        aliasEditTextPreference.setSummary(Preconditions.optionDefault(viewRule.alias, getString(R.string.rule_details_set_alias)));
        aliasEditTextPreference.setPersistent(false);
        aliasEditTextPreference.setOnPreferenceChangeListener(this);
        aliasEditTextPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((EditTextPreference) preference).setText(viewRule.alias);
                return false;
            }
        });
        preference = aliasEditTextPreference;
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        Rect bounds = new Rect(viewRule.x, viewRule.y, viewRule.x + viewRule.width, viewRule.y + viewRule.height);
        preference.setTitle(R.string.rule_details_field_view_bounds);
        preference.setSummary(bounds.toShortString());
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_view_type);
        preference.setSummary(viewRule.viewClass);
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_view_depth);
        preference.setSummary(Arrays.toString(viewRule.depth));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_res_name);
        preference.setSummary(viewRule.resourceName);
        preferenceScreen.addPreference(preference);

        if (!TextUtils.isEmpty(viewRule.text)) {
            preference = new Preference(context);
            preference.setTitle(R.string.rule_details_field_text);
            preference.setSummary(viewRule.text);
            preferenceScreen.addPreference(preference);
        }
        if (!TextUtils.isEmpty(viewRule.description)) {
            preference = new Preference(context);
            preference.setTitle(R.string.rule_details_field_description);
            preference.setSummary(viewRule.description);
            preferenceScreen.addPreference(preference);
        }

        DropDownPreference dropDownPreference = new DropDownPreference(context);
        dropDownPreference.setPersistent(false);
        dropDownPreference.setKey(KEY_VISIBILITY);
        dropDownPreference.setOnPreferenceChangeListener(this);
        dropDownPreference.setTitle(R.string.rule_details_field_visibility);
        CharSequence[] entries = {getString(R.string.rule_details_invisible), getString(R.string.rule_details_gone)};
        dropDownPreference.setSummary("%s");
        dropDownPreference.setEntries(entries);
        dropDownPreference.setEntryValues(new CharSequence[]{String.valueOf(View.INVISIBLE), String.valueOf(View.GONE)});
        dropDownPreference.setValue(String.valueOf(viewRule.visibility));
        preference = dropDownPreference;
        preferenceScreen.addPreference(preference);
        boolean hasSnapshot = !TextUtils.isEmpty(viewRule.imagePath);
        if (hasSnapshot) {
            preference = new ImageViewPreference(context);
            snapshot = BitmapFactory.decodeFile(viewRule.imagePath);
            preference.setDefaultValue(snapshot);
            preferenceScreen.addPreference(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_rule_details);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Preconditions.checkBitmap(snapshot)) {
            snapshot.recycle();
        }
        dataObserver = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        GodModeManager manager = GodModeManager.getDefault();
        if (KEY_ALIAS.equals(key)) {
            viewRule.alias = (String) newValue;
            preference.setSummary(viewRule.alias);
            manager.updateRule(packageName.toString(), viewRule);
            dataObserver.onItemChanged(index);
        } else if (KEY_VISIBILITY.equals(key)) {
            int newVisibility = Integer.parseInt((String) newValue);
            if (newVisibility != viewRule.visibility) {
                viewRule.visibility = newVisibility;
                manager.updateRule(packageName.toString(), viewRule);
                dataObserver.onItemChanged(index);
            }
        }
        return true;
    }

}
