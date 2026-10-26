package com.diskree.achievetodo.ui.bukkit;

import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import com.diskree.achievetodo.ui.UiAbilityBrowserService;
import com.diskree.achievetodo.ui.UiIconKey;
import com.diskree.achievetodo.ui.UiRoute;
import com.diskree.achievetodo.ui.UiScreenViewModel;
import com.diskree.achievetodo.ui.UiSlotAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class BukkitUiSessionManager {

    private static final int INVENTORY_SIZE = 54;

    private final JavaPlugin plugin;
    private final BukkitProgressionRuntime progressionRuntime;
    private final UiAbilityBrowserService browserService;
    private final BukkitUiItemFactory itemFactory;
    private final BukkitUiTitleFactory titleFactory;
    private final BukkitUiPresentationPreferenceRepository presentationPreferences;
    private final Map<UUID, PlayerUiSession> sessionsByPlayer = new LinkedHashMap<>();
    private final Set<UUID> dirtySessions = new LinkedHashSet<>();
    private final Set<UUID> suppressedClosePlayers = new LinkedHashSet<>();
    private final AtomicLong nextSessionId = new AtomicLong(1L);

    BukkitUiSessionManager(
        JavaPlugin plugin,
        BukkitProgressionRuntime progressionRuntime,
        UiAbilityBrowserService browserService,
        BukkitUiItemFactory itemFactory
    ) {
        this.plugin = plugin;
        this.progressionRuntime = progressionRuntime;
        this.browserService = browserService;
        this.itemFactory = itemFactory;
        this.titleFactory = new BukkitUiTitleFactory();
        this.presentationPreferences = new BukkitUiPresentationPreferenceRepository(
            plugin.getDataFolder().toPath().resolve("ui").resolve("presentation.properties")
        );
    }

    boolean open(Player player) {
        if (!progressionRuntime.isReady()) {
            player.sendMessage(browserService.textService().resolve(player.getLocale(), "achievetodo.ui.open.not_ready"));
            return false;
        }
        PlayerUiSession session = new PlayerUiSession(
            player.getUniqueId(),
            nextSessionId.getAndIncrement(),
            UiRoute.initial(),
            browserService.textService().normalizeLocale(player.getLocale()),
            presentationPreferences.get(player.getUniqueId())
        );
        sessionsByPlayer.put(player.getUniqueId(), session);
        render(player, session, true);
        return true;
    }

    UiPresentationMode getPresentationMode(Player player) {
        PlayerUiSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session != null) {
            return session.presentationMode();
        }
        return presentationPreferences.get(player.getUniqueId());
    }

    void setPresentationMode(Player player, UiPresentationMode presentationMode) {
        presentationPreferences.set(player.getUniqueId(), presentationMode);
        PlayerUiSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.presentationMode(presentationMode);
        render(player, session, true);
    }

    void markDirty(UUID playerId) {
        if (sessionsByPlayer.containsKey(playerId)) {
            dirtySessions.add(playerId);
        }
    }

    void refreshDirtySessions() {
        if (dirtySessions.isEmpty()) {
            return;
        }
        Set<UUID> snapshot = new LinkedHashSet<>(dirtySessions);
        dirtySessions.clear();
        for (UUID playerId : snapshot) {
            Player player = Bukkit.getPlayer(playerId);
            PlayerUiSession session = sessionsByPlayer.get(playerId);
            if (player == null || session == null) {
                sessionsByPlayer.remove(playerId);
                continue;
            }
            render(player, session, false);
        }
    }

    void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        event.setCancelled(true);
        PlayerUiSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session == null || !matchesHolder(session, event.getView().getTopInventory())) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (!BukkitUiClickPolicy.isSemanticUiClick(
            event.getClickedInventory() == event.getView().getTopInventory(),
            event.getClick()
        )) {
            return;
        }
        UiScreenViewModel view = session.lastView();
        if (view == null) {
            return;
        }
        UiSlotAction action = view.actions().get(event.getRawSlot());
        if (action == null) {
            return;
        }
        if (action.kind() == com.diskree.achievetodo.ui.UiSlotActionKind.CLOSE) {
            player.closeInventory();
            return;
        }
        session.route(browserService.apply(session.route(), action, view.totalPages()));
        render(player, session, true);
    }

    void handleDrag(InventoryDragEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            event.setCancelled(true);
        }
    }

    void handleClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (suppressedClosePlayers.remove(player.getUniqueId())) {
            return;
        }
        sessionsByPlayer.remove(player.getUniqueId());
        dirtySessions.remove(player.getUniqueId());
    }

    void handleQuit(Player player) {
        sessionsByPlayer.remove(player.getUniqueId());
        dirtySessions.remove(player.getUniqueId());
    }

    void handleLocaleChange(Player player) {
        PlayerUiSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.locale(browserService.textService().normalizeLocale(player.getLocale()));
        render(player, session, true);
    }

    void closeAll() {
        for (UUID playerId : List.copyOf(sessionsByPlayer.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && BukkitUiInventories.isOwnedUi(player.getOpenInventory())) {
                player.closeInventory();
            }
        }
        sessionsByPlayer.clear();
        dirtySessions.clear();
    }

    private void render(Player player, PlayerUiSession session, boolean reopenInventory) {
        Optional<UiScreenViewModel> viewOptional = browserService.build(session.locale(), progressionRuntime.getProgressionState(player.getUniqueId()), session.route());
        if (viewOptional.isEmpty()) {
            player.sendMessage(browserService.textService().resolve(session.locale(), "achievetodo.ui.open.not_ready"));
            player.closeInventory();
            return;
        }
        UiScreenViewModel view = viewOptional.get();
        session.route(view.route());
        boolean createInventory = reopenInventory || session.inventory() == null || session.lastView() == null;
        if (!createInventory && view.equals(session.lastView())) {
            return;
        }
        if (createInventory) {
            Inventory inventory = plugin.getServer().createInventory(
                new BukkitUiInventoryHolder(player.getUniqueId(), session.sessionId()),
                INVENTORY_SIZE,
                titleFactory.createTitle(view.title(), session.presentationMode())
            );
            session.inventory(inventory);
        }
        fillInventory(session.inventory(), session.locale(), view, session.presentationMode());
        session.lastView(view);
        if (createInventory || player.getOpenInventory().getTopInventory() != session.inventory()) {
            if (player.getOpenInventory() != null && BukkitUiInventories.isOwnedUi(player.getOpenInventory())) {
                suppressedClosePlayers.add(player.getUniqueId());
            }
            player.openInventory(session.inventory());
        }
    }

    private void fillInventory(Inventory inventory, String locale, UiScreenViewModel view, UiPresentationMode presentationMode) {
        ItemStack filler = itemFactory.createFiller(presentationMode);
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        for (Map.Entry<Integer, com.diskree.achievetodo.ui.UiTabViewModel> entry : view.tabs().entrySet()) {
            inventory.setItem(entry.getKey(), itemFactory.createTabItem(entry.getValue(), presentationMode));
        }
        for (Map.Entry<Integer, com.diskree.achievetodo.ui.UiAbilityViewModel> entry : view.abilities().entrySet()) {
            inventory.setItem(entry.getKey(), itemFactory.createAbilityItem(entry.getValue(), presentationMode));
        }
        boolean previousActive = view.actions().containsKey(UiAbilityBrowserService.SLOT_PREVIOUS);
        inventory.setItem(UiAbilityBrowserService.SLOT_PREVIOUS, itemFactory.createNavigationItem(
            UiIconKey.navigation("previous"),
            browserService.textService().resolve(
                locale,
                previousActive ? "achievetodo.ui.navigation.previous" : "achievetodo.ui.navigation.previous.disabled"
            ),
            List.of(view.footerLine()),
            previousActive,
            presentationMode
        ));
        inventory.setItem(UiAbilityBrowserService.SLOT_CLOSE, itemFactory.createNavigationItem(
            UiIconKey.navigation("close"),
            browserService.textService().resolve(locale, "achievetodo.ui.navigation.close"),
            List.of(view.pageLine()),
            true,
            presentationMode
        ));
        boolean nextActive = view.actions().containsKey(UiAbilityBrowserService.SLOT_NEXT);
        inventory.setItem(UiAbilityBrowserService.SLOT_NEXT, itemFactory.createNavigationItem(
            UiIconKey.navigation("next"),
            browserService.textService().resolve(
                locale,
                nextActive ? "achievetodo.ui.navigation.next" : "achievetodo.ui.navigation.next.disabled"
            ),
            List.of(view.footerLine()),
            nextActive,
            presentationMode
        ));
        inventory.setItem(47, itemFactory.createInfoItem(view.pageLine(), List.of(view.footerLine()), presentationMode));
        inventory.setItem(51, itemFactory.createInfoItem(
            browserService.textService().resolve(locale, "achievetodo.ui.hover.title"),
            List.of(browserService.textService().resolve(locale, "achievetodo.ui.hover.description")),
            presentationMode
        ));
    }

    private boolean matchesHolder(PlayerUiSession session, Inventory inventory) {
        if (!(inventory.getHolder() instanceof BukkitUiInventoryHolder holder)) {
            return false;
        }
        return holder.ownerId().equals(session.playerId()) && holder.sessionId() == session.sessionId();
    }
}
