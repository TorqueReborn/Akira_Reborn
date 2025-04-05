package com.ghostreborn.akira.anilist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ghostreborn.akira.database.AniList;
import com.ghostreborn.akira.database.AniListDao;
import com.ghostreborn.akira.database.AniListDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AniListAuthorize {

    public void getToken(String code, Activity activity) {
        OkHttpClient client = new OkHttpClient();
        SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String json;
        try {
            json = new JSONObject()
                    .put("grant_type", "authorization_code")
                    .put("client_id", "25543")
                    .put("client_secret", "r4tJrIO1c4LwmGLch6F84nkfAFKk9lxvR8hezcaf")
                    .put("redirect_uri", "akira://ghostreborn.in")
                    .put("code", code)
                    .toString();
        } catch (JSONException e) {
            Log.e("TAG", "Error creating JSON request: ", e);
            return; // Important: Exit the method if JSON creation fails
        }

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("https://anilist.co/api/v2/oauth/token")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                String rawJSON = responseBody == null ? "{}" : responseBody.string();
                String token = new JSONObject(rawJSON)
                        .getString("access_token");
                preferences.edit()
                        .putString("ANILIST_TOKEN", token)
                        .apply();
                 getUserID(token, activity);
            }
        } catch (IOException | JSONException e) {
            Log.e("TAG", e.toString());
        }
    }

    private void getUserID(String token, Activity activity) {
        OkHttpClient client = new OkHttpClient();
        String query = "{\n" +
                "  Viewer {\n" +
                "    id\n" +
                "  }\n" +
                "}";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("query", query);
        } catch (Exception e) {
            Log.e("TAG", e.toString());
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String responseData = response.body().string();
                try {
                    String id = new JSONObject(responseData)
                            .getJSONObject("data")
                            .getJSONObject("Viewer")
                            .getString("id");
                    SharedPreferences preferences = activity.getSharedPreferences("AKIRA", Context.MODE_PRIVATE);
                    preferences.edit()
                            .putString("ANILIST_USER_ID", id)
                            .putBoolean("ANILIST_LOGGED_IN", true)
                            .apply();
                    ArrayList<AniList> aniLists = new AniListUserList().aniListEntry(id, token);

                    AniListDatabase db = AniListDatabase.getDatabase(activity);
                    AniListDao aniListDao = db.aniListDao();
                    aniListDao.insertAll(aniLists);

                } catch (Exception e) {
                    Log.e("TAG", e.toString());
                }
            }
        } catch (IOException e) {
            Log.e("TAG", e.toString());
        }
    }

}
