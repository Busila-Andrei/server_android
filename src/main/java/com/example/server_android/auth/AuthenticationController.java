package com.example.server_android.auth;

import com.example.server_android.*;
import com.example.server_android.exam.QuestionDTO;
import com.example.server_android.exam.QuestionMapper;
import com.example.server_android.exam.QuestionService;
import com.example.server_android.words.Word;
import com.example.server_android.words.WordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth/")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final WordService wordService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;

    @GetMapping("/check-server-connection")
    public ResponseEntity<ApiResponse> checkServerConnection() {
        logger.info("Checking server connection...");
        return ResponseEntity.ok(new ApiResponse(true, "Server is up and running!"));
    }

    @PostMapping("/check-enabled-account")
    public ResponseEntity<ApiResponse> checkIfUserIsEnabled(@RequestBody String emailJson) {
        logger.info("Checking if user is enabled for email: {}", emailJson);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(emailJson);
            String email = rootNode.get("email").asText();

            return ResponseEntity.ok(authenticationService.isUserEnabled(email));
        } catch (Exception e) {
            logger.error("Error checking if user is enabled: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, e.getMessage()));
        }

    }

    @PostMapping("/create-account")
    public ResponseEntity<ApiResponse> createAccount(@RequestBody @Validated RegisterRequest request) {
        logger.info("Creating account for email: {}", request.getEmail());
        try {
            ApiResponse apiResponse = authenticationService.register(request);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Account creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/confirm-account")
    public ResponseEntity<ApiResponse> confirmAccount(@RequestParam("token") String token) {
        logger.info("Confirming account with token: {}", token);
        try {
            ApiResponse apiResponse = authenticationService.confirmAccount(token);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Account confirmation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/resend-confirmation-email")
    public ResponseEntity<ApiResponse> resendConfirmationEmail(@RequestBody String emailJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(emailJson);
            String email = rootNode.get("email").asText();

            return ResponseEntity.ok(authenticationService.resendConfirmationEmail(email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Invalid request: " + e.getMessage()));
        }
    }

    @PostMapping("/login-account")
    public ResponseEntity<ApiResponse> authenticate(@RequestBody LoginRequest loginRequest) {
        logger.info("Authenticating user: {}", loginRequest.getEmail());
        try {
            ApiResponse apiResponse = authenticationService.login(loginRequest);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/verify-token")
    public ResponseEntity<ApiResponse> verifyToken(@RequestBody String tokenJson) {
        logger.info("Verifying token: {}", tokenJson);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(tokenJson);
            String token = rootNode.get("token").asText();
            ApiResponse apiResponse = authenticationService.verifyToken(token);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Token verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/logout-account")
    public ResponseEntity<ApiResponse> logout(@RequestBody String tokenJson) {
        logger.info("Logging out user with request: {}", tokenJson);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(tokenJson);
            String token = rootNode.get("token").asText();

            ApiResponse apiResponse = authenticationService.logout(token);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }
    @GetMapping("/words")
    public ResponseEntity<ApiResponse> getAllWords() {
        logger.info("Received request to fetch all words");
        try {
            List<Word> words = wordService.getAllWords();
            logger.info("Successfully fetched {} words", words.size());
            return ResponseEntity.ok(new ApiResponse(true, "Words fetched successfully", words));
        } catch (Exception e) {
            logger.error("Error fetching words: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "Error fetching words"));
        }
    }

    private final QuestionService questionService;

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAllQuestions().stream()
                .map(QuestionMapper::toQuestionDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse(true, "Questions fetched successfully", questions));
    }


    @GetMapping("/categories")
    public ResponseEntity<ApiResponse> getAllCategories() {
        logger.info("Fetching all categories");
        try {
            List<Category> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(new ApiResponse(true, "Categories fetched successfully", categories));
        } catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "Error fetching categories"));
        }
    }

    @GetMapping("/subcategory/{categoryId}")
    public ResponseEntity<ApiResponse> getSubcategoriesByCategoryId(@PathVariable Long categoryId) {
        logger.info("Fetching subcategories for category ID: {}", categoryId);
        try {
            List<SubcategoryDTO> subcategories = subcategoryService.getSubcategoriesByCategoryId(categoryId);
            return ResponseEntity.ok(new ApiResponse(true, "Subcategories fetched successfully", subcategories));
        } catch (Exception e) {
            logger.error("Error fetching subcategories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "Error fetching subcategories"));
        }
    }

}
