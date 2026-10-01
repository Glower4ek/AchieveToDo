package com.diskree.achievetodo.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class AdvancementLinkCommandTest {
    @Test void absentModProducesOneFallbackWithoutAttemptingInterop() {
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        var result = AdvancementLinkCommand.navigate("minecraft:story/mine_stone", navigation(false, false), failures::add);
        assertEquals(AdvancementLinkCommand.Result.ABSENT, result);
        assertEquals(java.util.List.of(result), failures);
    }
    @Test void installedUsableModNavigatesWithoutInstallFallback() {
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        assertEquals(AdvancementLinkCommand.Result.OPENED,
            AdvancementLinkCommand.navigate("blazeandcave:adventure/im_not_lost_anymore", navigation(true, true), failures::add));
        assertTrue(failures.isEmpty());
    }
    @Test void changedIntegrationIsUnavailableAndDoesNotClaimModIsAbsent() {
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        assertEquals(AdvancementLinkCommand.Result.UNAVAILABLE,
            AdvancementLinkCommand.navigate("minecraft:story/mine_stone", navigation(true, false), failures::add));
        assertEquals(java.util.List.of(AdvancementLinkCommand.Result.UNAVAILABLE), failures);
    }
    @Test void actualBrigadierTreePreservesVanillaBacapAndAtdIdentifiersAndCallsOnce() throws Exception {
        var dispatcher = new CommandDispatcher<FabricClientCommandSource>();
        var identifiers = new ArrayList<String>();
        AdvancementLinkCommand.register(dispatcher, (source, id) -> { identifiers.add(id); return 1; });
        for (String id : java.util.List.of("minecraft:story/mine_stone", "blazeandcave:adventure/im_not_lost_anymore",
            "achievetodo:abilities/open_inventory")) {
            assertEquals(1, dispatcher.execute("advancementssearch highlight " + id + " obtained_status", null));
            assertEquals(1, dispatcher.execute("advancementssearch highlight " + id, null));
        }
        assertEquals(6, identifiers.size());
        assertEquals("blazeandcave:adventure/im_not_lost_anymore", identifiers.get(2));
    }
    @Test void invalidIdentifierFailsOnceBeforeOptionalNavigation() {
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        assertEquals(AdvancementLinkCommand.Result.UNAVAILABLE,
            AdvancementLinkCommand.navigate("invalid ID", navigation(false, false), failures::add));
        assertEquals(1, failures.size());
    }
    @Test void absentScreenContractCannotLinkOptionalClasses() {
        assertFalse(OptionalAdvancementSearch.requestHighlight(new Object(), Identifier.parse("minecraft:story/mine_stone")));
        assertEquals("advancements_search", OptionalAdvancementSearch.MOD_ID);
    }
    @Test void installedScreenContractUsesNativeFlashingStateWithoutCopyingRendering() {
        var screen = new SearchScreenFixture();
        var id = Identifier.parse("blazeandcave:building/en_garde");
        assertTrue(OptionalAdvancementSearch.requestHighlight(screen, id));
        assertEquals(id, screen.flashingAdvancementId);
        assertEquals(0, screen.ticks);
        assertEquals(1, screen.stops);
    }
    @Test void changedFieldTypeAndOptionalLinkageFailureAreSafe() {
        assertFalse(OptionalAdvancementSearch.requestHighlight(new ChangedScreenFixture(), Identifier.parse("minecraft:story/root")));
        assertFalse(OptionalAdvancementSearch.requestHighlight(new BrokenScreenFixture(), Identifier.parse("minecraft:story/root")));
    }
    @Test void oneDispatchedClickProducesOneFallbackAndNoServerRetry() throws Exception {
        var dispatcher = new CommandDispatcher<FabricClientCommandSource>();
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        AdvancementLinkCommand.register(dispatcher, (source, id) -> {
            AdvancementLinkCommand.navigate(id, navigation(false, false), failures::add);
            return 0;
        });
        assertEquals(0, dispatcher.execute("advancementssearch highlight blazeandcave:building/en_garde obtained_status", null));
        assertEquals(java.util.List.of(AdvancementLinkCommand.Result.ABSENT), failures);
    }
    @Test void optionalImplementationIsNotOnTheTestClasspath() {
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("io.github.diskria.advancements_search.AdvancementsSearchMod"));
    }
    @Test void usableIntegrationReceivesExactIdentifierOnce() {
        var received = new ArrayList<Identifier>();
        var failures = new ArrayList<AdvancementLinkCommand.Result>();
        var id = "minecraft:adventure/kill_a_mob";
        assertEquals(AdvancementLinkCommand.Result.OPENED, AdvancementLinkCommand.navigate(id,
            new AdvancementLinkCommand.Navigation() {
                public boolean installed() { return true; }
                public boolean highlight(Identifier target) { received.add(target); return true; }
            }, failures::add));
        assertEquals(java.util.List.of(Identifier.parse(id)), received);
        assertTrue(failures.isEmpty());
    }
    public static class SearchScreenFixture {
        private Identifier flashingAdvancementId;
        int ticks = 12;
        int stops;
        public void advancements_search$stopFlashing() { flashingAdvancementId = null; ticks = 0; stops++; }
    }
    public static class ChangedScreenFixture {
        private String flashingAdvancementId;
        public void advancements_search$stopFlashing() {}
    }
    public static class BrokenScreenFixture {
        private Identifier flashingAdvancementId;
        public void advancements_search$stopFlashing() { throw new NoClassDefFoundError("optional API changed"); }
    }
    private AdvancementLinkCommand.Navigation navigation(boolean installed, boolean usable) {
        return new AdvancementLinkCommand.Navigation() {
            public boolean installed() { return installed; }
            public boolean highlight(Identifier id) {
                assertTrue(installed, "Absent branch must not invoke optional integration");
                return usable;
            }
        };
    }
}
