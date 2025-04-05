package com.ghostreborn.akira.anilist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AniListAuthorize {

    public static void getToken(String code, Activity activity) {
        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String json = "{}";
        try {
            json = new JSONObject()
                    .put("grant_type", "authorization_code")
                    .put("client_id", "25543")
                    .put("client_secret", "r4tJrIO1c4LwmGLch6F84nkfAFKk9lxvR8hezcaf")
                    .put("redirect_uri", "akira://ghostreborn.in")
                    .put("code", code)
                    .toString();
        } catch (JSONException e) {
            Log.e("TAG", "Error: ", e);
        }

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("https://anilist.co/api/v2/oauth/token")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String accessToken = new JSONObject(response.body().string())
                    .getString("access_token");
            SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
            preferences.edit()
                    .putBoolean("ANILIST_LOGGED_IN", true)
                    .putString("ANILIST_TOKEN", accessToken)
                    .apply();
        } catch (IOException | JSONException e) {
            Log.e("TAG", "Error: ", e);
        }
    }

}
