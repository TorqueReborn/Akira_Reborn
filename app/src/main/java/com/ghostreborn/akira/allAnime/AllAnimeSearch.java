package com.ghostreborn.akira.allAnime;

import android.util.Log;

import com.ghostreborn.akira.model.Anime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeSearch {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String query(String anime){
        String variables = "\"search\":{\"query\":\"" + anime + "\"}";
        String queryTypes = "$search:SearchInput!";
        String query = "shows(search:$search){edges{_id}}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public ArrayList<Anime> search(String anime){
        String rawJSON = query(anime);
        ArrayList<Anime> ids = new ArrayList<>();
        try {
            JSONArray edges = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("shows")
                    .getJSONArray("edges");
            for(int i=0; i<edges.length(); i++){
                String id = edges.getJSONObject(i).getString("_id");
                ids.add(new Anime(id));
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return ids;
    }

}
