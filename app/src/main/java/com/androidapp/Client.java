package com.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorI;
import com.zeroc.Ice.Util;
import com.zeroc.IceInternal.Instance;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import mp3App.*;
import metaServer.*;

public class Client extends AppCompatActivity {

	//FunctionPrx function;
	msFunctionPrx function;
	Communicator ic;
	AudioManager manager;

	SimpleExoPlayer mediaPlayer;
	PlayerView playerView;
	ListView listView;
	TextView highlitedMusic;

	Button recordButton;
	boolean isRecording;
	AudioRecord recorder;
	Thread recordingThread;
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	String selectedMusic;
	boolean playWhenReady = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);

		playerView = findViewById(R.id.video_view);
		listView = (ListView) findViewById(R.id.listView);
		highlitedMusic = (TextView) findViewById(R.id.tv);
		selectedMusic = "";

		recordButton = findViewById(R.id.recordButton);
		isRecording = false;

		ic = Util.initialize();
		function = msFunctionPrx.checkedCast(ic.stringToProxy("server:tcp -h 10.0.2.2 -p 4061"));

		initializeMusicList();
	}

	private void initializePlayer() {

		mediaPlayer = ExoPlayerFactory.newSimpleInstance(
				new DefaultRenderersFactory(this),
				new DefaultTrackSelector(), new DefaultLoadControl());

		playerView.setPlayer(mediaPlayer);
		mediaPlayer.setPlayWhenReady(false);
		mediaPlayer.seekTo(0);

		Uri uri = Uri.parse(getString(R.string.url));
		MediaSource mediaSource = buildMediaSource(uri);
		mediaPlayer.prepare(mediaSource, true, false);


	}


	@Override
	public void onStart() {
		super.onStart();
		initializePlayer();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mediaPlayer == null) {
			initializePlayer();
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		releasePlayer();
	}

	@Override
	public void onStop() {
		//function.stopMusicAsync();
		function.parse("","stop");
		super.onStop();
		releasePlayer();
	}

	private void releasePlayer() {
		if (mediaPlayer != null) {
			playWhenReady = mediaPlayer.getPlayWhenReady();
			mediaPlayer.release();
			mediaPlayer = null;
			//function.stopMusic();
			function.parse("","stop");
		}
	}

	private MediaSource buildMediaSource(Uri uri) {
		return new ExtractorMediaSource.Factory(
				new DefaultHttpDataSourceFactory("exoplayer-codelab")).
				createMediaSource(uri);
	}

	// Create the music list based on running servers
	public void initializeMusicList() {

		List<String> musicList = new ArrayList<>();
		musicList = Arrays.asList(function.receive());
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
				(this, android.R.layout.simple_list_item_1, musicList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent){

				View view = super.getView(position, convertView, parent);
				TextView tv = (TextView) view.findViewById(android.R.id.text1);
				tv.setTextColor(getResources().getColor(R.color.colorText));
				return view;
			}

		};
		listView.setAdapter(arrayAdapter);

		// Handle events while clicking on a song in the item list
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedMusic = (String) parent.getItemAtPosition(position);
				highlitedMusic.setText("Song selected : " + selectedMusic + ", hit the play button!");
				//function.stopMusicAsync();
				//function.playMusicAsync(selectedMusic);
				function.parse("stop", "json");
				function.parse("play " + selectedMusic.substring(0, selectedMusic.length() - 4), "json");
				initializePlayer();
				mediaPlayer.setPlayWhenReady(true);

			}
		});

		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getApplicationContext(),
						"click",
						Toast.LENGTH_SHORT).show();
				startSpeechToText();					// This triggers the google speech to text next line needs to be commented (line 243)
				//function.parse("stop test1","json");
				initializePlayer();
				mediaPlayer.setPlayWhenReady(true);
			}
		});
	}

	public void startSpeechToText() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"Speak something...");
		try {
			startActivityForResult(intent, 666);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(),
					"Sorry! Speech recognition is not supported in this device.",
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case 666: {
				if (resultCode == RESULT_OK && null != data) {
					ArrayList<String> result = data
							.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					String text = result.get(0);
					System.out.println("RECORDED : " + text);
					function.parseAsync(text,"json");
				}
				break;
			}
		}
	}
}