package com.ghostreborn.akira.allManga;

import android.util.Log;

import com.ghostreborn.akira.model.Manga;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllMangaDetails {

    private String connectAllManga(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String details(String id) {
        String variables = "\"mangaId\":\"" + id + "\"";
        String queryTypes = "$mangaId:String!";
        String query = "manga(_id:$mangaId){name, englishName,thumbnail,lastChapterInfo,rating,status}";
        return connectAllManga(variables, queryTypes, query);
    }

    public Manga mangaDetails(Manga manga) {
        String rawJSON = details(manga.getId());
        String name = "";
        String thumbnail = "";
        String chapters = "";
        String rating = "";
        String status = "";
        try {
            JSONObject manhwa = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("manga");
            name = manhwa.getString("englishName");

            if (name.equals("null")) {
                name = manhwa.getString("name");
            }

            thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + manhwa.getString("thumbnail");
            chapters = manhwa.getJSONObject("lastChapterInfo")
                    .getJSONObject("sub")
                    .getString("chapterString");
            rating = manhwa.getString("rating");
            status = manhwa.getString("status");

            if (status.equals("Not Yet Released")) {
                status = "Not Found";
            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        manga.setMangaName(name);
        manga.setMangaImage(thumbnail);
        manga.setMangaChapters(chapters);
        manga.setMangaRating(rating);
        manga.setMangaStatus(status);

        return manga;
    }

}
