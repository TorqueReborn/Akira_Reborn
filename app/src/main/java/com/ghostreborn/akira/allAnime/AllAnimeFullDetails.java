package com.ghostreborn.akira.allAnime;

import android.util.Log;

import com.ghostreborn.akira.model.AnimeDetails;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeFullDetails {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String full(String id) {
        String variables = "\"showId\":\"" + id + "\"";
        String queryTypes = "$showId:String!";
        String query = "show(_id:$showId){thumbnail,banner}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public AnimeDetails fullDetails(String id) {
        String rawJSON = full(id);
        String thumbnail = "";
        String banner = "";
        try {
            JSONObject show = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("show");
            thumbnail = show.getString("thumbnail");
            banner = show.getString("banner");

            // thumbnail fix
            if (!thumbnail.contains("https")) {
                thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + thumbnail;
            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return new AnimeDetails(banner, thumbnail);
    }

}
