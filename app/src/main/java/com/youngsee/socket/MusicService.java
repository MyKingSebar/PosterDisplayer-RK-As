package com.youngsee.socket;

/**
 * Created by WeiYanGeorge on 17/7/31.
 */

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.youngsee.common.FileUtils;
import com.youngsee.webservices.SocketServer;

import java.io.FileDescriptor;
import java.io.IOException;


public class MusicService extends Service {

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private IBinder binder = new MusicBinder();

    private MediaPlayer mediaPlayer;// 播放器

    private int playcount = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    /**
     * 从新播放
     *
     * @param url
     */
    public void play(final String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if (playcount == 1){
                        start();
                    }else {
                        stop();
                        playcount = 0;
                        FileUtils.delFile(url);
                        SocketServer.isLoadingVoiceProgram =false;
                    }
                }
            });
            seekTo(0);
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        mediaPlayer.pause();
    }

    /**
     * 播放
     */
    public void start() {
        playcount++;
        mediaPlayer.start();
    }

    /**
     * 停止
     */
    public void stop() {
        mediaPlayer.stop();
    }

    /**
     * 选择进度
     *
     * @param second
     */
    public void seekTo(int second) {
        mediaPlayer.seekTo(second * 1000);
    }

    /**
     * 是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
