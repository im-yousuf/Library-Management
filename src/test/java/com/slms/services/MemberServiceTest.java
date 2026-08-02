package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.setTestMode(true);
        DatabaseManager.initializeDatabase();
        memberService = new MemberService();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        DatabaseManager.setTestMode(false);
    }

    @Test
    void testAddAndSearchMember() {
        Member m = new Member();
        m.setName("John Doe");
        m.setEmail("john@example.com");
        m.setPhone("1234567890");
        m.setAddress("123 Main St");
        
        boolean added = memberService.addMember(m);
        assertTrue(added);

        List<Member> members = memberService.searchMembers("John");
        assertEquals(1, members.size());
        assertEquals("John Doe", members.get(0).getName());
        assertEquals("ACTIVE", members.get(0).getStatus());
    }

    @Test
    void testDeleteMemberFailsWhenHasTransactions() throws Exception {
        Member m = new Member();
        m.setName("Jane Doe");
        memberService.addMember(m);
        
        Member savedMember = memberService.searchMembers("Jane").get(0);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO books (book_id, title) VALUES (1, 'Test');")) {
            ps.executeUpdate();
            try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO transactions (book_id, member_id, status) VALUES (1, ?, 'RETURNED')")) {
                ps2.setInt(1, savedMember.getMemberId());
                ps2.executeUpdate();
            }
        }

        boolean deleted = memberService.deleteMember(savedMember.getMemberId());
        assertFalse(deleted, "Member should not be deleted if they have transactions.");
    }
}
