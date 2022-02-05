package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

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
import okhttp3.MediaType;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class VIPActivity extends AppCompatActivity {

    private static String url;
    private static String secret;

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

        TextView userCountTextView = findViewById(R.id.user_count);
        TextView vipCountTextView = findViewById(R.id.vip_count);
        TextView lastVipTextView = findViewById(R.id.last_vip);
        TextView lastVipTSTextView = findViewById(R.id.last_vip_ts);

        Button refreshBtn = findViewById(R.id.refresh_vip);
        refreshBtn.setOnClickListener(v -> new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url(getFullUrl()).get().build();
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                customToast("Response " + response.code());
                ResponseBody body = response.body();
                if (body != null) {
                    String string = body.string();
                    String applyTs = "最近一次请求VIP时间: " + getKeyFromJson(string, "apply_ts");
                    String vipCount = "VIP用户数: " + getKeyFromJson(string, "vip_count");
                    String userCount = "用户总数: " + getKeyFromJson(string, "user_count");
                    String lastVip = "最近一次请求VIP用户: " + getKeyFromJson(string, "last_vip");

                    boolean lastIsVip = "true".equals(getKeyFromJson(string, "last_is_vip"));

                    runOnUiThread(() -> {
                        lastVipTSTextView.setText(applyTs);
                        userCountTextView.setText(userCount);
                        vipCountTextView.setText(vipCount);
                        lastVipTextView.setText(lastVip);

                        if (lastIsVip) {
                            lastVipTextView.setTextColor(Color.GREEN);
                        } else {
                            lastVipTextView.setTextColor(Color.RED);
                        }

                    });
                } else {
                    customToast("Response null point.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                customToast("Failed to request to " + url);
            }
        }).start());

        EditText editText = findViewById(R.id.input_eml);

        Button copyBtn = findViewById(R.id.copy_eml);
        copyBtn.setOnClickListener(v -> {
            String text = lastVipTextView.getText().toString();
            String lastVipEmail = text.substring("最近一次请求VIP用户: ".length()).trim();
            if ("null".equalsIgnoreCase(lastVipEmail)) {
                customToast("Refresh before copy!");
            } else {
                editText.setText(lastVipEmail);
            }
        });

        Button setBtn = findViewById(R.id.set_vip);
        setBtn.setOnClickListener(v -> new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build();
            MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("eml", editText.getText());
            } catch (JSONException e) {
                e.printStackTrace();
                customToast("Unknown Error");
                finish();
            }
            RequestBody requestBody = RequestBody.create(String.valueOf(jsonObject), mediaType);
            Request request = new Request.Builder().url(getFullUrl()).post(requestBody).build();
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                if (response.code() == 404) {
                    customToast("Invalid User!");
                    return;
                }

                ResponseBody responseBody = response.body();

                if (responseBody != null) {
                    String string = responseBody.string();
                    String code = getKeyFromJson(string, "code");
                    if ("1".equals(code)) {
                        customToast("User is already VIP");
                    } else if ("2".equals(code)) {
                        customToast("Failed to set VIP");
                    } else if ("0".equals(code)) {
                        customToast("Done!");
                    } else {
                        customToast("Unknown Response: " + string);
                    }
                } else {
                    customToast("Response null point.");
                }

            } catch (IOException e) {
                e.printStackTrace();
                customToast("Failed to request to " + url);
            }
        }).start());
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

    private void customToast(String content) {
        // TODO: 默认参数Toast.LENGTH_SHORT
        runOnUiThread(() -> Toast.makeText(VIPActivity.this, content, Toast.LENGTH_SHORT).show());
    }

    private String getFullUrl() {
        String fullUrl = null;
        long ts = System.currentTimeMillis();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update((ts + "" + secret).getBytes());
            String token = new BigInteger(1, md5.digest()).toString(16);
            fullUrl = url + "?ts=" + ts + "&token=" + token;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            customToast("Failed to get MD5 instance.");
            this.finish();
        }
        System.out.println("Full url: " + fullUrl);
        return fullUrl;
    }
}
