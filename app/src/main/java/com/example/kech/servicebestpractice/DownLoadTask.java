package com.example.kech.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public  class DownLoadTask extends AsyncTask<String ,Integer,Integer> {
    public static final int TYPE_SUCCESS=0;//下载成功
    public static final int TYPE_FAILED=1;//下载失败
    public static final int TYPE_PAUSED=2;//暂停下载
    public static final int TYPE_CANCELED=3;//取消下载

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;
    public DownLoadTask(DownloadListener listener){
        this.listener=listener;
    }

    public DownLoadTask() {

    }

    /**
     *doInBackground()方法用于在后台执行具体的下载逻辑
     * @param params
     * @return
     */
    @Override
    protected Integer doInBackground(String... params){
        InputStream is=null;
        RandomAccessFile savedFile = null;
        File file=null;
        try{
            Log.d("TAG5","运行5");
            long downloadedLength = 0;
            String downloadUrl = params[0];
            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //下载到SD卡的Download目录下
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory+fileName);
            //判断是否已经存在要下载的文件，如果存在的话则读取已经下载的字节数，实现断点续传的功能
            if(file.exists()){
                downloadedLength=file.length();
            }
            //获取待下载文件的总长度
            long contentLenth = getContentLength(downloadUrl);

            if(contentLenth == 0){
                return TYPE_FAILED;//若文件长度等于0，说明文件有问题
            }else if(contentLenth == downloadedLength){
                return TYPE_SUCCESS;//已下载的字节和文件总字节相等，说明已经下载完成了

            }
            //使用OkHttp来发送一条网络请求，已经下载部分无需再下载
            OkHttpClient client=new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response= null;

            response = client.newCall(request).execute();

            if (response != null) {
                is = response.body().byteStream();

                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);//跳过已经下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len = is.read()) != 1){
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else  if (isPaused){
                        return TYPE_PAUSED;
                    }else {
                        //实时计算当前的下载进度，调用publicProgress()方法进行通知
                        total +=len;
                        savedFile.write(b,0,len);
                        int progress = (int)((total + downloadedLength)*100/contentLenth);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
            }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(is != null){
                    is.close();
                }
                if (savedFile != null){
                    savedFile.close();
                }
                if (isCanceled && file !=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d("TAG6","运行6");
        return TYPE_FAILED;

    }

    /**
     *onProgressUpdate()方法用于在界面上更新当前的下载进度
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values){
        int progress = values[0];
        if(progress > lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }

    /**
     * onPostExecute()方法用户通知最终的下载结果
     * @param status
     */
    @Override
    protected void onPostExecute(Integer status){
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }
    public void pauseDownlad(){
        isPaused = true;
    }
    public void cancelDownload(){
        isCanceled = true;

    }



    protected long  getContentLength(String downloadUrl) throws Exception{
        OkHttpClient client = new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response=client.newCall(request).execute();
        if((response != null) && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }

        return 0;
    }

}
