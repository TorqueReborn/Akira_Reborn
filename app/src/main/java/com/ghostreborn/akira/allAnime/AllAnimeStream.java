package com.ghostreborn.akira.allAnime;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllAnimeStream {

    public static String decrypt(String decrypt) {
        StringBuilder decryptedString = new StringBuilder();
        for (int i = 0; i < decrypt.length(); i += 2)
            decryptedString.append((char) (Integer.parseInt(decrypt.substring(i, i + 2), 16) ^ 56));
        return decryptedString.toString();
    }

    private String connectAllAnime(String url) {
        Request request = new Request.Builder().url(url).header("Referer", "https://youtu-chan.com/").build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            Log.e("TAG", e.toString());
        }
        return "{}";
    }

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

    private String getEncryptedEpisodeUrls(String id, String episode) {
        String variables = "\"showId\":\"" + id + "\",\"episode\":\"" + episode + "\",\"translationType\":\"sub\"";
        String queryTypes = "$showId:String!,$episode:String!,$translationType:VaildTranslationTypeEnumType!";
        String query = "episode(showId:$showId,episodeString:$episode,translationType:$translationType){" +
                "sourceUrls" +
                "}";
        return connectAllAnime(variables, queryTypes, query);
    }

    private ArrayList<String> decryptedUrls(String rawJSON) {
        ArrayList<String> decrypted = new ArrayList<>();
        try {
            JSONArray sourceUrls = new JSONObject(rawJSON)
                    .getJSONObject("data")
                    .getJSONObject("episode")
                    .getJSONArray("sourceUrls");
            for (int i = 0; i < sourceUrls.length(); i++) {
                String sourceUrl = sourceUrls.getJSONObject(i).getString("sourceUrl");
                if (sourceUrl.contains("--")) {
                    sourceUrl = decrypt(sourceUrl.substring(2));
                    if (!sourceUrl.contains("fast4speed")) {
                        sourceUrl = "https://allanime.day" + sourceUrl.replace("clock", "clock.json");
                        decrypted.add(sourceUrl);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }
        return decrypted;
    }

    private ArrayList<String> serverUrls(String url) {
        String rawJSON = connectAllAnime(url);

        ArrayList<String> urls = new ArrayList<>();
        try {
            JSONArray links = new JSONObject(rawJSON)
                    .getJSONArray("links");
            for (int i = 0; i < links.length(); i++) {
                String link = links.getJSONObject(i).getString("link");
                urls.add(link);
            }
        } catch (JSONException e) {
            Log.e("TAG", e.toString());
        }

        return urls;

    }

    public ArrayList<String> serverUrls(String id, String episode) {
        String encryptedUrls = getEncryptedEpisodeUrls(id, episode);
        ArrayList<String> decryptedUrls = decryptedUrls(encryptedUrls);
        ArrayList<String> allUrls = new ArrayList<>();
        for(String decryptedUrl: decryptedUrls){
            allUrls.addAll(serverUrls(decryptedUrl));
        }
        return allUrls;
    }

}
