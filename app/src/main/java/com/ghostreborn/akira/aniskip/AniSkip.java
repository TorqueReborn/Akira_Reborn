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

    private Map<String, Long> startSkip(String id, String episode) {
        String url = "https://api.aniskip.com/v2/skip-times/" + id + "/" + episode + "?types=op&episodeLength=0";
        Request request = new Request.Builder().url(url).build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? parseSkip(response.body().string(), "OP_") : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Long> endSkip(String id, String episode) {
        String url = "https://api.aniskip.com/v2/skip-times/" + id + "/" + episode + "?types=ed&episodeLength=0";
        Request request = new Request.Builder().url(url).build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? parseSkip(response.body().string(), "ED_") : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Long> parseSkip(String rawJSON, String name){
        Map<String, Long> startSkip = new HashMap<>();
        try {
            JSONObject interval = new JSONObject(rawJSON)
                    .getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONObject("interval");
            long startTime = Long.parseLong(interval.getString("startTime").replace(".", ""));
            long endTime = Long.parseLong(interval.getString("endTime").replace(".", ""));
            startSkip.put(name + "startTime", startTime);
            startSkip.put(name + "endTime", endTime);
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return startSkip;
    }

    public Map<String, Long> startEndSkip(String id, String episode){
        Map<String, Long> startEndSkips = new HashMap<>();
        startEndSkips.putAll(startSkip(id, episode));
        startEndSkips.putAll(endSkip(id, episode));
        return startEndSkips;
    }

}
