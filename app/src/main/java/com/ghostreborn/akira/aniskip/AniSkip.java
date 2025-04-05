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

    private Map<String, Long> parseSkip(String rawJSON, String name) {
        Map<String, Long> startSkip = new HashMap<>();
        try {
            JSONObject interval = new JSONObject(rawJSON)
                    .getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONObject("interval");
            long startTime;
            String startTimeStr = interval.getString("startTime");
            if (startTimeStr.contains(".")) {
                startTime = Long.parseLong(startTimeStr.replace(".", ""));
            } else {
                startTime = Long.parseLong(startTimeStr + "000");
            }

            long endTime;
            String endTimeStr = interval.getString("endTime");
            if (endTimeStr.contains(".")) {
                endTime = Long.parseLong(endTimeStr.replace(".", ""));
            } else {
                endTime = Long.parseLong(endTimeStr + "000");
            }
            startSkip.put(name + "startTime", startTime);
            startSkip.put(name + "endTime", endTime);
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return startSkip;
    }

    public Map<String, Long> startEndSkip(String id, String episode) {
        Map<String, Long> startEndSkips = new HashMap<>();
        startEndSkips.putAll(startSkip(id, episode));
        startEndSkips.putAll(endSkip(id, episode));
        return startEndSkips;
    }

}
