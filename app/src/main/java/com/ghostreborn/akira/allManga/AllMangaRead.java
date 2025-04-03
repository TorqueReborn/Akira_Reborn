package com.ghostreborn.akira.allManga;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllMangaRead {

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

    private String chapters(String id, String chapter) {
        String variables = "\"mangaId\":\"" + id + "\",\"chapterString\":\"" + chapter + "\",\"translationType\":\"sub\"";
        String queryTypes = "$mangaId:String!,$chapterString:String!,$translationType:VaildTranslationTypeMangaEnumType!";
        String query = "chapterPages(mangaId:$mangaId,chapterString:$chapterString,translationType:$translationType){" +
                "edges{pictureUrls}" +
                "}";
        return connectAllManga(variables, queryTypes, query);
    }

    public ArrayList<String> getChapters(String id, String chapter){
        String rawJSON = chapters(id, chapter);
        ArrayList<String> thumbnails = new ArrayList<>();
        try {
            JSONArray pictureUrls = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("chapterPages")
                    .getJSONArray("edges")
                    .getJSONObject(0)
                    .getJSONArray("pictureUrls");
            for(int i=0; i<pictureUrls.length(); i++){
                String thumbnail = "https://ytimgf.youtube-anime.com/" +
                        pictureUrls.getJSONObject(i)
                        .getString("url");
                thumbnails.add(thumbnail);
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        return thumbnails;
    }

}
