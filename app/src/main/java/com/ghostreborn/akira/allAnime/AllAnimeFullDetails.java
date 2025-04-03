package com.ghostreborn.akira.allAnime;

import android.text.Html;
import android.util.Log;

import com.ghostreborn.akira.model.AnimeDetails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

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
            Log.e("TAG", e.toString());
        }
        return "{}";
    }

    private String full(String id) {
        String variables = "\"showId\":\"" + id + "\"";
        String queryTypes = "$showId:String!";
        String query = "show(_id:$showId){name,englishName,description,thumbnail,banner,availableEpisodesDetail,relatedShows}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public AnimeDetails fullDetails(String id) {
        String rawJSON = full(id);

        if (rawJSON.equals("{}")){
            return null;
        }

        String anime = "";
        String description = "";
        String thumbnail = "";
        String banner = "";
        String prequel = "";
        String sequel = "";
        ArrayList<String> episodes = new ArrayList<>();

        try {
            JSONObject show = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("show");

            anime = show.getString("englishName");

            if(anime.equals("null")){
                anime = show.getString("name");
            }

            description = String.valueOf(Html.fromHtml(show.getString("description"), Html.FROM_HTML_MODE_LEGACY));
            thumbnail = show.getString("thumbnail");
            banner = show.getString("banner");

            JSONArray sub = show.getJSONObject("availableEpisodesDetail")
                    .getJSONArray("sub");
            for(int i=0; i<sub.length(); i++){
                episodes.add(sub.getString(i));
            }

            JSONArray relatedShows = show.getJSONArray("relatedShows");
            for (int i = 0; i < relatedShows.length(); i++) {
                JSONObject related = relatedShows.getJSONObject(i);
                String relation = related.optString("relation");
                if ("prequel".equals(relation)) {
                    prequel = related.optString("showId");
                }
                if ("sequel".equals(relation)) {
                    sequel = related.optString("showId");
                }
            }

            // thumbnail fix
            if (!thumbnail.contains("https")) {
                thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + thumbnail;
            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return new AnimeDetails(anime, description, banner, thumbnail, prequel, sequel,episodes);
    }

}
