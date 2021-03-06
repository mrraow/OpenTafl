package com.manywords.softworks.tafl;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.manywords.softworks.tafl.command.player.external.engine.ExternalEngineClient;
import com.manywords.softworks.tafl.engine.ai.evaluators.PieceSquareTable;
import com.manywords.softworks.tafl.rules.Variants;
import com.manywords.softworks.tafl.rules.Rules;
import com.manywords.softworks.tafl.ui.AdvancedTerminal;
import com.manywords.softworks.tafl.ui.lanterna.TerminalUtils;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class Debug {
    public static void run(Map<String, String> args) {
        Rules r = Variants.rulesForNameAndDimension("Fetlar", 11);
        PieceSquareTable table = new PieceSquareTable(r, null);

        table.logTable(0);
        Log.print(Log.Level.VERBOSE, "\n");
        table.logTable(1);
        Log.print(Log.Level.VERBOSE, "\n");
        table.logTable(2);

        if(args.containsKey("--engine")) {
            ExternalEngineClient.run();
        }
        else {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            Terminal t = null;

            if(args.containsKey("--fallback")) {
                factory.setForceTextTerminal(true);
                try {
                    t = factory.createTerminal();
                    System.setOut(TerminalUtils.newDummyPrintStream());
                    System.setErr(TerminalUtils.newDummyPrintStream());
                } catch (IOException e) {
                    System.out.println("Unable to start.");
                }
            }
            /*
            else if(args.containsKey("--server-mode")) {
                // Blocks here
                NetworkServer ns = new NetworkServer(3);
            }*/
            else if(args.containsKey("--dummy-client")) {
                ConsoleReader reader = null;
                Socket server = null;
                PrintWriter writer = null;
                try {
                    server = new Socket("localhost", 11541);
                    reader = new ConsoleReader();
                    writer = new PrintWriter(new OutputStreamWriter(server.getOutputStream()), true);
                } catch (IOException e) {
                    System.out.println("Error starting console reader! " + e);
                    System.exit(-1);
                }

                while(true) {
                    try {
                        String toSend = reader.readLine("Send to server] ");
                        if(toSend.equals("exit")) break;

                        writer.println(toSend);

                        server.getOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    writer.close();
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (t != null) {
                AdvancedTerminal<? extends Terminal> th = new AdvancedTerminal<>(t);
            }
        }
    }
}
