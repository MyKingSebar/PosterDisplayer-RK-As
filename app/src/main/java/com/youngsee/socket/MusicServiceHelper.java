package com.youngsee.socket;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.youngsee.posterdisplayer.PosterMainActivity;


public class MusicServiceHelper {

    private static MusicService musicService;
    private static MusicServiceConnection musicServiceConnection;

    private static String url;

    private static boolean isBindMusicService;// 是否绑定MusicService

    /**
     * 绑定MusicService
     *
     * @param activity
     */
    public static void doBindMusicService() {
        doBindMusicService( null);
    }

    /**
     * 绑定MusicService
     *
     * @param activity
     * @param url
     */
    public static void doBindMusicService(String url) {
        MusicServiceHelper.url = url;
        isBindMusicService = false;
        musicServiceConnection = new MusicServiceConnection();
        Intent intent = new Intent(PosterMainActivity.INSTANCE, MusicService.class);
        try {
            isBindMusicService = PosterMainActivity.INSTANCE.bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            isBindMusicService = false;
            e.printStackTrace();
        }
    }

    /**
     * 解除绑定MusicService
     */
    public static void doUnBindMusicService() {
        if (musicServiceConnection != null) {
            if (isBindMusicService) {
                try {
                    PosterMainActivity.INSTANCE.unbindService(musicServiceConnection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            musicService.stopSelf();
            url = null;
        }
    }

    /**
     * 播放
     */
    public static void doPlay() {
            if (musicService != null) {
                musicService.play(url);
            }
    }

    /**
     * 暂停
     */
    public static void doPause() {
        if (musicService != null) {
            musicService.pause();
        }
    }

    /**
     * 恢复播放(暂停后用)
     */
    public static void doReplay() {
        if (musicService != null) {
            musicService.start();
        }
    }

    /**
     * 停止
     */
    public static void doStop() {
        if (musicService != null) {
            musicService.stop();
        }
    }

    /**
     * 选择进度
     *
     * @param second
     */
    public static void doSeekTo(int second) {
        if (musicService != null) {
            musicService.seekTo(second);
        }
    }

    /**
     * 是否正在播放
     *
     * @return
     */
    public static boolean isPlaying() {
        if (musicService != null) {
            return musicService.isPlaying();
        }
        return false;
    }

    /**
     * 设置链接
     *
     * @param url
     */
    public static void setUrl(String url) {
        MusicServiceHelper.url = url;
        doPlay();
    }

    /**
     * 获取链接
     *
     * @return
     */
    public static String getUrl() {
        return url;
    }

    static class MusicServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) service;
            musicService = musicBinder.getService();
            doPlay();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}
