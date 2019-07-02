package com.example.kech.servicebestpractice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;



public class DownloadService extends Service {
    private DownLoadTask downloadTask;
    private String downloadUrl;
    private static boolean flag = false;



    private DownloadBinder mBinder = new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    //DownloadBinder可以实现和活动进行通信
    class DownloadBinder extends Binder{
        //开始下载
        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownLoadTask(listener);
                downloadTask.execute(downloadUrl);
                Log.d("TAG1","运行1");
                //使得服务变为前台服务
                startForeground(1,getNotification("正在下载... ",0));
                Toast.makeText(DownloadService.this,"正在下载中...",Toast.LENGTH_SHORT).show();
                Log.d("TAG2","运行2");
            }
        }
        //暂停下载
        public void pauseDownload(){
            if(downloadTask != null){
                downloadTask.pauseDownlad();
            }
        }
        //取消下载
        public void cancelDownload(){
            if(downloadTask != null){
                downloadTask.cancelDownload();
            }else{
                if (downloadUrl !=null){
                    //取消下载时需要将文件删除，并将通知关闭
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file=new File(directory+fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this,"Cancled",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    //创建一个DownloadListener的匿名类实例，并在匿名类中实现了5个方法
    private DownloadListener listener=new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            //调用getNotification()方法构建一个用于显示下载进度的通知
            getNotificationManager().notify(1,getNotification("正在下载中",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Success",-1));
            Toast.makeText(DownloadService.this,"Download Success",Toast.LENGTH_SHORT).show();
            flag=true;
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            //下载失败时将前台服务通知关闭，并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            //Toast.makeText(DownloadService.this,"Download Failed",Toast.LENGTH_SHORT).show();
            if(!flag){
                Toast.makeText(DownloadService.this,"正在申请继续下载",Toast.LENGTH_SHORT).show();
                mBinder.startDownload(downloadUrl);
            }
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            flag=true;
            Toast.makeText(DownloadService.this,"Paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask=null;
            flag=true;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"Canceled",Toast.LENGTH_SHORT).show();
        }
    };



    private NotificationManager getNotificationManager() {
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }
    private Notification getNotification(String title,int progress){


        Log.d("TAG3","运行3");
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this);
        //builder.setPriority(NotificationCompat.PRIORITY_MIN);
        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        Log.d("TAG4","运行4");

        //TargetSDK>=26必须加以下代码
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("001","my_channel",NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true); //是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
            getNotificationManager().createNotificationChannel(channel);
            builder.setChannelId("001");
        }

        if(progress>0){
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);

        }
        return builder.build();

    }



}
