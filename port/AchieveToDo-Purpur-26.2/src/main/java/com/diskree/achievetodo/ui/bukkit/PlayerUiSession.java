package com.diskree.achievetodo.ui.bukkit;

import com.diskree.achievetodo.ui.UiRoute;
import com.diskree.achievetodo.ui.UiScreenViewModel;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.UUID;

final class PlayerUiSession {

    private final UUID playerId;
    private final long sessionId;
    private UiRoute route;
    private String locale;
    private UiPresentationMode presentationMode;
    private Inventory inventory;
    private UiScreenViewModel lastView;

    PlayerUiSession(UUID playerId, long sessionId, UiRoute route, String locale, UiPresentationMode presentationMode) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.sessionId = sessionId;
        this.route = Objects.requireNonNull(route, "route");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode");
    }

    UUID playerId() {
        return playerId;
    }

    long sessionId() {
        return sessionId;
    }

    UiRoute route() {
        return route;
    }

    void route(UiRoute route) {
        this.route = Objects.requireNonNull(route, "route");
    }

    String locale() {
        return locale;
    }

    void locale(String locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    UiPresentationMode presentationMode() {
        return presentationMode;
    }

    void presentationMode(UiPresentationMode presentationMode) {
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode");
    }

    Inventory inventory() {
        return inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    UiScreenViewModel lastView() {
        return lastView;
    }

    void lastView(UiScreenViewModel lastView) {
        this.lastView = lastView;
    }
}
