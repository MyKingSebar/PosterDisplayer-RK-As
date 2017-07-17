package com.youngsee.multicast;

public class MulticastCommon
{
	/***********************************************************************************
	 * 同步状态关键字 (1 Byte) | 同步域ID (1 Byte ) | 同步域内容长度 (1 Byte) | 同步域内容 (N Byte) *
	 ***********************************************************************************/
	// 同步指示
    public final static int     MC_VALUE_PROGSYNC_CLOSE              = 0;    // 关闭同步
    public final static int     MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER  = 1;    // 组长
    public final static int     MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS = 2;    // 组员
    
    // 同步状态关键字
    public final static byte    MC_SYNCFLAG_CLOSE            = 0x00;    // 关闭同步
    public final static byte    MC_SYNCFLAG_LOAD_PROGRAM     = 0x01;    // 加载节目
    public final static byte    MC_SYNCFLAG_LOAD_MEDIA       = 0x02;    // 加载素材
    public final static byte    MC_SYNCFLAG_PLAY_MEDIA       = 0x03;    // 播放素材
    public final static byte    MC_SYNCFLAG_START_PLAY_VIDEO = 0x04;    // 开始播放视频
    
    // 同步域ID
    public final static byte   MC_FILED_PROGRAMTYPE_ID      = 0x01; // 同步域：当前节目类型(IDLE、NORMAL、ENGRENT)
    public final static byte   MC_FILED_PROGRAMID_ID        = 0x02; // 同步域：当前节目id
    public final static byte   MC_FILED_PGM_VERIFYCODE_ID   = 0x03; // 同步域：当前节目校验码 
    public final static byte   MC_FILED_WND_NAME_ID         = 0x04; // 同步域：窗口名称
    public final static byte   MC_FILED_MEDIA_FULLNAME_ID   = 0x05; // 同步域：素材名称
    public final static byte   MC_FILED_MEDIA_SOURCE_ID     = 0x06; // 同步域：素材来源：(File, Url, Rss) 本地文件或网络文件
    public final static byte   MC_FILED_MEDIA_CODE_ID       = 0x07; // 同步域：素材校验码
    public final static byte   MC_FILED_MEDIA_POSITION_ID   = 0x08; // 同步域：素材位置(加载素材时，表示素材在列表中的位置，若是视频播放过程中，表示播放的位置)
    
    public final static int    MC_BYTES_BEFORE_LEN           = 17 + 1;
    public static int          DEFAULT_SYNC_TIMES_DIFFERENCE = 100;
}
