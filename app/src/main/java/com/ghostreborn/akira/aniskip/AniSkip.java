package com.ghostreborn.akira.aniskip;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AniSkip {

    public String startAndEndSkip(String id, String episode) {
        String url = "https://api.aniskip.com/v2/skip-times/" + id + "/" + episode + "?types=ed&episodeLength=0";
        Request request = new Request.Builder().url(url).build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Long> startSkip(String id, String episode){
        String rawJSON = startAndEndSkip(id, episode);
        Map<String, Long> startSkip = new HashMap<>();
        try {
            JSONObject interval = new JSONObject(rawJSON)
                    .getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONObject("interval");
            long startTime = Long.parseLong(interval.getString("startTime").replace(".", ""));
            long endTime = Long.parseLong(interval.getString("endTime").replace(".", ""));
            startSkip.put("startTime", startTime);
            startSkip.put("endTime", endTime);
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        return startSkip;

    }

}
