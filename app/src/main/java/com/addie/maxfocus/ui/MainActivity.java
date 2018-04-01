package com.addie.maxfocus.ui;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.addie.maxfocus.R;
import com.addie.maxfocus.adapter.AppAdapter;
import com.addie.maxfocus.model.App;
import com.addie.maxfocus.receiver.AppDialogBroadcastReceiver;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements AppAdapter.AppOnClickHandler,
        TimePickerDialog.OnTimeSetListener {

    private static final String ACTION_APP_DIALOG = "com.addie.maxfocus.service.action.APP_DIALOG";
    private static final String TIME_KEY = "time";
    private static final String TARGET_PACKAGE_KEY = "target_package";

    @BindView(R.id.rv_main_apps)
    RecyclerView mAppsRecyclerView;

    private AppAdapter mAdapter;
    private ArrayList<App> mAppsList;
    private App mSelectedApp;
    private AppDialogBroadcastReceiver mAppDialogBroadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        requestUsageStatsPermission();

        loadAppsList();

        initialiseRecyclerView();

        //Register broadcast receiver to receive "stop app" dialogs
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APP_DIALOG);
        mAppDialogBroadcastReceiver = new AppDialogBroadcastReceiver();
        registerReceiver(mAppDialogBroadcastReceiver,filter);

    }



    void requestUsageStatsPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !hasUsageStatsPermission(this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mAppDialogBroadcastReceiver);
    }

    /**
     * Loads a list of installed apps on the device using PackageManager
     */
    //TODO Change to different thread to prevent main thread from freezing
    private void loadAppsList() {
        mAppsList = new ArrayList<>();

        final PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        // Sorts the list in alphabetical order of app names
        final PackageItemInfo.DisplayNameComparator comparator = new PackageItemInfo.DisplayNameComparator(packageManager);
        Collections.sort(packages, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return comparator.compare(lhs, rhs);
            }
        });

        // Adds app to the list if it has a launch activity
        for (ApplicationInfo packageInfo : packages) {

            if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {

                App app = new App(packageInfo.loadLabel(packageManager).toString(), packageInfo.packageName, packageInfo.loadIcon(packageManager));
                mAppsList.add(app);
                Timber.d(app.getmIcon() + " " + app.getmPackage() + " " + app.getmTitle());
            }
        }
    }

    /**
     * Initialises the RecyclerView displaying the list of apps
     */
    private void initialiseRecyclerView() {
        mAdapter = new AppAdapter(this, this);
        mAppsRecyclerView.setAdapter(mAdapter);

        mAdapter.setListData(mAppsList);

        mAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAppsRecyclerView.setHasFixedSize(true);

    }

    /**
     * Called by AppAdapter when an app is selected in the RecyclerView
     * @param selectedApp the app that is selected
     */
    @Override
    public void onClick(App selectedApp) {
        mSelectedApp = selectedApp;

        showTimerDialog();
    }

    /**
     * Shows timer dialog to select the duration for which the selected app is to be run
     */
    public void showTimerDialog() {
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                MainActivity.this, Calendar.HOUR_OF_DAY, Calendar.MINUTE, true
        );
        tpd.setTitle("Specify Usage Time HH:MM");
        tpd.setOkText("Start");
        tpd.setInitialSelection(0, 10);
        tpd.setCancelText("Cancel");
        tpd.show(getFragmentManager(), "Timepickerdialog");
    }

    /**
     * Called when time is selected and "start" is pressed on the dialog
     * @param view
     * @param hourOfDay
     * @param minute
     * @param second
     */
    @Override
    public void onTimeSet(TimePickerDialog view, final int hourOfDay, final int minute, int second) {

        // Launches the selected app
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(mSelectedApp.getmPackage());

        // Broadcast intent with selected time for app to be stopped
        int time = ((hourOfDay*60)+minute)*60000;
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra(TIME_KEY,time);
        broadcastIntent.putExtra(TARGET_PACKAGE_KEY,mSelectedApp.getmPackage());
        broadcastIntent.setAction(ACTION_APP_DIALOG);

        sendBroadcast(broadcastIntent);

        finish();
        startActivity(launchIntent);

    }
}
