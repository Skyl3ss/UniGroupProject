package ch.uzh.ifi.hase.soprafs24.config;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


public class ApiFallbackConfig {
    //Will the Fallback be used
    public static final boolean useFallback = true;
    // Define a class or method to store your fallback responses
    public static List<JSONObject> fallbackResponses = new ArrayList<>();

    static {
        // Populate the list with 5 different fallback responses
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1594754654150-2ae221b25fe8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3MTYyMDcwMzl8&ixlib=rb-4.0.3&q=80&w=1080", "Raphi See", "raphisee", 47.399591, 8.514325));
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1730743760937-9f15fb879c47?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3NDQwNDc5MTJ8&ixlib=rb-4.0.3&q=80&w=1080", "Ziang Guo", "eliaskuo", 46.020713, 7.749117));
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1725345651079-eb86af6eb2b0?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3NDQwNDgwMDl8&ixlib=rb-4.0.3&q=80&w=1080", "Ben Grayland", "bengrayland", 47.01364, 8.43716));
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1720417940938-2202c7d65616?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3NDQwNDgwNjd8&ixlib=rb-4.0.3&q=80&w=1080", "Ilia Afsharpoor", "iamiliaafsharpoor", 47.67809, 8.615449));
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1725392878707-bf70f0ab54b6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3NDQwNDgxMTV8&ixlib=rb-4.0.3&q=80&w=1080", "snap shoot", "s_napshoot", 47.050168, 8.3093));
        fallbackResponses.add(createFallbackResponse("https://images.unsplash.com/photo-1672804596121-d29b89bfeee2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w1NzE3ODd8MHwxfHJhbmRvbXx8fHx8fHx8fDE3NDQwNDgxNjR8&ixlib=rb-4.0.3&q=80&w=1080", "Anthony Gomez", "anthonygomez", 47.69589, 8.638049));
    }

    // Helper method to create a fallback response
    private static JSONObject createFallbackResponse(String imageUrl, String userName, String userUsername, double latitude, double longitude) {
        JSONObject fallbackResponse = new JSONObject();
        fallbackResponse.put("regular_url", imageUrl);
        fallbackResponse.put("user_name", userName);
        fallbackResponse.put("user_username", userUsername);
        fallbackResponse.put("latitude", latitude);
        fallbackResponse.put("longitude", longitude);
        return fallbackResponse;
    }

}
