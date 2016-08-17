package com.manywords.softworks.tafl.test;

import com.manywords.softworks.tafl.OpenTafl;
import com.manywords.softworks.tafl.test.ai.*;
import com.manywords.softworks.tafl.test.consistency.*;
import com.manywords.softworks.tafl.test.mechanics.*;
import com.manywords.softworks.tafl.test.network.HeadlessAITest;
import com.manywords.softworks.tafl.test.network.LoadServerGameTest;
import com.manywords.softworks.tafl.test.network.ServerTest;
import com.manywords.softworks.tafl.test.rules.*;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void run() {
        List<TaflTest> tests = new ArrayList<TaflTest>();

        // Initial tests (debug only)

        // Consistency tests
        tests.add(new GameSerializerConsistencyTest());
        tests.add(new MoveSerializerConsistencyTest());
        tests.add(new PositionSerializerConsistencyTest());
        tests.add(new RulesSerializerConsistencyTest());
        tests.add(new TranspositionTableConsistencyTest());
        tests.add(new NetworkPacketConsistencyTests());
        tests.add(new BerserkHistoryDuplicationTest());

        // Rules tests
        tests.add(new KingHammerAnvilTest());
        tests.add(new KingUnsafeAgainstBoardEdgeTest());
        tests.add(new SpeedLimitTest());
        tests.add(new RestrictedFortReentryTest());
        tests.add(new ThreefoldDrawTest());
        tests.add(new ThreefoldVictoryTest());
        tests.add(new TablutKingCaptureTest());
        tests.add(new StrongKingCaptureTest());
        tests.add(new DoubleCaptureTest());
        tests.add(new CaptureTest());
        tests.add(new EdgeVictoryTest());
        tests.add(new CornerVictoryTest());
        tests.add(new RestrictedSpaceTest());
        tests.add(new EncirclementTest());
        tests.add(new StrictShieldwallTest());
        tests.add(new ShieldwallTest());
        tests.add(new EdgeFortEscapeTest());
        tests.add(new EdgeFortEscapeFailedTest());
        tests.add(new CommanderCaptureVictoryTest());
        tests.add(new CommanderCornerCaptureVictoryTest());
        tests.add(new JumpCaptureBerserkerTest());
        tests.add(new BerserkMoveDuplicationTest());
        tests.add(new BadFetlarCaptureTest());
        tests.add(new BadCopenhagenCaptureTest());

        // Mechanics tests
        tests.add(new MoveAddressTest());
        tests.add(new ReplayGameTest());
        tests.add(new KingMissingPositionRecordTest());

        // Long-running mechanics tests
        tests.add(new ExternalEngineHostTest());
        tests.add(new GameClockTest());

        // Network tests
        tests.add(new ServerTickThreadTest());
        tests.add(new ServerTest());
        tests.add(new LoadServerGameTest());
        tests.add(new HeadlessAITest()); // also tests client connection somewhat

        // AI tests
        tests.add(new AIMatchingZobristTest());
        tests.add(new AICertainKingEscapeTest());
        tests.add(new AICertainKingCaptureTest());
        tests.add(new AITwoCornerEscapeAndRulesLoadingTest());
        tests.add(new AITwoEdgeEscapeAndRulesLoadingTest());
        tests.add(new AIMoveRepetitionTest());

        for (TaflTest test : tests) {
            try {
                OpenTafl.logPrint(OpenTafl.LogLevel.SILENT, test.getClass().getSimpleName() + ": ");
                test.run();
                OpenTafl.logPrintln(OpenTafl.LogLevel.SILENT, "passed");
            } catch (AssertionError e) {
                OpenTafl.logPrintln(OpenTafl.LogLevel.SILENT, "FAILED");
                OpenTafl.logPrintln(OpenTafl.LogLevel.SILENT, "Assertion: " + e);
                OpenTafl.logStackTrace(OpenTafl.LogLevel.SILENT, e);
                System.exit(1);
            } catch (Exception e) {
                OpenTafl.logPrintln(OpenTafl.LogLevel.SILENT, "CRASHED");
                OpenTafl.logPrintln(OpenTafl.LogLevel.SILENT, "Exception: " + e);
                OpenTafl.logStackTrace(OpenTafl.LogLevel.SILENT, e);
                System.exit(1);
            }
        }

        System.exit(0);
    }
}