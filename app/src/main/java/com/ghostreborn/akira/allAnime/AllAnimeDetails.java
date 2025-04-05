package com.ghostreborn.akira.allAnime;

import android.util.Log;
import com.ghostreborn.akira.model.Anime;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeDetails {

    private String connectAllAnime(String variables, String queryTypes, String query) {
        String url = "https://api.allanime.day/api?variables={" + variables + "}&query=query(" + queryTypes + "){" + query + "}";
        Request request = new Request.Builder().url(url).header("Referer", "https://allmanga.to").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String details(String id){
        String variables = "\"showId\":\"" + id + "\"";
        String queryTypes = "$showId:String!";
        String query = "show(_id:$showId){name, englishName,thumbnail,lastEpisodeInfo,rating,status}";
        return connectAllAnime(variables, queryTypes, query);
    }

    public Anime animeDetails(String id){
        String rawJSON = details(id);
        String name = "";
        String thumbnail = "";
        String episodes = "";
        String rating = "";
        String status = "";
        try {
            JSONObject show = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("show");
            name = show.getString("englishName");

            if(name.equals("null")){
                name = show.getString("name");
            }

            thumbnail = show.getString("thumbnail");
            episodes = show.getJSONObject("lastEpisodeInfo")
                    .getJSONObject("sub")
                    .getString("episodeString");
            rating = show.getString("rating");
            status = show.getString("status");

            if (status.equals("Not Yet Released")){
                status = "Not Found";
            }

            // thumbnail fix
            if(!thumbnail.contains("https")){
                thumbnail = "https://wp.youtube-anime.com/aln.youtube-anime.com/" + thumbnail;
            }

        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        Anime anime = new Anime(id);
        anime.setAnimeName(name);
        anime.setAnimeImage(thumbnail);
        anime.setAnimeEpisodes(episodes);
        anime.setAnimeRating(rating);
        anime.setAnimeStatus(status);

        return anime;
    }

}
