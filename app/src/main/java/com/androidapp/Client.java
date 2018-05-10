package com.androidapp;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import com.zeroc.Ice.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mp3App.*;
import metaServer.*;

public class Client extends AppCompatActivity {

	//FunctionPrx function;
	msFunctionPrx function;
	Communicator ic;

	SimpleExoPlayer mediaPlayer;
	PlayerView playerView;
	ListView listView;
	TextView highlitedMusic;

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
		function.parse("", "stop");
		super.onStop();
		releasePlayer();
	}

	private void releasePlayer() {
		if (mediaPlayer != null) {
			playWhenReady = mediaPlayer.getPlayWhenReady();
			mediaPlayer.release();
			mediaPlayer = null;
			//function.stopMusic();
			function.parse("", "stop");
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
				function.parse(selectedMusic, "stop");
				function.parse(selectedMusic, "play");
				initializePlayer();
				mediaPlayer.setPlayWhenReady(true);
			}
		});
	}
}