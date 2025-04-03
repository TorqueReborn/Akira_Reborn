package com.ghostreborn.akira.allManga;

import android.util.Log;

import com.ghostreborn.akira.model.Manga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllMangaSearch {

    private String connectAllManga(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String query(String manga){
        String variables = "\"search\":{\"query\":\"" + manga + "\"}";
        String queryTypes = "$search:SearchInput!";
        String query = "mangas(search:$search){edges{_id}}";
        return connectAllManga(variables, queryTypes, query);
    }

    public ArrayList<Manga> search(String manga){
        String rawJSON = query(manga);
        ArrayList<Manga> ids = new ArrayList<>();
        try {
            JSONArray edges = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("mangas")
                    .getJSONArray("edges");
            for(int i=0; i<edges.length(); i++){
                String id = edges.getJSONObject(i).getString("_id");
                ids.add(new Manga(id));
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return ids;
    }

}
