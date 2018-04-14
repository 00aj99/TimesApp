package com.addie.maxfocus.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.addie.maxfocus.R;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

//TODO Layout to display past usage of apps with/without usage of timers
public class MainActivity extends AppCompatActivity {

    private static final int ALARM_NOTIF_ID = 234;
    private static final String SHARED_PREFS_KEY = "shared_prefs";
    @BindView(R.id.btn_apps)
    Button mAppsButton;
    @BindView(R.id.btn_study_break)
    Button mStudyButton;
    @BindView(R.id.btn_instant_alarm)
    Button mAlarmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        mAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAppsActivity();
            }
        });

        mAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAlarmActivity();
            }
        });

        mStudyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStudyTimerActivity();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_main_action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            default:
                break;
        }

        return true;
    }

    private void startAppsActivity() {
        finish();
        startActivity(new Intent(MainActivity.this, AppsActivity.class));
    }

    //    TODO: Improve
    // TODO: Launch an activity on first launch?
    private void startAlarmActivity() {

        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);

        String minutes = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getString(getString(R.string.pref_alarm_time_key), "");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, Integer.valueOf(minutes));
        Timber.d(cal.getTime().toString());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);


        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        startActivity(intent);

        /// Launches notification that displays that alarm has been set
        NotificationManager notificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alarm has been set!";
            String description = "Time to set the phone aside";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("a", name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel);
        } else {

            Notification.Builder mBuilder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Alarm has been set!")
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setContentText("It's time to set the phone aside");
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
            notificationManager.notify(ALARM_NOTIF_ID, mBuilder.build());
        }

        finishAffinity();
    }

    //    TODO:Implement
    private void startStudyTimerActivity() {

        Intent breakTimerintent = new Intent(AlarmClock.ACTION_SET_TIMER);

        int breakTimerLength = 20;//This is a value in seconds;Change 20 to value decided in sharedpreferences

        breakTimerintent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        breakTimerintent.putExtra(AlarmClock.EXTRA_LENGTH, breakTimerLength);
        //TODO Change message
        breakTimerintent.putExtra(AlarmClock.EXTRA_MESSAGE, "Time for a break!");

        //TODO: Issue a notification as well?
        startActivity(breakTimerintent);
        Toast.makeText(this, "Timer set for " + String.valueOf(breakTimerLength) + " seconds from now", Toast.LENGTH_SHORT).show();

        //TODO: Figure out how to set study timer after decided time

        // This is not working; only 1 timer is supported at least in Samsung
        /*
        Intent studyTimerIntent = new Intent(AlarmClock.ACTION_SET_TIMER);

        int studyTimerLength = breakTimerLength + 20;// CHange 20 to a value chosen from shared preferences

        studyTimerIntent.putExtra(AlarmClock.EXTRA_SKIP_UI,true);
        studyTimerIntent.putExtra(AlarmClock.EXTRA_LENGTH,studyTimerLength);
        //TODO Change message
        studyTimerIntent.putExtra(AlarmClock.EXTRA_MESSAGE,"Break is over, time to study");

        //TODO: Issue a notification as well? Something like" your timer is set
        //TODO: check notificattion for details" which will contain clock intent
        startActivity(studyTimerIntent);

        //TODO: Prompt user to decide whether to continue timer cycle or not
*/
//        startActivity(new Intent(MainActivity.this,StudyTimerActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
