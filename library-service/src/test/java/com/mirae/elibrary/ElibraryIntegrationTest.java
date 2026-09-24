package com.mirae.elibrary;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.repository.BookRepository;
import com.mirae.elibrary.repository.LoanRepository;
import com.mirae.elibrary.repository.UserRepository;
import com.mirae.elibrary.web.dto.AuthResponse;
import com.mirae.elibrary.web.dto.LoginRequest;
import com.mirae.elibrary.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests exercising the real HTTP layer, security filter chain, JWT
 * auth, services and JPA persistence (H2). Covers the full borrow/return
 * lifecycle plus the main error paths and their consistent error body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ElibraryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoanRepository loanRepository;

    private UUID bookId;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        Book book = bookRepository.save(new Book("Clean Code", "Robert C. Martin",
                "9780132350884", "A handbook.", "Software", 2008, 2));
        bookId = book.getId();
    }

    @Test
    void browse_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void register_thenFullBorrowReturnFlow() throws Exception {
        String token = registerAndGetToken("member@demo.io", "password123", "Member");

        // Browse
        mockMvc.perform(get("/api/books").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Clean Code")))
                .andExpect(jsonPath("$.content[0].availableCopies", is(2)));

        // Borrow
        mockMvc.perform(post("/api/loans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.book.title", is("Clean Code")));

        // Available copies dropped
        mockMvc.perform(get("/api/books/" + bookId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.availableCopies", is(1)));

        // Currently borrowed
        MvcResult loansResult = mockMvc.perform(get("/api/loans/me?status=ACTIVE")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andReturn();
        String loanId = objectMapper.readTree(loansResult.getResponse().getContentAsString())
                .get(0).get("id").asString();

        // Return
        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RETURNED")));

        // Available copies restored
        mockMvc.perform(get("/api/books/" + bookId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.availableCopies", is(2)));
    }

    @Test
    void borrow_sameBookTwice_returns409() throws Exception {
        String token = registerAndGetToken("member@demo.io", "password123", "Member");
        mockMvc.perform(post("/api/loans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(borrowBody(bookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/loans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(borrowBody(bookId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void borrow_unknownBook_returns404() throws Exception {
        String token = registerAndGetToken("member@demo.io", "password123", "Member");
        mockMvc.perform(post("/api/loans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void getBook_unknownId_returns404() throws Exception {
        String token = registerAndGetToken("member@demo.io", "password123", "Member");
        mockMvc.perform(get("/api/books/" + UUID.randomUUID()).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_invalidPayload_returns400WithFieldErrors() throws Exception {
        RegisterRequest bad = new RegisterRequest("not-an-email", "short", "");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        registerAndGetToken("member@demo.io", "password123", "Member");
        LoginRequest login = new LoginRequest("member@demo.io", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private String registerAndGetToken(String email, String password, String name) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, name);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.token();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String borrowBody(UUID id) {
        return "{\"bookId\":\"" + id + "\"}";
    }
}
