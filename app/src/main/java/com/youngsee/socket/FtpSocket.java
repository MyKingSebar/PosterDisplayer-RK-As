package com.youngsee.socket;

import com.youngsee.common.Contants;
import com.youngsee.webservices.SocketServer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by WeiYanGeorge on 17/7/31.
 */

public class FtpSocket {
    /**
     * 服务器名
     */
    private String hostName;

    /**
     * 端口号
     */
    private int serverPort;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * FTP连接
     */
    private FTPClient ftpClient;

    public FtpSocket(){
        this.hostName = "192.168.102.23";
        this.serverPort = 21;
        this.userName = "administrator";
        this.password = "Dhcc@123";
        this.ftpClient = new FTPClient();
    }

    /**
     * 下载单个文件, 可实现断点下载
     *
     * @param serverPath
     *             Ftp目标及文件路径
     * @param localPath
     *
     * @param fileName
     *
     * @param listener
     *        监听器
     *
     */

    public void downloadSingleFile(String serverPath, String localPath, String fileName, DownLoadProgressListener listener) throws Exception{

        //打开FTP服务
        try{
            this.openConnect();
            listener.onDownLoadProgress(Contants.FTP_CONNECT_SUCCESS, 0, null);
        }catch (IOException e1){
            e1.printStackTrace();
            listener.onDownLoadProgress(Contants.FTP_CONNECT_FAILED, 0, null);
            return;
        }

        FTPFile[] files = ftpClient.listFiles(serverPath);

        if (files.length == 0){
            listener.onDownLoadProgress(Contants.FTP_FILE_NOEXIST, 0, null);
            this.closeConnect();
            return;
        }
        File mkFile = new File(localPath);
        if (!mkFile.exists()) {
            mkFile.mkdirs();
        }

        localPath = localPath + fileName;
        //接着判断下载的文件是否能断点下载
        long serverSize = files[0].getSize(); // 获取远程文件的长度
        File localFile = new File(localPath);
        long localSize = 0;
        if (localFile.exists()){
            localSize = localFile.length();// 如果本地文件存在, 获取本地文件的长度
            if (localSize >= serverSize){
                File file = new File(localPath);
                file.delete();
            }
        }

        // 进度
        long step = serverSize / 100;
        long process = 0;
        long currentSize = 0;
        // 开始准备下载文件
        OutputStream out = new FileOutputStream(localFile, true);
        ftpClient.setRestartOffset(localSize);
        InputStream input = ftpClient.retrieveFileStream(serverPath);
        byte[] b = new byte[1024];
        int length = 0;
        while ((length = input.read(b)) != -1){
            out.write(b, 0, length);
            currentSize = currentSize+ length;
            if (currentSize / step != process){
                process = currentSize / step;
                if (process %5 == 0){
                    listener.onDownLoadProgress(Contants.FTP_FILE_DOWNLOADING, process, null);
                }
            }
        }
        out.flush();
        out.close();
        input.close();
        if (ftpClient.completePendingCommand()){
            listener.onDownLoadProgress(Contants.FTP_DOWNLOAD_SUCCESS, 0 ,new File(localPath));
        } else{
            listener.onDownLoadProgress(Contants.FTP_DOWNLOAD_FAILED, 0, null);
        }

        // 下载完成之后关闭连接
        this.closeConnect();
        listener.onDownLoadProgress(Contants.FTP_DISCONNECT_SUCCESS,0, null);

        return;

    }

    /**
     * 打开FTP服务
     *
     * @throws IOException
     */

    public void openConnect() throws IOException{
        //中文转码
        ftpClient.setControlEncoding("UTF-8");
        int reply; //服务器相应值
        //连接至服务器
        ftpClient.connect(hostName,serverPort);
        ftpClient.setDefaultTimeout(15000);
        reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)){
            //断开连接
            ftpClient.disconnect();
            throw new IOException("connect fail: " + reply);
        }
        //登录到服务器
        ftpClient.login(userName, password);
        //获取响应值
        reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)){
            //断开连接
            ftpClient.disconnect();
            throw new IOException("connect fail: " + reply);
        }else {
            //获取登录信息
            FTPClientConfig config = new FTPClientConfig(ftpClient.getSystemType().split(" ")[0]);
            config.setServerLanguageCode("zh");
            ftpClient.configure(config);
            //使用被动模式为默认
            ftpClient.enterLocalPassiveMode();
            // 二进制文件支持
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        }
    }

    /**
     * 关闭FTP服务
     */
    public void closeConnect() throws IOException{
        if (ftpClient != null){
            // 退出FTP
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }


    /**
     *
     * 下载进度监听
     */
    public interface DownLoadProgressListener {
        public void onDownLoadProgress(String currentStep, long downProcess, File file);
    }



}
