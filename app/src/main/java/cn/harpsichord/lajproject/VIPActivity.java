package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class VIPActivity extends AppCompatActivity {

    private static String url;
    private static String secret;
    private static String fullUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip);

        String filePath = this.getExternalFilesDir("secret") + "/" + "secret.txt";
        File file = new File(filePath);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("url:")) {
                    url = line.substring(4).trim();
                } else if (line.startsWith("secret:")) {
                    secret = line.substring(7).trim();
                }
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get secret content.", Toast.LENGTH_SHORT).show();
            finish();
        }
        long ts = System.currentTimeMillis();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update((ts + "" + secret).getBytes());
            String token = new BigInteger(1, md5.digest()).toString(16);
            fullUrl = url + "?ts=" + ts + "&token=" + token;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get MD5 instance.", Toast.LENGTH_SHORT).show();
            finish();
        }

        TextView userCountTextView = findViewById(R.id.user_count);
        TextView vipCountTextView = findViewById(R.id.vip_count);
        TextView lastVipTextView = findViewById(R.id.last_vip);

        Button refreshBtn = findViewById(R.id.refresh_vip);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OkHttpClient client = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build();
                        Request request = new Request.Builder().url(fullUrl).get().build();
                        Call call = client.newCall(request);
                        try {
                            Response response = call.execute();
                            ResponseBody body = response.body();
                            if (body != null) {
                                String string = body.string();
                                String applyTs = getKeyFromJson(string, "apply_ts");
                                String vipCount = "VIP用户数: " + getKeyFromJson(string, "vip_count");
                                String userCount = "用户总数: " + getKeyFromJson(string, "user_count");
                                String lastVip = "最近一次请求VIP用户: " + getKeyFromJson(string, "last_vip");

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        userCountTextView.setText(userCount);
                                        vipCountTextView.setText(vipCount);
                                        lastVipTextView.setText(lastVip);
                                    }
                                });
                            } else {
                                Toast.makeText(VIPActivity.this, "Response null point.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(VIPActivity.this, "Failed to request to " + url, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).start();
            }
        });
    }

    private String getKeyFromJson(String jsonString, String key) {
        Pattern pattern = Pattern.compile("\"?" + key + "\"?:.*?\"?(.*?)\"?[,}]");
        Matcher m = pattern.matcher(jsonString);
        if (m.find()) {
            String res = m.group(1);
            if (res == null) {
                return null;
            }
            if (res.trim().startsWith("\"")) {
                return res.trim().substring(1);
            }
            return res.trim();
        } else {
            return null;
        }
    }
}
