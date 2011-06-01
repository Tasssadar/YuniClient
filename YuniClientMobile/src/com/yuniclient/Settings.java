package com.yuniclient;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.yuni.client.R;

public class Settings extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);  
        addPreferencesFromResource(R.xml.preferences);
    }
}