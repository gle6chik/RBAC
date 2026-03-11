package core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {
    private CommandParser parser;
    private RBACSystem system;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();

        // Перехват консольного вывода
        System.setOut(new PrintStream(outContent));
    }

    @Test
    void testCommandRegistrationAndExecution() {
        final boolean[] called = {false};
        parser.registerCommand("test", "desc", (scanner, sys) -> called[0] = true);

        parser.parseAndExecute("TEST", new Scanner(""), system);
        assertTrue(called[0], "Command should be executed regardless of case");
    }

    @Test
    void testParsingLogicWithArguments() {
        final String[] capturedArg = {null};
        parser.registerCommand("cmd", "desc", (scanner, sys) -> capturedArg[0] = "executed");

        parser.parseAndExecute("  cmd  arg1 arg2  ", new Scanner(""), system);
        assertEquals("executed", capturedArg[0]);
    }

    @Test
    void testEmptyInputHandling() {
        assertDoesNotThrow(() -> parser.parseAndExecute(null, new Scanner(""), system));
        assertDoesNotThrow(() -> parser.parseAndExecute("", new Scanner(""), system));
        assertDoesNotThrow(() -> parser.parseAndExecute("   ", new Scanner(""), system));
        assertTrue(outContent.toString().isEmpty(), "Empty input should not trigger anything");
    }

    @Test
    void testUnknownCommandHandling() {
        parser.executeCommand("unknown", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("Unknown command: 'unknown'"));
        assertTrue(output.contains("Type 'help'"));
    }

    @Test
    void testCommandExceptionHandling() {
        parser.registerCommand("fail", "will throw exception", (scanner, sys) -> {
            throw new RuntimeException("Test Error");
        });

        assertDoesNotThrow(() -> parser.executeCommand("fail", new Scanner(""), system));
        assertTrue(outContent.toString().contains("Error executing command: Test Error"));
    }

    @Test
    void testPrintHelp() {
        parser.registerCommand("b_cmd", "desc B", (scanner, sys) -> {});
        parser.registerCommand("a_cmd", "desc A", (scanner, sys) -> {});

        parser.printHelp();
        String output = outContent.toString();

        assertTrue(output.contains("AVAILABLE COMMANDS"));
        assertTrue(output.contains("a_cmd"));
        assertTrue(output.contains("b_cmd"));

        int indexA = output.indexOf("a_cmd");
        int indexB = output.indexOf("b_cmd");
        assertTrue(indexA < indexB, "Commands should be sorted alphabetically");
    }
}