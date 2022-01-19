package com.deepakb.app.filetile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import static android.widget.Toast.LENGTH_LONG;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FtpServerFactory sFactory=new FtpServerFactory();
    ListenerFactory lFactory=new ListenerFactory();
    PropertiesUserManagerFactory userFactory=new PropertiesUserManagerFactory();
    FtpServer ftpServer;

    @SuppressLint("AuthLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);
       Toast.makeText(this, "TestMain", LENGTH_LONG).show();
       ftpServer = sFactory.createServer();
        try {
            if(isHotspotOn(this)||isWifiConnected(this)) {
                manageServer();
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Connect to a Wifi Network or Turn on Hotspot to continue");
                builder.setTitle("Error");
                builder.setPositiveButton("Ok", ((dialogInterface, i) -> dialogInterface.dismiss()));
                builder.show();
            }
        } catch (IllegalAccessException | InvocationTargetException i) {
            i.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            ftpServer.stop();
        } catch (Exception e) {
            Log.e("Server Close Error", Objects.requireNonNull(e.getCause()).toString());
        }
        super.onDestroy();
    }

    private void manageServer(){
        if(ftpServer.isStopped()){
            lFactory.setPort(2222);
            sFactory.addListener("default", lFactory.createListener());

            File f= new File(Environment.getExternalStorageDirectory().getPath()+ "/users.properties");
            if(!f.exists()){
                try{
                    f.createNewFile();
                }catch(IOException i){
                    i.printStackTrace();
                }
            }
            userFactory.setFile(f);
            userFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
            UserManager u = userFactory.createUserManager();
            BaseUser bu = new BaseUser();
            bu.setName("Admin");
            bu.setPassword("2212");
            bu.setHomeDirectory(Environment.getExternalStorageDirectory().getPath());

            List<Authority> lAuth = new ArrayList<>();
            lAuth.add(new WritePermission());
            bu.setAuthorities(lAuth);
            try{
                u.save(bu);
            }catch(FtpException fe){
                fe.printStackTrace();
            }

            sFactory.setUserManager(u);

            Map<String, Ftplet> map = new HashMap<>();
            map.put("miaFtplet", new Ftplet() {
                @Override
                public void init(FtpletContext ftpletContext) throws FtpException {

                }

                @Override
                public void destroy() {

                }

                @Override
                public FtpletResult beforeCommand(FtpSession ftpSession, FtpRequest ftpRequest) throws FtpException, IOException {
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult afterCommand(FtpSession ftpSession, FtpRequest ftpRequest, FtpReply ftpReply) throws FtpException, IOException {
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onConnect(FtpSession ftpSession) throws FtpException, IOException {
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onDisconnect(FtpSession ftpSession) throws FtpException, IOException {
                    return FtpletResult.DEFAULT;
                }
            });

            sFactory.setFtplets(map);

            try{
                ftpServer.start();
                EditText ipa=(EditText)findViewById(R.id.IPAdd);
                ipa.setText(String.format("ftp://%s:%s", ipAddress(this), lFactory.getPort()));
            } catch (FtpException e) {
                e.printStackTrace();
            }
        }
        else if(ftpServer.isSuspended()) {
            ftpServer.resume();
        }
        else {
            ftpServer.suspend();
        }
    }

    private boolean isHotspotOn(Context context) throws IllegalAccessException, InvocationTargetException{
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method m;
        try{
            m = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            m.setAccessible(true);
            return (Boolean) m.invoke(wifiManager);
        }catch(NoSuchMethodException n){
            n.printStackTrace();
        }
        return false;
    }

    private boolean isWifiConnected(Context context){
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager!=null;
        if(wifiManager.isWifiEnabled()){
            WifiInfo wifiInfo=wifiManager.getConnectionInfo();
            return(wifiInfo.getNetworkId()!=-1);
        }

        return false;
    }

    private String ipAddress(Context context){
        return getIPAddress(true);
    }

    public static String getIPAddress(boolean ipv4){
        try{
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for(NetworkInterface inf : interfaces){
                List<InetAddress> adds = Collections.list(inf.getInetAddresses());
                for(InetAddress add: adds) {
                    if (!add.isLoopbackAddress()) {
                        String sAdd = add.getHostAddress();
                        boolean isIPv4 = sAdd.indexOf(':') < 0;

                        if (ipv4) {
                            if (isIPv4)
                                return sAdd;
                        } else {
                            if (!isIPv4) {
                                int del = sAdd.indexOf('%');
                                return del < 0 ? sAdd.toUpperCase() : sAdd.substring(0, del).toLowerCase();
                            }
                        }
                    }
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }
}
