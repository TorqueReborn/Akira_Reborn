package com.ghostreborn.akira.allAnime;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeQueryPopular {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String query() {
        String variables = "\"type\":\"anime\", \"size\":20, \"dateRange\":1";
        String queryTypes = "$type:VaildPopularTypeEnumType!, $size:Int!, $dateRange:Int";
        String query = "queryPopular(type:$type, size:$size, dateRange:$dateRange){recommendations{anyCard{_id}}}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public List<String> queryPopular() {
        String rawJSON = query();
        List<String> ids = new ArrayList<>();
        try {
            JSONArray recommendations = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("queryPopular")
                    .getJSONArray("recommendations");
            for (int i = 0; i < recommendations.length(); i++) {
                String id = recommendations.getJSONObject(i).getJSONObject("anyCard").getString("_id");
                ids.add(id);
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return ids;
    }

}
