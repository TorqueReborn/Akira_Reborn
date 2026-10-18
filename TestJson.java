import org.json.JSONObject;

public class TestJson {
    public static void main(String[] args) {
        JSONObject obj = new JSONObject("{\"englishName\": null, \"name\": \"Suite Precure\", \"title\": null}");
        System.out.println("optString englishName: [" + obj.optString("englishName") + "]");
        System.out.println("optString null fallback: [" + obj.optString("englishName", null) + "]");
        System.out.println("isNull: " + obj.isNull("englishName"));
    }
}
