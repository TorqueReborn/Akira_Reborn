package com.ghostreborn.akira.allAnime;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeSearchMal {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String searchMal(String anime) {
        String variables = "\"search\":{\"query\":\"" + anime + "\"}";
        String queryTypes = "$search:SearchInput!";
        String query = "shows(search:$search){edges{_id,malId}}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public String getAllAnimeId(String anime, String malId) {
        String rawJSON = searchMal(anime);
        try {
            JSONArray edges = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("shows")
                    .getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                JSONObject animeObject = edges.getJSONObject(i);
                if (animeObject.getString("malId").equals(malId)) {
                    return animeObject.getString("_id");
                }
            }
        } catch (JSONException e) {
            Log.e("TAG", "Error parsing JSON: ", e);
        }
        return "";
    }

}
