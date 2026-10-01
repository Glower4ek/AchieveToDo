# 0.1.5.2 — targeted user smoke

User acceptance is pending. Use Minecraft 26.2, Java 25, Loader 0.19.5 and Fabric API 0.161.0+26.2.

| Scope | Manual check | Expected result |
|---|---|---|
| BUG-01 flower | Legitimately receive Цветик-семицветик; inspect inventory tooltip in Russian and default language | Translated name, original color/bold/italic and four lore lines; no serialized JSON |
| BUG-01 siblings | Inspect another trophy, such as Your Cat's Prize or Bouquet, and the quest's Tenth Parchment | Both custom_name/single-quoted lore and namespaced item_name/double-quoted lore render normally |
| BUG-01 persistence | Save, reload and rejoin with one custom reward | Name, style and separate lore lines survive |
| BUG-02 earned chat | Earn a vanilla advancement and BACAP task, goal or challenge through gameplay; hover the earned names | Exactly one earned message; localized title and description tooltip, original frame/color and click behavior |
| BUG-02 existing hover | Hover a previously working command/warning advancement reference | Existing tooltip still works; secret/hidden behavior remains unchanged |
| BUG-03 locks | Trigger crouch and at least two other locks, e.g. diamond tools, shield or cauldron | Two centered lines: denied action above remaining count; translated wording and count remain correct |
| BUG-04 fresh world | Create a fresh world and inspect latest.log | No early false missing bac_advancements ERROR; objective binds and ability progression becomes ready |
| BUG-04 existing world | Reload/rejoin an initialized world | Existing objective binds normally; no error spam |
| Quick regression | Axe and cauldron locked/unlocked controls; shield/deflect_arrow; save/reload | Existing accepted behavior remains intact |

Prior 0.1.5 manual results remain recorded as USER_SMOKE_GREEN for Axe, Cauldron, Shield and save/reload/rejoin. They do not count as acceptance of 0.1.5.2. Raider manual smoke remains optional/deferred and has not been reported GREEN.

Automated coverage: all 215 frozen function-created custom items, full source Component equality and NBT component-codec round trips, Russian/default rendering, 1202 active shipped tellraw references, all AbilityType values at counts 1/2/10 and permanent locks, transient/existing/persistently missing scoreboard states, and targeted native earned messages plus real flower/parchment ItemStack persistence. Pixel-level client confirmation remains this checklist's purpose.

Future changed candidates must increment the final canonical modVersion component: 0.1.5.3, 0.1.5.4, etc. The Jar task refuses to overwrite an existing four-component candidate. No cleanup or Phase B is part of this pass.
