package com.bignerdranch.android.criminalintent;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public class CrimeListActivity extends SingleFragmentActivity {

    private static String sMSG = "ZONA --> CrimeListActivity ";

    @Override
    protected Fragment createFragment() {

        Log.v(sMSG, "createFragment");

        return new CrimeListFragment();
    }

}
