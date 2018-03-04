/* Copyright (C) 2013, 2014, 2016  Olga Yakovleva <yakovleva.o.v@gmail.com> */

/* This program is free software: you can redistribute it and/or modify */
/* it under the terms of the GNU Lesser General Public License as published by */
/* the Free Software Foundation, either version 2.1 of the License, or */
/* (at your option) any later version. */

/* This program is distributed in the hope that it will be useful, */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the */
/* GNU Lesser General Public License for more details. */

/* You should have received a copy of the GNU Lesser General Public License */
/* along with this program.  If not, see <http://www.gnu.org/licenses/>. */


package com.github.olga_yakovleva.rhvoice.android;


import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.InputType;
import com.github.olga_yakovleva.rhvoice.LanguageInfo;
import com.github.olga_yakovleva.rhvoice.TTSEngine;
import com.github.olga_yakovleva.rhvoice.VoiceInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG="RHVoiceSettingsFragment";

    private Preference.OnPreferenceChangeListener onVoiceChange=new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref,Object obj)
            {
                pref.setSummary(obj.toString());
                return true;
            }
        };

    private Map<String,List<VoiceInfo>> groupVoicesByLanguage(List<VoiceInfo> voices)
    {
        Map<String,List<VoiceInfo>> result=new TreeMap<String,List<VoiceInfo>>();
        for(VoiceInfo voice:voices)
            {
                LanguageInfo language=voice.getLanguage();
                String code=language.getAlpha2Code();
                List<VoiceInfo> languageVoices=result.get(code);
                if(languageVoices==null)
                    {
                        languageVoices=new ArrayList<VoiceInfo>();
                        result.put(code,languageVoices);
                    }
                languageVoices.add(voice);
            }
        return result;
    }

    private void buildLanguagePreferenceCategory(PreferenceScreen screen,List<VoiceInfo> voices)
    {
        PreferenceScreen cat=getPreferenceManager().createPreferenceScreen(getActivity());
        cat.setPersistent(false);
        LanguageInfo language=voices.get(0).getLanguage();
        String firstVoiceName=voices.get(0).getName();
        String code2=language.getAlpha2Code();
        String code3=language.getAlpha3Code();
        cat.setKey("language."+code3);
        Locale locale=new Locale(code2);
        cat.setTitle(locale.getDisplayName());
        screen.addPreference(cat);
        ListPreference voicePref=new ListPreference(getActivity());
        voicePref.setOnPreferenceChangeListener(onVoiceChange);
        voicePref.setKey("language."+code3+".voice");
        voicePref.setTitle(R.string.default_voice_title);
        voicePref.setSummary(firstVoiceName);
        voicePref.setDialogTitle(R.string.default_voice_dialog_title);
        int voiceCount=voices.size();
        String[] voiceNames=new String[voiceCount];
        for(int i=0;i<voiceCount;++i)
            {
                voiceNames[i]=voices.get(i).getName();
            }
        voicePref.setEntries(voiceNames);
        voicePref.setEntryValues(voiceNames);
        voicePref.setDefaultValue(firstVoiceName);
        cat.addPreference(voicePref);
        voicePref.setSummary(voicePref.getEntry());
        CheckBoxPreference detectPref=new CheckBoxPreference(getActivity());
        detectPref.setKey("language."+code3+".detect");
        detectPref.setTitle(R.string.detect_language_title);
        detectPref.setDefaultValue(true);
        cat.addPreference(detectPref);
        if(language.getPseudoEnglish())
            {
                CheckBoxPreference engPref=new CheckBoxPreference(getActivity());
                engPref.setKey("language."+code3+".use_pseudo_english");
                engPref.setTitle(R.string.pseudo_english_title);
                engPref.setDefaultValue(true);
                cat.addPreference(engPref);
            }
        InputFilter[] inputFilters=new InputFilter[]{new InputFilter.LengthFilter(3)};
        EditTextPreference volumePref=new EditTextPreference(getActivity());
        volumePref.setKey("language."+code3+".volume");
        volumePref.setTitle(R.string.speech_volume);
        volumePref.setDefaultValue("100");
        volumePref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        volumePref.getEditText().setSelectAllOnFocus(true);
        volumePref.getEditText().setFilters(inputFilters);
        cat.addPreference(volumePref);
        EditTextPreference ratePref=new EditTextPreference(getActivity());
        ratePref.setKey("language."+code3+".rate");
        ratePref.setTitle(R.string.speech_rate);
        ratePref.setDefaultValue("100");
        ratePref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        ratePref.getEditText().setSelectAllOnFocus(true);
        ratePref.getEditText().setFilters(inputFilters);
        cat.addPreference(ratePref);
    }

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.settings);
        List<VoiceInfo> voices=Data.getVoices(getActivity());
        if(voices.isEmpty())
            return;
        Map<String,List<VoiceInfo>> voiceGroups=groupVoicesByLanguage(voices);
        PreferenceScreen screen=getPreferenceManager().createPreferenceScreen(getActivity());
        screen.setKey("speech_settings");
        screen.setTitle(R.string.speech_settings);
        getPreferenceScreen().addPreference(screen);
        for(Map.Entry<String,List<VoiceInfo>> entry: voiceGroups.entrySet())
            {
                buildLanguagePreferenceCategory(screen,entry.getValue());
            }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs,String key)
    {
        if(DataSyncAction.getWifiOnlyKey().equals(key))
            getActivity().startService(new Intent(getActivity(),DataService.class));
}

    @Override
    public void onResume()
    {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
}

    @Override
    public void onPause()
    {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
}
}
