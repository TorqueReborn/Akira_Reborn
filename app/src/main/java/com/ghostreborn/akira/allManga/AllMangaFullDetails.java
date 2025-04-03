package com.ghostreborn.akira.allManga;

import android.text.Html;
import android.util.Log;

import com.ghostreborn.akira.model.MangaDetails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllMangaFullDetails {

    private String connectAllManga(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            Log.e("TAG", e.toString());
        }
        return "{}";
    }

    public String full(String id) {
        String variables = "\"mangaId\":\"" + id + "\"";
        String queryTypes = "$mangaId:String!";
        String query = "manga(_id:$mangaId){name,englishName,description,thumbnail,banner,availableChaptersDetail}";
        return connectAllManga(variables, queryTypes, query);
    }

    public MangaDetails fullDetails(String id) {
        String rawJSON = full(id);

        if (rawJSON.equals("{}")) {
            return null;
        }

        String manga = "";
        String description = "";
        String thumbnail = "";
        String banner = "";
        ArrayList<String> chapters = new ArrayList<>();

        try {
            JSONObject manhwa = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("manga");

            manga = manhwa.getString("englishName");

            if (manga.equals("null")) {
                manga = manhwa.getString("name");
            }

            description = String.valueOf(Html.fromHtml(manhwa.getString("description"), Html.FROM_HTML_MODE_LEGACY));
            thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + manhwa.getString("thumbnail");

            banner = manhwa.getString("banner");

            JSONArray sub = manhwa.getJSONObject("availableChaptersDetail")
                    .getJSONArray("sub");
            for (int i = 0; i < sub.length(); i++) {
                chapters.add(sub.getString(i));
            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return new MangaDetails(manga, description, banner, thumbnail, chapters);
    }

}
