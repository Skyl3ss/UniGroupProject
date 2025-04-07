package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.config.ApiFallbackConfig;
import ch.uzh.ifi.hase.soprafs24.config.ApiKeyConfig;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.RoundRepository;
import ch.uzh.ifi.hase.soprafs24.repository.RoundStatsRepository;

import static ch.uzh.ifi.hase.soprafs24.config.ApiFallbackConfig.fallbackResponses;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@SpringBootTest
public class RoundServiceTest {

    @Mock
    private RoundRepository roundRepository;
    @Mock
    private RoundStatsRepository roundStatsRepository;
    @Mock
    private GameService gameService;
    @InjectMocks
    private RoundService roundService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createRound_success() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        User mockuser = new User();
        game.setGameId(gameId);
        game.addNewPlayer(mockuser);
        when(gameService.getGame(gameId)).thenReturn(game);

        // Act
        roundService.createRound(gameId);

        // Assert
        verify(roundRepository, times(1)).save(any(Round.class));
    }

    @Test
    public void createRound_failure() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        game.setGameId(gameId);
        when(gameService.getGame(gameId)).thenReturn(game);

        // Act
        try {
            roundService.createRound(gameId);
        } catch (ResponseStatusException e) {
            // Assert
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
    }

    @Test
    public void getRound_success() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        Round round = new Round();
        when(roundRepository.findById(gameId)).thenReturn(Optional.of(round));

        // Act
        Round result = roundService.getRound(gameId);

        // Assert
        assertEquals(round, result);
    }

    @Test
    public void getRound_failure() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        when(roundRepository.findById(gameId)).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(ResponseStatusException.class, () -> roundService.getRound(gameId));
    }

    @Test
    public void updatePlayerGuess_success() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        Long userId = 1L;
        Integer pointsInc = 1;
        double latitude = 47.399591;
        double longitude = 8.514325;
        RoundStats roundStats = new RoundStats();
        when(roundStatsRepository.findByGame_GameIdAndGamePlayer_PlayerId(gameId, userId)).thenReturn(Optional.of(roundStats));

        // Act
        roundService.updatePlayerGuess(gameId, userId, pointsInc, latitude, longitude);

        // Assert
        verify(roundStatsRepository, times(1)).save(any(RoundStats.class));
    }

    @Test
    public void generateResponse_success() {
        // Arrange
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("urls", new JSONObject().put("regular", "https://example.com"));
        jsonResponse.put("user", new JSONObject().put("name", "John Doe").put("username", "johndoe"));
        double latitude = 47.399591;
        double longitude = 8.514325;
        LocalTime endTime = LocalTime.now();
    
        // Act
        JSONObject result = roundService.generateResponse(jsonResponse, latitude, longitude, endTime);
    
        // Assert
        assertEquals("https://example.com", result.get("regular_url"));
        assertEquals(47.399591, result.get("latitude"));
        assertEquals(8.514325, result.get("longitude"));
        assertEquals("John Doe", result.get("user_name"));
        assertEquals("johndoe", result.get("user_username"));
        assertEquals(endTime.toString(), result.get("end_time"));
    }

    @Test
    public void updatePlayerGuess_failure() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        Long userId = 1L;
        when(roundStatsRepository.findByGame_GameIdAndGamePlayer_PlayerId(gameId, userId)).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> roundService.updatePlayerGuess(gameId, userId, 1, 47.399591, 8.514325));
    }

    @Test
    public void calculateEndTime_success() {
        // Arrange
        Game game = new Game();
        game.setGuessTime(30);

        // Act
        LocalTime result = roundService.calculateEndTime(game);

        // Assert
        LocalTime expectedEndTime = ZonedDateTime.now(ZoneId.of("Europe/Zurich")).toLocalTime().plusSeconds(31);
        assertEquals(expectedEndTime.getHour(), result.getHour());
        assertEquals(expectedEndTime.getMinute(), result.getMinute());
        assertEquals(expectedEndTime.getSecond(), result.getSecond(), 1); // allow some tolerance for test execution delay
    }
  
  
    @Test
    public void calculateDistance1_success() {
        // Arrange
        double lat1 = 47.399591;
        double lon1 = 8.514325;
        double lat2 = 47.399591;
        double lon2 = 8.514325;
    
        // Act
        double result = roundService.calculateDistance(lat1, lon1, lat2, lon2);
    
        // Assert
        assertEquals(0, result);
    }

    @Test
    public void calculateDistance2_success() {
        // Arrange
        double lat1 = 47.399591;
        double lon1 = 8.514325;
        double lat2 = 46.94809;
        double lon2 = 7.44744;

        // Act
        double result = roundService.calculateDistance(lat1, lon1, lat2, lon2);

        // Assert
        double expectedDistance = 94.48; // approximate value in kilometers
        assertEquals(expectedDistance, result, 1.0); // allow 1 km tolerance
    }

  
    @Test
    public void generateFallbackResponse_success() {
        // Arrange
        LocalTime endTime = LocalTime.now();
        Round round = new Round();

        // Act
        JSONObject result = roundService.generateFallbackResponse(endTime, round);

        // Assert
        boolean matchFound = false;

        for (JSONObject fallback : ApiFallbackConfig.fallbackResponses) {
            boolean matches =
                    fallback.get("regular_url").equals(result.get("regular_url")) &&
                            fallback.get("latitude").equals(result.get("latitude")) &&
                            fallback.get("longitude").equals(result.get("longitude"));

            if (matches) {
                matchFound = true;
                break;
            }
        }

        assertTrue(matchFound, "Result should match one of the known fallback responses.");
        assertEquals(endTime.toString(), result.get("end_time"));

        // Make sure round was updated to match one of the fallback responses
        double resultLat = (double) result.get("latitude");
        double resultLng = (double) result.get("longitude");

        assertEquals(resultLat, round.getLatitude(), 0.0001);
        assertEquals(resultLng, round.getLongitude(), 0.0001);
    }

    @Test
    public void generateFallbackResponse_shouldReturnAllUniqueFallbacksBeforeRepeating() {
        int totalFallbacks = ApiFallbackConfig.fallbackResponses.size();

        Set<String> usedUrls = new HashSet<>();

        for (int i = 0; i < totalFallbacks; i++) {
            Round round = new Round();
            LocalTime time = LocalTime.now();

            JSONObject result = roundService.generateFallbackResponse(time, round);

            String url = result.getString("regular_url");
            assertFalse(usedUrls.contains(url), "Fallback was repeated before all were used.");
            usedUrls.add(url);

            // Optional: validate round got updated with correct coordinates
            assertEquals(result.getDouble("latitude"), round.getLatitude(), 0.0001);
            assertEquals(result.getDouble("longitude"), round.getLongitude(), 0.0001);
        }

        // After all are used, next one could repeat (reset), so we allow it
        Round extraRound = new Round();
        JSONObject repeated = roundService.generateFallbackResponse(LocalTime.now(), extraRound);
        assertNotNull(repeated);
    }

}
