package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.config.ApiFallbackConfig;
import ch.uzh.ifi.hase.soprafs24.config.ApiKeyConfig;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Round;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats;
import ch.uzh.ifi.hase.soprafs24.repository.RoundRepository;
import ch.uzh.ifi.hase.soprafs24.repository.RoundStatsRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static ch.uzh.ifi.hase.soprafs24.config.ApiFallbackConfig.fallbackResponses;

// import org.locationtech.jts.geom.Geometry;
// import org.locationtech.jts.geom.Coordinate;
// import org.locationtech.jts.geom.GeometryFactory;
// import org.geotools.geojson.geom.GeometryJSON;
// import java.io.InputStreamReader;
// import java.io.Reader;

@Service
public class RoundService {
    @Autowired
    private RoundRepository roundRepository;
    @Autowired
    private RoundStatsRepository roundStatsRepository;
    @Autowired
    private GameService gameService;
    @Autowired
    private ApiKeyConfig apiKeyConfig;


    private static final double EARTH_RADIUS = 6371; // in kilometers

    private Set<Integer> usedFallbackIndices = new HashSet<>();

    public void createRound(UUID gameId) {
        // Fetch game
        Game game = gameService.getGame(gameId);

        // Set default values and save the Round instance
        Round newRound = new Round();
        newRound.setGameId(gameId);
        newRound.setCheckIn(0);

        // Iterate through gamePlayer set and create RoundStats instances
        for (GamePlayer gamePlayer : game.getPlayers()) {
            RoundStats roundStats = new RoundStats(game, gamePlayer, 0, 0, 0);
            roundStatsRepository.save(roundStats);
            newRound.addExistingRoundStats(roundStats);
        }

        // Save the Round instance again to persist the RoundStats instances
        roundRepository.save(newRound);
        roundRepository.flush();
    }

    public Round getRound(UUID gameId) {
        Optional<Round> roundOpt = roundRepository.findById(gameId);
        if (!roundOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found!");
        }
        return roundOpt.get();
    }

    @Transactional
    public void updatePlayerGuess(UUID gameId, Long userId, Integer pointsInc, double latitude, double longitude) {
        RoundStats roundStats = roundStatsRepository.findByGame_GameIdAndGamePlayer_PlayerId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in the game"));

        roundStats.updateRoundStats(pointsInc, latitude, longitude);
        roundStatsRepository.save(roundStats);
    }

    public String getRandomPicture(Round round, Game game) {
        try {
            if (ApiFallbackConfig.useFallback) { // Use the fallbacks without API access
                LocalTime endTime = calculateEndTime(game);
                return generateFallbackResponse(endTime,round).toString();
            }
            else { // Otherwise do the random picture Api Call
                JSONObject jsonResponse = fetchPictureFromApi();

                System.out.println(jsonResponse);
                // Check location data of api call
                JSONObject location = jsonResponse.optJSONObject("location");

                if (location == null ||
                        !location.has("position") ||
                        location.isNull("position") ||
                        !location.getJSONObject("position").has("latitude") ||
                        !location.getJSONObject("position").has("longitude") ||
                        location.getJSONObject("position").isNull("latitude") ||
                        location.getJSONObject("position").isNull("longitude") ||
                        location.getJSONObject("position").getDouble("latitude") == 0 ||
                        location.getJSONObject("position").getDouble("longitude") == 0 ||
                        (location.getJSONObject("position").getDouble("latitude") == 46.818188 &&
                                location.getJSONObject("position").getDouble("longitude") == 8.227512)) {
                    System.out.println("Image does not have valid location data. Retrying...");
                    return getRandomPicture(round, game);
                }

                double latitude = location.getJSONObject("position").getDouble("latitude");
                double longitude = location.getJSONObject("position").getDouble("longitude");

                // Check that the location is within Switzerland
                // if (!isPointWithinSwissBoundary(latitude, longitude)) {
                //     System.out.println("Location is not within the specified boundary. Retrying...");
                //     return getRandomPicture(round, game);
                // }

                // Set objects with data
                round.setLatitude(latitude);
                round.setLongitude(longitude);
                roundRepository.save(round);

                // Return trimmed object to the user
                LocalTime endTime = calculateEndTime(game);
                return generateResponse(jsonResponse, latitude, longitude, endTime).toString();
            }
        } catch (Exception e) {
            System.err.println("An error occurred. Returning fallback response.");
            System.err.println("Error details: " + e.getMessage());
            LocalTime endTime = calculateEndTime(game);
            return generateFallbackResponse(endTime,round).toString();
        }
    }

    // private boolean isPointWithinSwissBoundary(double latitude, double longitude) throws IOException {
    //     try {
    //         System.out.println("Checking if location is within Switzerland...");
    //         // Load the GeoJSON file from the repository
    //         Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("src/main/resources/swissboundary.geojson"));
    //         System.out.println("----- reader -----" + reader);
    //         GeometryJSON gjson = new GeometryJSON();
    //         Geometry boundary = gjson.read(reader);

    //         System.out.println("----- Swiss boundary: -----" + reader);

    //         // Create a point
    //         org.locationtech.jts.geom.Point point = new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
    //         System.out.println("----- point created: -----" + point);

    //         // Check if the point is within the boundary
    //         return boundary.contains(point);

    //     } catch (Exception e) {
    //         System.err.println("An error occurred. Returning True...");
    //         System.err.println("Error details: " + e + "\nError message: " + e.getMessage());
    //         return true;
    //     }
    // }

    public JSONObject fetchPictureFromApi() throws URISyntaxException, IOException, InterruptedException {
        String apiUrl = "https://api.unsplash.com/photos/random";
        String query = "Switzerland+landscape+cityscape";
        String clientId = apiKeyConfig.getCurrentApiKey(); // Replace with your Unsplash access key
        String urlString = apiUrl + "?query=" + query;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                .header("Authorization", "Client-ID " + clientId)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    public LocalTime calculateEndTime(Game game) {
        int guessTime = game.getGuessTime() + 1;

        // Get the current time in Zurich
        ZoneId zurichZone = ZoneId.of("Europe/Zurich");
        ZonedDateTime zurichTime = ZonedDateTime.now(zurichZone);
        LocalTime generationTime = zurichTime.toLocalTime();

        return generationTime.plusSeconds(guessTime);
    }

    public JSONObject generateFallbackResponse(LocalTime endTime, Round round) {
        Random rand = new Random();

        // If all fallbacks have been used, reset
        if (usedFallbackIndices.size() == fallbackResponses.size()) {
            usedFallbackIndices.clear();
        }

        // Build a list of available indices
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < fallbackResponses.size(); i++) {
            if (!usedFallbackIndices.contains(i)) {
                availableIndices.add(i);
            }
        }

        // Randomly select from available ones
        int randomIndex = availableIndices.get(rand.nextInt(availableIndices.size()));
        usedFallbackIndices.add(randomIndex);

        JSONObject selectedFallback = fallbackResponses.get(randomIndex);

        selectedFallback.put("end_time", endTime.toString());

        // Set latitude and longitude on the round object
        double latitude = selectedFallback.getDouble("latitude");
        double longitude = selectedFallback.getDouble("longitude");

        round.setLatitude(latitude);
        round.setLongitude(longitude);
        roundRepository.save(round);

        return selectedFallback;
    }

    public JSONObject generateResponse(JSONObject jsonResponse, double latitude, double longitude, LocalTime endTime) {
        JSONObject trimmedResponse = new JSONObject();
        trimmedResponse.put("regular_url", jsonResponse.getJSONObject("urls").getString("regular"));
        trimmedResponse.put("latitude", latitude);
        trimmedResponse.put("longitude", longitude);
        trimmedResponse.put("user_name", jsonResponse.getJSONObject("user").getString("name"));
        trimmedResponse.put("user_username", jsonResponse.getJSONObject("user").getString("username"));
        trimmedResponse.put("end_time", endTime.toString());
        return trimmedResponse;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;

        return distance;
    }

}
