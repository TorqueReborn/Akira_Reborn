package com.ghostreborn.akira.anilist;

import android.util.Log;

import com.ghostreborn.akira.database.AniList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AniListUserList {

    private static String connectAniList(String graph, String token) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("query", graph);
        } catch (Exception e) {
            Log.e("TAG", "Error putting json: ", e);
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if(response.body() != null){
                return response.body().string();
            }
        } catch (IOException ex) {
            Log.e("TAG", "Error: ", ex);
        }
        return "{}";
    }

    private String animeList(String userID, String token) {
        String graph = "query{\n" +
                "  MediaListCollection(userId:" + userID + ",type:ANIME,status:CURRENT){\n" +
                "    lists{\n" +
                "      entries {\n" +
                "        media{ \n" +
                "          idMal\n" +
                "          title {\n" +
                "            userPreferred\n" +
                "          }\n" +
                "        }\n" +
                "        progress\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        return connectAniList(graph, token);
    }

    public ArrayList<AniList> aniListEntry(String userID, String token){
        String rawJSON = animeList(userID, token);
        ArrayList<AniList> aniListEntries = new ArrayList<>();
        try{
            JSONArray entries = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("MediaListCollection")
                    .getJSONArray("lists")
                    .getJSONObject(0)
                    .getJSONArray("entries");
            for(int i=0; i< entries.length(); i++){
                JSONObject entry = entries.getJSONObject(i);
                String progress = entry.getString("progress");
                JSONObject media = entry.getJSONObject("media");
                String idMAL = media.getString("idMal");
                String title = media.getJSONObject("title")
                        .getString("userPreferred");
                aniListEntries.add(new AniList(idMAL,title,progress));
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return aniListEntries;
    }

}
