package com.ghostreborn.akira.allManga;

import android.util.Log;

import com.ghostreborn.akira.model.Manga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllMangaDetailByIds {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String details(String ids) {
        String variables = "\"ids\":" + "[" + ids.substring(0, ids.length() - 1) + "]";
        String queryTypes = "$ids:[String!]!";
        String query = "mangasWithIds(ids:$ids){_id,name,englishName,thumbnail,lastChapterInfo,rating,status}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public ArrayList<Manga> mangaDetails(List<String> idList) {
        StringBuilder ids = new StringBuilder();
        for (String id : idList) {
            ids.append("\"").append(id).append("\",");
        }
        ArrayList<Manga> mangas = new ArrayList<>();
        String rawJSON = details(ids.toString());
        String id;
        String name;
        String thumbnail;
        String chapters;
        String rating;
        String status;
        try {
            JSONArray shows = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONArray("mangasWithIds");

            for (int i = 0; i < shows.length(); i++) {
                JSONObject show = shows.getJSONObject(i);

                id = show.getString("_id");

                name = show.getString("englishName");

                if (name.equals("null")) {
                    name = show.getString("name");
                }

                thumbnail = show.getString("thumbnail");
                chapters = show.getJSONObject("lastChapterInfo")
                        .getJSONObject("sub")
                        .getString("chapterString");
                rating = show.getString("rating");
                status = show.getString("status");

                if (status.equals("Not Yet Released")) {
                    status = "Not Found";
                }

                // thumbnail fix
                if (!thumbnail.contains("https")) {
                    thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + thumbnail;
                }

                Manga manga = new Manga(id);
                manga.setMangaName(name);
                manga.setMangaImage(thumbnail);
                manga.setMangaChapters(chapters);
                manga.setMangaRating(rating);
                manga.setMangaStatus(status);

                mangas.add(manga);

            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        return mangas;
    }

}
