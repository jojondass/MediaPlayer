package fr.doranco.mediaplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String RAW_SONG = "machanson";
    private final static int TIME_STEP = 5000; // en millisecondes

    private TextView textViewTitle, textViewSongTitle, textViewMaxDuration, textViewCurrentPosition;
    private SeekBar seekBar;
    private ImageView imageViewRewind, imageViewPlay, imageViewPause, imageViewStop, imageViewForward;
    private Button buttonLoadSong;

    private MediaPlayer mediaPlayer;
    private Handler threadHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        textViewMaxDuration = findViewById(R.id.textViewMaxDuration);
        textViewCurrentPosition = findViewById(R.id.textViewCurrentPosition);
        seekBar = findViewById(R.id.seekBar);
        imageViewRewind = findViewById(R.id.imageViewRewind);
        imageViewPlay = findViewById(R.id.imageViewPlay);
        imageViewPause = findViewById(R.id.imageViewPause);
        imageViewStop = findViewById(R.id.imageViewStop);
        imageViewForward = findViewById(R.id.imageViewForward);
        buttonLoadSong = findViewById(R.id.btnLoadSong);

        mediaPlayer = new MediaPlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();

        textViewSongTitle.setText("Aucune chanson chargée.");
        textViewMaxDuration.setText("");
        textViewCurrentPosition.setText("");

        //pour empêcher de faire bouger le curseur du seekBar avec le doigt
        //seekBar.setEnabled(false);

        // on désactive tous les boutons de contrôle et on active le bouton de chargement de chanson
        initializeAllButtons(false);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        mediaPlayer.setAudioAttributes(audioAttributes);

    }

    public void loadSong(View view) {
        mediaPlayer.reset();
        boolean isSongLoaded = false;
        try {
            isSongLoaded = setMediaPlayerSong(RAW_SONG);
        } catch (Exception e) {
            String errorMessage = "Erreur technique lors du chargement de la chanson !";
            Log.e(TAG, errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
        if (!isSongLoaded) {
            String errorMessage = "Erreur lors du chargement de la chanson !";
            Log.e(TAG, errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            return;
        }
        int duration = mediaPlayer.getDuration();
        if (duration == 0) {
            textViewMaxDuration.setText("");
            textViewCurrentPosition.setText("");
            Toast.makeText(this, "La chanson chargée est vide !", Toast.LENGTH_LONG).show();
            return;
        }
        seekBar.setMax(duration);
        textViewSongTitle.setText("Titre : " + RAW_SONG);
        textViewMaxDuration.setText(millisecondsToString(duration));
        textViewCurrentPosition.setText(millisecondsToString(mediaPlayer.getCurrentPosition()));
        initializeButtonsAfterLoadSong();
        Toast.makeText(this, "Chanson chargée avec succès.", Toast.LENGTH_LONG).show();
    }

    public void play(View view) {
        if (mediaPlayer.isPlaying()) {
            return;
        }
        int duration = mediaPlayer.getDuration();
        int currentPosition = mediaPlayer.getCurrentPosition();
        if (currentPosition == 0) {
            seekBar.setMax(duration);
            textViewMaxDuration.setText(millisecondsToString(duration));
            textViewCurrentPosition.setText(millisecondsToString(currentPosition));
        }
        mediaPlayer.start();  // on démarre la chanson
        // on lance le thread qui va faire la MAJ du seekBar et du textViewCurrentPosition
        UpdateSeekBarThread thread = new UpdateSeekBarThread();
        threadHandler.postDelayed(thread, 100);
        initializeButtonsAfterPlay();
    }

    public void pause(View view) {
        if (!mediaPlayer.isPlaying()) {
            return;
        }
        mediaPlayer.pause();
        initializeButtonsAfterPause();
    }

    public void stop(View view) {
        mediaPlayer.stop();
//        mediaPlayer.pause();
//        mediaPlayer.seekTo(0);
        seekBar.setProgress(0);
        textViewCurrentPosition.setText(millisecondsToString(0));
        initializeButtonsAfterStop();
    }

    public void rewind(View view) {
        int currentPosition = mediaPlayer.getCurrentPosition();
        if (currentPosition > TIME_STEP) {
            mediaPlayer.seekTo(currentPosition - TIME_STEP);
        } else {
            mediaPlayer.seekTo(0);
        }
    }

    public void forward(View view) {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int maxDuration = mediaPlayer.getDuration();
        if (currentPosition + TIME_STEP < maxDuration) {
            mediaPlayer.seekTo(currentPosition + TIME_STEP);
        } else {
            mediaPlayer.seekTo(maxDuration);
        }
    }

    private void initializeAllButtons(boolean value) {
        enableImage(imageViewRewind, false);
        enableImage(imageViewPlay, false);
        enableImage(imageViewPause, false);
        enableImage(imageViewStop, false);
        enableImage(imageViewForward, false);
        buttonLoadSong.setEnabled(true);
    }

    private void initializeButtonsAfterLoadSong() {
        buttonLoadSong.setEnabled(false);
        enableImage(imageViewPlay, true);
    }

    private void initializeButtonsAfterPlay() {
        enableImage(imageViewPlay, false);
        enableImage(imageViewRewind, true);
        enableImage(imageViewPause, true);
        enableImage(imageViewStop, true);
        enableImage(imageViewForward, true);
    }

    private void initializeButtonsAfterPause() {
        enableImage(imageViewPlay, true);
        enableImage(imageViewRewind, false);
        enableImage(imageViewPause, false);
        enableImage(imageViewStop, true);
        enableImage(imageViewForward, false);
    }

    private void initializeButtonsAfterStop() {
        enableImage(imageViewPlay, false);
        enableImage(imageViewRewind, false);
        enableImage(imageViewPause, false);
        enableImage(imageViewStop, false);
        enableImage(imageViewForward, false);
        buttonLoadSong.setEnabled(true);
    }

    private void enableImage(ImageView imageView, boolean value) {
        imageView.setEnabled(value);
        if (value) {
            imageView.setBackgroundColor(getColor(R.color.btn_background_violet));
        } else {
            imageView.setBackgroundColor(getColor(R.color.btn_background_gray));
        }
    }

    private boolean setMediaPlayerSong(String resourceSongName) throws Exception {
        Context context = getApplicationContext();
        String packageName = context.getPackageName();

        int songId = context.getResources().getIdentifier(resourceSongName, "raw", packageName);
        if (songId == 0) {
            String errorMessage = "La chanson '" + resourceSongName + "' n'existe pas !";
            Log.e(TAG, errorMessage);
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            return false;
        }
        Uri uri = Uri.parse("android.resource://" + packageName + "/" + songId);
        Log.i(TAG, "Uri de la chanson = " + uri);
        mediaPlayer.setDataSource(context, uri);
        mediaPlayer.prepareAsync();
        Thread.sleep(500);
        return true;
    }

    private String millisecondsToString(int milliseconds) {
        //01h05mn29s
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds - hours * 3600 * 1000); // 1 heure = 3600 secondes (1 seconde = 1000 ms)
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds - (hours * 3600 + minutes * 60) * 1000);
        String hoursStr = hours < 10 ? "0".concat(String.valueOf(hours)):String.valueOf(hours);
        String minutesStr = minutes < 10 ? "0".concat(String.valueOf(minutes)):String.valueOf(minutes);
        String secondsStr = seconds < 10 ? "0".concat(String.valueOf(seconds)):String.valueOf(seconds);
        if (hours == 0) {
            return minutesStr.concat(":").concat(secondsStr);
        } else {
            return hoursStr.concat(":").concat(minutesStr).concat(":").concat(secondsStr);
        }
    }

    class UpdateSeekBarThread implements Runnable {

        @Override
        public void run() {
            if (mediaPlayer.isPlaying()) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                textViewCurrentPosition.setText(millisecondsToString(currentPosition));
                seekBar.setProgress(currentPosition);
                // on définit le délai du thread (ici : 100 ms)
                threadHandler.postDelayed(this, 100);
            }
        }
    }
}