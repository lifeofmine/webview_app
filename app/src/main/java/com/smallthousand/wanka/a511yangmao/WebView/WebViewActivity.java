package com.smallthousand.wanka.a511yangmao.WebView;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.allenliu.versionchecklib.v2.callback.CustomVersionDialogListener;
import com.allenliu.versionchecklib.v2.callback.RequestVersionListener;
import com.smallthousand.wanka.a511yangmao.R;
import com.smallthousand.wanka.a511yangmao.widget.PtrClassicFrameLayout;
import com.smallthousand.wanka.a511yangmao.widget.PtrDefaultHandler;
import com.smallthousand.wanka.a511yangmao.widget.PtrFrameLayout;
import com.smallthousand.wanka.a511yangmao.widget.PtrHandler;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity {
    //声明引用
    private WebView webViews;
    private PtrClassicFrameLayout mPtrFrame;
    private String url;
    public static final String VERSION = "1.2";
    public String status = "", downloadUrl = "", serverVersion = "";

    //文件上传
    private static final String TAG = WebViewActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;

    //select whether you want to upload multiple files (set 'true' for yes)
    private boolean multiple_files = false;

    //apk地址
    private String fileName;
    //广播接收者
    MyReceiver receiver = new MyReceiver();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //checking if response is positive
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null || intent.getData() == null){
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if(multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @SuppressWarnings({"findViewById", "RedundantCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        //获取控件对象
        webViews = (WebView) findViewById(R.id.WV_Id);
        mPtrFrame= (PtrClassicFrameLayout) findViewById(R.id.rotate_header_web_view_frame);
        initView();
        initApp();
        //注册广播
        IntentFilter intentFilter = new IntentFilter(DownloadService.BROADCAST_ACTION);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    private void initApp() {
        AllenVersionChecker
                .getInstance()
                .requestVersion()
                .setRequestUrl("http://www.92wanka.com/app/yangmao-update?version="+VERSION)
                .request(new RequestVersionListener() {
                    @Nullable
                    @Override
                    public UIData onRequestVersionSuccess(String result) {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            status = jsonObject.getString("status");
                            downloadUrl = jsonObject.getString("url");
                            serverVersion = jsonObject.getString("version");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (status.equals("success") && Float.valueOf(serverVersion) > Float.valueOf(VERSION)) {
                            return UIData
                                    .create()
                                    .setContent(getString(R.string.updatecontent))
                                    .setDownloadUrl(downloadUrl);
                        }

                        return null;
                    }

                    @Override
                    public void onRequestVersionFailure(String message) {
                        Toast.makeText(WebViewActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCustomVersionDialogListener(createCustomDialog())
                .setForceRedownload(true)
                .executeMission(WebViewActivity.this);
    }

    private CustomVersionDialogListener createCustomDialog() {
        return (context, versionBundle) -> {
            BaseDialog baseDialog = new BaseDialog(context, R.style.BaseDialog, R.layout.custom_dialog_layout);
            TextView textView = baseDialog.findViewById(R.id.tv_msg);
            textView.setText(versionBundle.getContent());
            baseDialog.setCanceledOnTouchOutside(true);
            return baseDialog;
        };
    }

    /**
     * 初始化页面
     */
    private void initView(){
        WebSettings webSetting = webViews.getSettings();
        //设置JavaScrip
        webSetting.setJavaScriptEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setLoadWithOverviewMode(true);
        webSetting.setDomStorageEnabled(true);

        //文件上传
        if(Build.VERSION.SDK_INT >= 21){
            webSetting.setMixedContentMode(0);
            webViews.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT >= 19){
            webViews.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else {
            webViews.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        //访问首页
        webViews.loadUrl("http://app.511yangmao.com/Home/Public/login.html");
        //url = "http://dawndew.me/user/login?isAppStatus=1";

        //设置在当前WebView继续加载网页
        webViews.setWebViewClient(new MyWebViewClient());
        webViews.setWebChromeClient(new MyWebChromeClient());
        webViews.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                downloadBySystem(url, contentDisposition, mimeType);
            }
        });

        //下拉刷新监听
        mPtrFrame.setLastUpdateTimeRelateObject(this);
        mPtrFrame.setPtrHandler(new PtrHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, webViews, header);
            }
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                updateData();
            }
        });
        mPtrFrame.setResistance(1.7f);
        mPtrFrame.setRatioOfHeaderHeightToRefresh(1.2f);
        mPtrFrame.setDurationToClose(200);
        mPtrFrame.setDurationToCloseHeader(1000);
        mPtrFrame.setPullToRefresh(false);
        mPtrFrame.setKeepHeaderWhenRefresh(true);

//        mPtrFrame.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mPtrFrame.autoRefresh();
//            }
//        }, 1000);
    }

    private void downloadBySystem(String url, String contentDisposition, String mimeType) {
        //DownloadService继承IntentService，一会说明
        Intent serviceIntent = new Intent(WebViewActivity.this, DownloadService.class);
        //写入你的apk下载地址，下面这个地址只是模拟的
        serviceIntent.setData(Uri.parse(url));
        fileName  = URLUtil.guessFileName(url, contentDisposition, mimeType);
        serviceIntent.putExtra("fileName", fileName);
        //开启服务，不要写成了startActivity(serviceIntent);
        startService(serviceIntent);
//        // 指定下载地址
//        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
//        request.allowScanningByMediaScanner();
//        // 设置通知的显示类型，下载进行时和完成后显示通知
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        // 设置通知栏的标题，如果不设置，默认使用文件名
////        request.setTitle("This is title");
//        // 设置通知栏的描述
////        request.setDescription("This is description");
//        // 允许在计费流量下下载
//        request.setAllowedOverMetered(true);
//        // 允许该记录在下载管理界面可见
//        request.setVisibleInDownloadsUi(true);
//        // 允许漫游时下载
//        request.setAllowedOverRoaming(true);
//        // 允许下载的网路类型
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
//        // 设置下载文件保存的路径和文件名
//        fileName  = URLUtil.guessFileName(url, contentDisposition, mimeType);
//        //log.debug("fileName:{}", fileName);
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
////        另外可选一下方法，自定义下载路径
////        request.setDestinationUri()
////        request.setDestinationInExternalFilesDir()
//        final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//        // 添加一个下载任务
//        downloadManager.enqueue(request);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            //获取权限
            try {
                Runtime.getRuntime().exec("chmod 777" + file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent = new Intent(Intent.ACTION_VIEW);
            //如果设置，这个活动将成为这个历史堆栈上的新任务的开始
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //判读版本是否在7.0以上
            if (Build.VERSION.SDK_INT >= 24) {
                //7.0以上的版本
                Uri apkUri = FileProvider.getUriForFile(context, "com.smallthousand.wanka.a511yangmao.WebView.FileProvider", file);
                //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                //7.0以下的版本
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            startActivity(intent);
        }
    }

    /**
     * 刷新加载数据
     */
    private void updateData() {
        webViews.loadUrl(webViews.getUrl());
    }

    /**
     * webView Client
     */
    class MyWebViewClient extends WebViewClient{
        ProgressDialog progressDialog;
        @Override  //WebView代表是当前的WebView
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            url = request.getUrl().toString();
            //打开拨号界面
            try {
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                //打开支付宝APP
                if (url.toLowerCase().startsWith("alipays://platformapi")){
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                //微信H5支付核心代码
                if (url.startsWith("weixin://wap/pay?")) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                return true;
            }

            //微信H5支付初始支付链接处理
            if (url.startsWith("https://wx.tenpay.com")) {
                Map<String, String> extraHeaders = new HashMap<>();
                extraHeaders.put("Referer", "http://www.511qian.com");
                view.loadUrl(url, extraHeaders);
                return true;
            }

            if (!url.toLowerCase().startsWith("http")) {
                if(view.canGoBack()){
                    view.goBack();
                } else {
                    view.loadUrl("http://www.92wanka.com/user/login?isAppStatus=1");
                }
                return true;
            }
            //表示在当前的WebView继续打开网页
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            //跳转支付宝app错误
            if (failingUrl.toLowerCase().startsWith("alipays://platformapi")) {
                view.loadUrl("http://www.92wanka.com/no-alipays?isAppStatus=1");
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //super.onPageStarted(view, url, favicon);
            //页面加载loading动画开启
            if (progressDialog == null) {
                // in standard case YourActivity.this
                progressDialog = new ProgressDialog(WebViewActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.show();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //页面加载loading动画关闭
            try{
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }catch(Exception exception){
                exception.printStackTrace();
            }

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    mPtrFrame.refreshComplete();
                }
            }, 1000);
        }
    }

    /**
     * Chrome Web Client
     */
    class MyWebChromeClient extends WebChromeClient{
        //文件上传
        /*
         * openFileChooser is not a public Android API and has never been part of the SDK.
         */
        //handling input[type="file"] requests for android API 16+
        @SuppressLint("ObsoleteSdkInt")
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUM = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            if (multiple_files && Build.VERSION.SDK_INT >= 18) {
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
        }

        //文件上传
        //handling input[type="file"] requests for android API 21+
        @SuppressLint("InlinedApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (file_permission()) {
                String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

                //checking for storage permission to write images for upload
                if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebViewActivity.this, perms, FCR);

                    //checking for WRITE_EXTERNAL_STORAGE permission
                } else if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, FCR);

                    //checking for CAMERA permissions
                } else if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.CAMERA}, FCR);
                }
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(WebViewActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                if (multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }else{
                return false;
            }
        }

        @Override //监听加载进度
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }
        @Override//接受网页标题
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            //把当前的Title设置到Activity的title上显示
            setTitle(title);
        }
    }

    //callback reporting if error occurs
    public class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), "Failed loading app!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean file_permission(){
        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        }else{
            return true;
        }
    }

    //creating new image file here
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    //back/down key handling
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK:
                    if(webViews.canGoBack()){
                        webViews.goBack();
                    }else{
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
