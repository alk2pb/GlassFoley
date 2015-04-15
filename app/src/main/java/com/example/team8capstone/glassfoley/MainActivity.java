package com.example.team8capstone.glassfoley;

import com.example.team8capstone.glassfoley.image.ImageActivity;
import com.example.team8capstone.glassfoley.video.VideoActivity;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;
import java.lang.Runtime;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class MainActivity extends Activity implements MediaPlayer.OnCompletionListener {
    // Array of Card Infos
    private ArrayList<CardInfo> cardInfos = new ArrayList<CardInfo>();

    // Other variables used (name is self explanatory)
    private CardScrollView mCardScroller;
    private boolean mVoiceMenuEnabled = true;
    private CardScrollAdapter mAdapter;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isPaused = false;

    private Intent image;
    private Intent video;

//    private static final String TAG = "LogEntryStart";
//
//    private StringBuilder log=new StringBuilder();
//
//    private Time time = new Time();

//    private boolean hasPlayed = false;

//    private int currentSlide = 0;

    private AudioManager am;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Sets Card Info
        setCardInfo();

        // Instantiates a new intent for the ImageActivity that will be activated when
        // the user wishes to view a picture
        image = new Intent(this, ImageActivity.class);

        // Instantiates a new intent for the VideoActivity that will be activated when
        // the user wishes to view a video
        video = new Intent(this, VideoActivity.class);

        // Requests a voice menu on this activity. As for any other window feature,
        // be sure to request this before setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCardScroller = new CardScrollView(this);
        mAdapter = new CardAdapter(createCards(this), this, cardInfos);
        mCardScroller.setAdapter(mAdapter);
        setCardScrollerListener();
        setContentView(mCardScroller);

//        try {
//            Process process = Runtime.getRuntime().exec("logcat -d");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//
//
//            String line = "";
//            while ((line = bufferedReader.readLine()) != null) {
//                log.append(line);
//            }
//
//        } catch (IOException e) {
//        }

        // Set mediaPlayer volume to max
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
        am.setStreamVolume(AudioManager.STREAM_SYSTEM,am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM),0);

    }

    // When the MediaPlayer finishes, close and refresh the menu
    public void onCompletion(MediaPlayer mediaplayer) {
        getWindow().closePanel(WindowUtils.FEATURE_VOICE_COMMANDS);
        closeOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
//        time.setToNow();
//        Log.i(TAG,time.toString() + ", " + "MainActivity activated" + " LogEntryEnd");
//        Log.i(TAG,time.toString() + ", " + "Current Slide:" + mCardScroller.getSelectedItemPosition() + " LogEntryEnd");
    }

    @Override
    protected void onPause() {
        if (mediaPlayer.isPlaying()){
            mediaPlayer.reset();
        }

        mCardScroller.deactivate();
//        time.setToNow();
//        Log.i(TAG, time.toString() + ", " + "MainActivity deactivated" + " LogEntryEnd");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Release the MediaPlayer when the activity finishes
        mediaPlayer.release();
//        time.setToNow();
//        Log.i(TAG,time.toString() + ", " + "MainActivity destroyed" + " LogEntryEnd");
//        try {
//            File file = new File(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS + "/log_" + time.toString().replace("/","_") + ".txt");
//
//            BufferedWriter output = new BufferedWriter(new FileWriter(file));
//            output.write(log.toString());
//            output.close();
//        }
//        catch (Exception e){
//            Log.e(TAG,e.getMessage());
//        }
        super.onDestroy();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.voice_menu, menu);
//            time.setToNow();
//            Log.i(TAG,time.toString() + ", " + "Menu created" + " LogEntryEnd");
            return true;
        }
        // Good practice to pass through, for options menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            int position = mCardScroller.getSelectedItemPosition();

            addDefaultMenuOptions(menu, position);
            addCustomMenuOptions(menu, position);

            // If the MediaPlayer is playing, add media options
            if (mediaPlayer.isPlaying() || isPaused){
                addMediaMenuOptions(menu);

            }

            collapseMenu(menu,1);
//            time.setToNow();
//            Log.i(TAG,time.toString() + ", " + "Menu populated" + " LogEntryEnd");

            // Dynamically decides between enabling/disabling voice menu.
            return mVoiceMenuEnabled;
        }

        // Good practice to pass through, for options menu.
        return super.onPreparePanel(featureId, view, menu);
    }

    // Add default menu options
    private void addDefaultMenuOptions(Menu menu, int position){
        menu.add(Menu.NONE,10,Menu.NONE,"next");
        menu.add(Menu.NONE,11,Menu.NONE,"back");
        menu.addSubMenu(Menu.NONE,12,Menu.NONE,"goto");
        setGotoMenuOptions(menu, position);
        menu.addSubMenu(Menu.NONE,13,Menu.NONE,"exit");
        menu.findItem(13).getSubMenu().add(Menu.NONE,14,Menu.NONE,"yes");
    }

    private void setGotoMenuOptions(Menu menu, int position) {
        // Add menu options based on slide position
        for (CardInfo cardInfo : cardInfos) {
            if (!cardInfo.hasHeader) {
                menu.findItem(12).getSubMenu().add(Menu.NONE, cardInfo.goTo, Menu.NONE, Integer.toString(cardInfo.slideNumber + 1));
            }
            else {
                menu.findItem(12).getSubMenu().add(Menu.NONE, cardInfo.goTo, Menu.NONE, cardInfo.header);
            }
        }
    }

    private void addCustomMenuOptions(Menu menu, int position){

        // Add and adjust menu options based on slide content
        menu.findItem(12).getSubMenu().removeItem(cardInfos.get(0).offset + position);

        if (position == 0){
            menu.removeItem(11);
        }

        if (position == cardInfos.size() - 1){
            menu.removeItem(10);
        }

        if (cardInfos.get(position).hasAudio){
            if (!mediaPlayer.isPlaying() && !isPaused) {
                menu.add(Menu.NONE,1,Menu.NONE,"play audio");
            }
        }

        if (cardInfos.get(position).hasVideo){
            menu.add(Menu.NONE,0,Menu.NONE,"play video");
        }

        if (cardInfos.get(position).hasImage){
            menu.add(Menu.NONE,2,Menu.NONE,"view picture");
        }

    }

    private void addMediaMenuOptions(Menu menu){
        menu.add(Menu.NONE,3,Menu.NONE,"stop media");
        menu.addSubMenu(Menu.NONE,9,Menu.NONE,"media options");
        if (isPaused){
            menu.findItem(9).getSubMenu().add(Menu.NONE,4,Menu.NONE,"resume");
        }
        else {
            menu.findItem(9).getSubMenu().add(Menu.NONE,5,Menu.NONE,"pause");
        }
        menu.findItem(9).getSubMenu().add(Menu.NONE,6,Menu.NONE,"rewind");
        menu.findItem(9).getSubMenu().add(Menu.NONE,7,Menu.NONE,"fast forward");
        menu.findItem(9).getSubMenu().add(Menu.NONE,8,Menu.NONE,"play from beginning");
//        menu.findItem(9).getSubMenu().add(Menu.NONE,15,Menu.NONE,"volume up");
//        menu.findItem(9).getSubMenu().add(Menu.NONE,16,Menu.NONE,"volume down");
    }

    // Collapse a menu to prevent menu options from going off the viewable screen
    private void collapseMenu(Menu menu, int level){
//        if (level == 1){
//            if (menu.size() > 4){
//                menu.addSubMenu(Menu.NONE,level*100,Menu.NONE,"more options");
//            }
//        }
//        else {
//            if (menu.size() > 3){
//                menu.addSubMenu(Menu.NONE,level*100,Menu.NONE,"more options");
//            }
//        }
//
//        if (level == 1){
//            if (menu.size() > 4){
//                for (int i = 3; i < menu.size(); i++){
//                    if (!(menu.getItem(i).getItemId()/100 >= 1)){
//                        if(menu.getItem(i).hasSubMenu()){
//                            reMenu(menu.findItem(level*100).getSubMenu(), menu.getItem(i));
//                        }
//                        else {
//                            menu.findItem(level*100).getSubMenu().add(Menu.NONE,menu.getItem(i).getItemId(),Menu.NONE,menu.getItem(i).getTitle());
//                        }
//                        menu.removeItem(menu.getItem(i).getItemId());
//                    }
//                }
//            }
//        }
//        else {
//            if (menu.size() > 3){
//                for (int i = 2; i < menu.size(); i++){
//                    if (!(menu.getItem(i).getItemId()/100 >= 1)){
//                        if(menu.getItem(i).hasSubMenu()){
//                            reMenu(menu.findItem(level*100).getSubMenu(), menu.getItem(i));
//                        }
//                        else {
//                            menu.findItem(level*100).getSubMenu().add(Menu.NONE,menu.getItem(i).getItemId(),Menu.NONE,menu.getItem(i).getTitle());
//                        }
//                        menu.removeItem(menu.getItem(i).getItemId());
//                    }
//                }
//            }
//        }

        for (int i = 0; i < menu.size(); i++){
            if (menu.getItem(i).hasSubMenu()){
                collapseMenu(menu.getItem(i).getSubMenu(),level+1);
            }
        }
        menu.add(Menu.NONE,99,Menu.NONE,"cancel");
    }

    // Add a pre-populated SubMenu to another Menu
    private void reMenu(Menu menu, MenuItem reMenu){
        menu.addSubMenu(Menu.NONE,reMenu.getItemId(),Menu.NONE,reMenu.getTitle());
        for (int i = 0; i < reMenu.getSubMenu().size(); i++){
            if (reMenu.getSubMenu().getItem(i).hasSubMenu()){
                menu.findItem(reMenu.getItemId()).getSubMenu().addSubMenu(Menu.NONE,reMenu.getSubMenu().getItem(i).getItemId(),Menu.NONE,reMenu.getSubMenu().getItem(i).getTitle());
                reMenu(menu.findItem(reMenu.getItemId()).getSubMenu().findItem(reMenu.getSubMenu().getItem(i).getItemId()).getSubMenu(),reMenu.getSubMenu().getItem(i));
            }
            else{
                menu.findItem(reMenu.getItemId()).getSubMenu().add(Menu.NONE,reMenu.getSubMenu().getItem(i).getItemId(),Menu.NONE,reMenu.getSubMenu().getItem(i).getTitle());
            }
        }
    }

    // Set menu item actions
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case 2:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "ImageActivity started" + " LogEntryEnd");
                    startActivity(image);
                    break;
                case 1:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer started" + " LogEntryEnd");
                    mediaPlayer.start();
                    break;
                case 0:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "VideoActivity started" + " LogEntryEnd");
                    startActivity(video);
                    break;
                case 3:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer stopped" + " LogEntryEnd");
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(0);
                    isPaused = false;
                    break;
                case 4:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer resumed" + " LogEntryEnd");
                    mediaPlayer.start();
                    isPaused = false;
                    break;
                case 5:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer paused" + " LogEntryEnd");
                    mediaPlayer.pause();
                    isPaused = true;
                    break;
                case 6:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer rewinded" + " LogEntryEnd");
                    if (mediaPlayer.getCurrentPosition() < 3000){
                        mediaPlayer.seekTo(0);
                    }
                    else {
                        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 3000);
                    }
                    break;
                case 7:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "ImageActivity fast forwarded" + " LogEntryEnd");
                    if (mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() < 3000){
                        mediaPlayer.seekTo(mediaPlayer.getDuration());
                    }
                    else {
                        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 3000);
                    }
                    break;
                case 8:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "MediaPlayer restarted" + " LogEntryEnd");
                    mediaPlayer.seekTo(0);
                    isPaused = false;
                    break;
                case 10:
                    if (mCardScroller.getSelectedItemPosition() < cardInfos.size())
                    {
                        mCardScroller.setSelection(mCardScroller.getSelectedItemPosition() + 1);
                    }
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "Next slide selected" + " LogEntryEnd");
                    break;
                case 11:
                    if (mCardScroller.getSelectedItemPosition() > 0)
                    {
                        mCardScroller.setSelection(mCardScroller.getSelectedItemPosition() - 1);
                    }
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "Previous slide selected" + " LogEntryEnd");
                    break;
                case 14:
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "Exit selected" + " LogEntryEnd");
                    finish();
                    break;
//                case 15:
//                    am.adjustStreamVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_RAISE,0);
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "Volume increased" + " LogEntryEnd");
//                    break;
//                case 16:
//                    am.adjustStreamVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_LOWER,0);
//                    time.setToNow();
//                    Log.i(TAG,time.toString() + ", " + "Volume decreased" + " LogEntryEnd");
//                    break;
                default:
                    for (CardInfo cardInfo : cardInfos){
                        if (item.getItemId() == cardInfo.goTo){
                            mCardScroller.setSelection(cardInfo.slideNumber);
//                            time.setToNow();
//                            Log.i(TAG,time.toString() + ", " + "Goto selected" + " LogEntryEnd");
                            return true;
                        }
                    }
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onMenuOpened (int featureId, Menu menu) {
//        time.setToNow();
//        Log.i(TAG,time.toString() + ", " + "Menu opened" + " LogEntryEnd");
        return true;
    }

    // When the menu is closed, refresh it
    @Override
    public void onPanelClosed (int featureId, Menu menu) {
        getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
        invalidateOptionsMenu();
//        time.setToNow();
//        Log.i(TAG,time.toString() + ", " + "Menu closed" + " LogEntryEnd");
    }

    // Set media resources based on slide position
    private void setMediaResources(int position){
        if (cardInfos.get(position).hasImage){
            image.removeExtra("imageResource");
            image.putExtra("imageResource", cardInfos.get(position).imageResource);
        }

        if (cardInfos.get(position).hasAudio){
            if (!mediaPlayer.isPlaying() && !isPaused) {
                mediaPlayer = MediaPlayer.create(MainActivity.this, cardInfos.get(position).audioResource);
                mediaPlayer.setOnCompletionListener(MainActivity.this);
            }
        }

        if (cardInfos.get(position).hasVideo){
            video.removeExtra("videoResource");
            video.putExtra("videoResource", cardInfos.get(position).videoResource);
        }
    }

    // Starts audio and video when card is selected
    private void startMedia(int position) {
        if (cardInfos.get(position).hasAudio){
//            time.setToNow();
//            Log.i(TAG,time.toString() + ", " + "MediaPlayer started" + " LogEntryEnd");
            mediaPlayer.start();
        }

        if (cardInfos.get(position).hasVideo){
//            time.setToNow();
//            Log.i(TAG,time.toString() + ", " + "VideoActivity started" + " LogEntryEnd");
            startActivity(video);
        }
//        hasPlayed = true;
    }

    private void setCardScrollerListener() {
        // When an item is selected, refresh the menu
        mCardScroller.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
                invalidateOptionsMenu();
                setMediaResources(mCardScroller.getSelectedItemPosition());
//                time.setToNow();
//                Log.i(TAG,time.toString() + ", " + "New slide selected" + " LogEntryEnd");
//                Log.i(TAG,time.toString() + ", " + "Current slide: " + mCardScroller.getSelectedItemPosition() + " LogEntryEnd");
//                if (currentSlide != mCardScroller.getSelectedItemPosition()) {
//                    hasPlayed = false;
//                    currentSlide = mCardScroller.getSelectedItemPosition();
//                }
//                if (!hasPlayed) {
//                    startMedia(mCardScroller.getSelectedItemPosition());
//                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add sound effect when an slide is clicked
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int soundEffect = Sounds.TAP;
//                time.setToNow();
//                Log.i(TAG,time.toString() + ", " + "Slide " + mCardScroller.getSelectedItemPosition() + " tapped" + " LogEntryEnd");
                // Play sound.
                am.playSoundEffect(soundEffect);
                openOptionsMenu();
            }
        });
    }

    // Create the slides for the "powerpoint" view
    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();

        for (CardInfo cardInfo : cardInfos){
            if (cardInfo.hasXmlLayout){
                cards.add(cardInfo.slideNumber, new CardBuilder(context,cardInfo.layout)
                        .setEmbeddedLayout(cardInfo.xmlLayout));
            }
            else {
                CardBuilder cardBuilder = new CardBuilder(context,cardInfo.layout);

                if (cardInfo.hasText){
                    cardBuilder.setText(cardInfo.text);
                }

                cards.add(cardInfo.slideNumber, cardBuilder);
            }
        }
        return cards;
    }

    // Set Card Info
    private void setCardInfo() {
        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Male Catheterization")
                .setText("")
                .setVideoResource(R.raw.fvid0)
                .setImageResource(R.drawable.fpic0));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Preparation")
                .addBullet("Bring kit and all equipment to bedside")
                .addBullet("Perform Hand Hygiene")
                .addBullet("Provide privacy")
                .setVideoResource(R.raw.fvid1)
                .setImageResource(R.drawable.fpic1));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Cleaning")
                .addBullet("Place waterproof pad under patient")
                .addBullet("Put on gloves")
                .addBullet("Clean genital area with castile wipes or washcloth with warm water and soap.  Clean tip of penis first outward from meatus, than shaft of penis should be washed with downward strokes")
                .setVideoResource(R.raw.fvid2)
                .setImageResource(R.drawable.fpic2));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Open the Catheter Kit")
                .addBullet("Open kit using sterile procedure")
                .addBullet("Don sterile gloves using proper technique")
                .setVideoResource(R.raw.fvid3)
                .setImageResource(R.drawable.fpic3));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Prepare the Equipment")
                .addBullet("Remove tray from box and place on sterile field")
                .addBullet("Open lubricant and squeeze into tray well")
                .addBullet("Place fenestrated drape over penis")
                .addBullet("Inspect catheter and place end in lubricant")
                .setVideoResource(R.raw.fvid4)
                .setImageResource(R.drawable.fpic4));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Sterilizing")
                .addBullet("Open cleansing swabs (unless already open)")
                .addBullet("Lift penis with non-dominant hand at a 60-90* angle. This hand will remain in position until the catheterization is complete")
                .addBullet("Using dominant hand clean the meatus with swabs in circular motion.  Complete this three times. (This step will be simulated, do not touch model with swabs)")
                .setVideoResource(R.raw.fvid5)
                .setImageResource(R.drawable.fpic5)
                .setTextSize(10));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Inset Catheter")
                .addBullet("Pick up the catheter near the tip and ensure it is well lubricated")
                .addBullet("While holding the penis firmly, advance catheter to bifurcation")
                .setVideoResource(R.raw.fvid6)
                .setImageResource(R.drawable.fpic6));

        cardInfos.add(new CardInfo(cardInfos.size(), R.layout.left_column_layout)
                .setHeader("Finish Task")
                .addBullet("Hold catheter securely at meatus with non-dominant hand, use dominant hand to secure water-filled syringe to luer-lock")
                .addBullet("Push plunger to inflate balloon")
                .addBullet("Disconnect syringe")
                .addBullet("Gently pull back on catheter to ensure it is securely seated in the bladder")
                .setVideoResource(R.raw.fvid7)
                .setImageResource(R.drawable.fpic7)
                .setTextSize(10));
    }
}
