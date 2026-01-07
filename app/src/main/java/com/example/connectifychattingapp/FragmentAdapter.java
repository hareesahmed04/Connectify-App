package com.example.connectifychattingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class FragmentAdapter extends FragmentPagerAdapter {
    public FragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new ChatFragment();
            case 1:
               return new ContactFragment();
            case 2:
                return new CallLogsFragment();
            default:
                return new ChatFragment();
        }
    }
    @Override
    public int getCount() {
        return 3;
    }
}
