package com.coolweather.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;
import com.google.gson.GsonBuilder;

import org.litepal.util.LogUtil;

import java.io.IOException;
import java.io.Serializable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author xavier
 */
public class WelcomeActivity extends Activity {


    public static final int succeed = 1;
    public static final int fail = 2;
    public static final int nonet = 3;
    public String normalDistrict;
    public String locationCity = "北京";
    public LocationClient mLocationClient = null;
    public BDLocationListener mListener;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        initData();

    }

    private void initData() {
        showProgressDialog("自动定位中...");
        initBaiduMapLocation();


    }

    private void initBaiduMapLocation() {
        mLocationClient = new LocationClient(this.getApplicationContext());
        mListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mListener);// 娉ㄥ唽鐩戝惉鍑芥暟
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
        LogUtil.d("initBaiduMapLocation","initBaiduMapLocation");
    }

    private void showProgressDialog(String title) {

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.setMessage(title + "...");
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.show();
    }

    public void requestWeather() {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + locationCity + "&key=bc0418b57b2d4918819d3974ac1285d9";
        LogUtil.d("weatherUrl", weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();

                            Intent intent = new Intent(WelcomeActivity.this,
                                    WeatherActivity.class);

                            intent.putExtra("weather_data", (Serializable) weather);
                            intent.putExtra("normal_city", normalDistrict);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(WelcomeActivity.this, "获取天气信息失败!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WelcomeActivity.this, "获取天气信息失败!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 拦截返回键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            Toast.makeText(WelcomeActivity.this,"location.getCity()",Toast.LENGTH_SHORT).show();
            if (location != null) {
                normalDistrict = location.getDistrict();
                locationCity = location.getCity();
                LogUtil.d("Location", locationCity);
                if (locationCity == null) {
                    Toast.makeText(WelcomeActivity.this, "定位失败，请检查网络", Toast.LENGTH_SHORT).show();
                } else {
                    pDialog.dismiss();
                    String[] str = locationCity.split("市");
                    locationCity = str[0];
                    if ("".equals(locationCity)) {
                        Toast.makeText(WelcomeActivity.this, "定位失败，默认为北京", Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(WelcomeActivity.this, WeatherActivity.class);
                    intent.putExtra("weather_id", locationCity);
                    startActivity(intent);
                    WelcomeActivity.this.finish();
                }
            }
        }
    }
}
