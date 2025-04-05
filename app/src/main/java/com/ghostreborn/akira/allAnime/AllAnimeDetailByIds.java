package com.ghostreborn.akira.allAnime;

import android.util.Log;

import com.ghostreborn.akira.model.Anime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeDetailByIds {

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
        String query = "showsWithIds(ids:$ids){name, englishName,thumbnail,lastEpisodeInfo,rating,status}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public ArrayList<Anime> animeDetails(List<String> idList) {
        StringBuilder ids = new StringBuilder();
        for (String id : idList) {
            ids.append("\"").append(id).append("\",");
        }
        ArrayList<Anime> animes = new ArrayList<>();
        String rawJSON = details(ids.toString());
        String name;
        String thumbnail;
        String episodes;
        String rating;
        String status;
        try {
            JSONArray shows = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONArray("showsWithIds");

            for (int i = 0; i < shows.length(); i++) {
                JSONObject show = shows.getJSONObject(i);

                name = show.getString("englishName");

                if (name.equals("null")) {
                    name = show.getString("name");
                }

                thumbnail = show.getString("thumbnail");
                episodes = show.getJSONObject("lastEpisodeInfo")
                        .getJSONObject("sub")
                        .getString("episodeString");
                rating = show.getString("rating");
                status = show.getString("status");

                if (status.equals("Not Yet Released")) {
                    status = "Not Found";
                }

                // thumbnail fix
                if (!thumbnail.contains("https")) {
                    thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + thumbnail;
                }

                Anime anime = new Anime(idList.get(i));
                anime.setAnimeName(name);
                anime.setAnimeImage(thumbnail);
                anime.setAnimeEpisodes(episodes);
                anime.setAnimeRating(rating);
                anime.setAnimeStatus(status);

                animes.add(anime);

            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        return animes;
    }

}
