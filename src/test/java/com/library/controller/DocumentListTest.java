package com.library.controller;

import com.library.config.CustomUserDetails;
import com.library.model.User;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DocumentListTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin != null) {
            CustomUserDetails cud = new CustomUserDetails(admin);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    cud, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    @Test
    public void testGetDocumentsList() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetQrRequestPage() throws Exception {
        User reader = userRepository.findByUsername("reader1").orElse(null);
        if (reader != null) {
            CustomUserDetails cud = new CustomUserDetails(reader);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    cud, null, List.of(new SimpleGrantedAuthority("ROLE_READER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        mockMvc.perform(get("/borrow/qr-request").param("bookId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetBorrowHistoryPage() throws Exception {
        User reader = userRepository.findByUsername("reader1").orElse(null);
        if (reader != null) {
            CustomUserDetails cud = new CustomUserDetails(reader);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    cud, null, List.of(new SimpleGrantedAuthority("ROLE_READER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        mockMvc.perform(get("/borrow/history"))
                .andExpect(status().isOk());
    }
}
