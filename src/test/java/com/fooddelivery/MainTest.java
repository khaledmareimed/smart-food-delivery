package com.fooddelivery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void mainWiresTheApplicationAndRunsTheConsole() {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("alice\nalice123\n0\n".getBytes()));
            System.setOut(new PrintStream(bytes));
            Main.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        String out = bytes.toString();
        assertTrue(out.contains("Welcome, alice"));
        assertTrue(out.contains("Goodbye!"));
    }
}
