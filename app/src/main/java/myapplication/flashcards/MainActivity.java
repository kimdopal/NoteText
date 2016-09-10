package myapplication.flashcards;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;

import static android.widget.AdapterView.OnItemLongClickListener;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected static ArrayList<String> questions;
    protected static ArrayList<String> answers;
    private static ArrayAdapter<String> itemsAdapter;
    private ListView lvItems;
    private int position;
    private String setName;
    private static final int DIALOG_ID = 0;
    private int setHour, setMinute;
    //private int extraPosition;
    //private ArrayList<Set> Sets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final Intent editIntent = getIntent();
        setName = editIntent.getStringExtra("name");

        lvItems = (ListView) findViewById(R.id.lvItems);
        questions = new ArrayList<String>();
        answers = new ArrayList<String>();

        //set up adapter and listeners
        itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, questions);
        lvItems.setAdapter(itemsAdapter);
        setupListViewListener();

        //add questions to listView by typing and clicking the add button
        Button addButton = (Button) findViewById(R.id.btnAddItem);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText textField = (EditText) findViewById(R.id.textField);
                String itemText = textField.getText().toString();
                itemsAdapter.add(itemText);
                answers.add("");
                textField.setText("");
                //pushNotification("5 second delay", 5000);
                //pushNotification();
            }

        });

        //set up drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) this);

        fileRead();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_shufflereview) {
            shuffle("true");
            return true;
        }
        if (id == R.id.action_review) {
            shuffle("false");
            return true;
        }
        if (id == R.id.action_sets) {
            Intent intent = new Intent(MainActivity.this, MainMenu.class);
            intent.putExtra("questions", questions);
            intent.putExtra("answers", answers);
            // intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            fileWrite();
            startActivity(intent);
            overridePendingTransition(R.anim.activity_open_scale,R.anim.activity_close_translate);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_view_all_sets) {
            Intent intent = new Intent(MainActivity.this, MainMenu.class);
            intent.putExtra("questions", questions);
            intent.putExtra("answers", answers);
            // intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            fileWrite();
            startActivity(intent);
            overridePendingTransition(R.anim.activity_open_scale,R.anim.activity_close_translate);

        } else if (id == R.id.nav_shuffle_review) {
            shuffle("true");

        } else if (id == R.id.nav_review) {
            shuffle("false");

        } else if (id == R.id.nav_notification) {
            setNotification();

        } else if (id == R.id.nav_settings) {
            Toast.makeText(getBaseContext(),"Settings!",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);

        } else if (id == R.id.nav_save) {
            fileWrite();
            Toast.makeText(getBaseContext(),"Set saved!",Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.nav_share) {
            Toast.makeText(getBaseContext(),"Share!!",Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*Allows the time picker to set when the notification takes place*/
    public void setNotification(){
        createTimer(DIALOG_ID).show();
    }

    protected Dialog createTimer(int id){
        if(id == DIALOG_ID)
            return new TimePickerDialog(MainActivity.this, timePickerListener, setHour, setMinute, false);
        return null;
    }

    /*Timepicker dialog that allows user to set the time for notification*/
    protected TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener(){
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay){

            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            setHour = hourOfDay;
            setMinute = minuteOfDay;

            hour = setHour - hour;
            minute = setMinute - minute;

            if(hour < 0 || (hour == 0 && minute < 0)) {
                hour += 24;
            }

            String msg = "Time to review " + setName +"!";
            int delay = hour * 60 * 60 * 1000 + minute * 60 * 1000;

            Toast.makeText(MainActivity.this,
                    "difference in time: "+ hour + " hours, " + minute + " minutes", Toast.LENGTH_SHORT).show();

            pushNotification(msg, delay);
        }
    };


    /*Pushes the notification when the set time has elapsed. */

    private void pushNotification(String content, int delay) {

        long totalDelay = SystemClock.elapsedRealtime() + delay;

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder notification = new Notification.Builder(this)
                .setContentTitle("Scheduled Notification")
                .setContentText(content)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp);


        Intent notificationIntent = new Intent(MainActivity.this, mBroadcastReceiver.class);
        //notificationIntent.putExtra(mBroadcastReceiver.getNotificationID(), 1);
        notificationIntent.putExtra(mBroadcastReceiver.getNotification(), notification.build());
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, totalDelay, pendingIntent);

    }



    // Attaches a long click listener and click listener to the listview
    private void setupListViewListener() {
        lvItems.setOnItemLongClickListener(new OnItemLongClickListener() {
        @Override

        public boolean onItemLongClick(AdapterView<?> adapter, View item, int pos, long id) {
              // Remove the item within array at position
              position = pos;
              new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                    questions.remove(getPos());
                                    answers.remove(getPos());
                                    // Refresh the adapter
                                    itemsAdapter.notifyDataSetChanged();
                                     }
                                })
                     .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                            }
                                 })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
                    // Return true consumes the long click event (marks it handled)
                 return true;
                    }
                }
        );
        final Intent[] intent = {null};
        lvItems.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                intent[0] = new Intent(MainActivity.this, EditCard.class);
                intent[0].putExtra("questions", questions.get(position));
                intent[0].putExtra("answers", answers.get(position));
                intent[0].putExtra("position", position);
                intent[0].putExtra("setName", setName);
                startActivity(intent[0]);
            }
        });
    }

    /*
    Writes data to internal storage.
    */

    private void fileWrite() {

        File file = new File(getFilesDir() +"/"+ setName+".txt");
        try {
            FileWriter  fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for(int i=0; i <questions.size();i++) {
                bw.write(questions.get(i) + "\n");
                bw.write(answers.get(i) + "\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Reads from internal storage, parses the data, then loads into current activity.*/
    private void fileRead(){
        try{
            FileInputStream fin = openFileInput(setName+".txt");
            int c, row=0;
            String data="";

            while( (c = fin.read()) != -1){
                data = data + Character.toString((char)c);
                if(data.contains("\n")){
                    data = data.substring(0, data.length()-1);
                    if((row & 1) == 0)
                         itemsAdapter.add(data);
                    else
                        answers.add(data);
                    data = "";
                    row++;
                }
            }

            fin.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //if randomize is true, then shuffle when switching to new activity. Else use default order
    private void shuffle(String randomize){
        if (getSize() <= 0)
            Toast.makeText(getApplicationContext(), "No cards to review", Toast.LENGTH_LONG).show();
        else {
            Intent intent = new Intent(MainActivity.this, ReviewCards.class);
            intent.putExtra("shuffle", randomize);
            startActivity(intent);
        }
    }


    protected static void refreshAdapter(){
        itemsAdapter.notifyDataSetChanged();
    }

    protected int getPos() {
        return position;
    }

    protected static void setAnswers(int pos, String str) {
        answers.set(pos, str);
    }

    protected static void setQuestions(int pos, String str) {
        questions.set(pos, str);
    }

    protected static String getAnswers(int pos) {
        return answers.get(pos);
    }

    protected static String getQuestions(int pos) {
        return questions.get(pos);
    }

    protected static int getSize() {
        return questions.size();
    }
}
