# DATAPACK CONTENT MAP

Date verified: August 16, 2026

## Embedded datapack in this port

- Source: `original/src/main/java/com/diskree/achievetodo/ability/generation/AbilityAdvancementsGenerator.java`
- Source: `original/src/main/java/com/diskree/achievetodo/ability/generation/AbilityUnlockMessagesGenerator.java`
- Source: `original/src/main/java/com/diskree/achievetodo/ability/generation/AbilitiesGenerator.java`
- Purpose: synthetic `achievetodo:abilities/*` advancements and unlock-message reward functions.
- Required / optional: required.
- License concern: first-party AchieveToDo port content; generated from original project logic.
- Future stage: runtime grant/revoke logic on the Purpur server side.

## `bacap_override`

- Source: `original/src/main/resources/resourcepacks/bacap_override`
- Purpose: override / patch pack for BlazeandCave advancement reward flow and messaging; contains large third-party datapack-style content under `blazeandcave` and `bacap_rewards`.
- Required / optional: optional.
- License concern: high. Contains non-trivial external pack content that should not be re-distributed blindly.
- Future stage: BACAP integration audit if server-side compatibility becomes required.

## `bacap_hardcore_override`

- Source: `original/src/main/resources/resourcepacks/bacap_hardcore_override`
- Purpose: hardcore-specific BACAP reward overrides.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: BACAP compatibility stage only.

## `bacap_terralith_override`

- Source: `original/src/main/resources/resourcepacks/bacap_terralith_override`
- Purpose: Terralith-specific BACAP reward overrides.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: optional worldgen compatibility stage.

## `bacap_amplified_nether_override`

- Source: `original/src/main/resources/resourcepacks/bacap_amplified_nether_override`
- Purpose: Amplified Nether-specific BACAP reward overrides.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: optional worldgen compatibility stage.

## `bacap_nullscape_override`

- Source: `original/src/main/resources/resourcepacks/bacap_nullscape_override`
- Purpose: Nullscape-specific BACAP reward overrides.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: optional worldgen compatibility stage.

## `bacap_rewards_item`

- Source: `original/src/main/resources/resourcepacks/bacap_rewards_item`
- Purpose: item-reward mode for BlazeandCave.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: BACAP reward-mode compatibility only.

## `bacap_rewards_experience`

- Source: `original/src/main/resources/resourcepacks/bacap_rewards_experience`
- Purpose: experience-reward mode for BlazeandCave.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: BACAP reward-mode compatibility only.

## `bacap_rewards_trophy`

- Source: `original/src/main/resources/resourcepacks/bacap_rewards_trophy`
- Purpose: trophy-reward mode for BlazeandCave.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: BACAP reward-mode compatibility only.

## `bacap_cooperative_mode`

- Source: `original/src/main/resources/resourcepacks/bacap_cooperative_mode`
- Purpose: cooperative-mode functions for BlazeandCave progression / rewards.
- Required / optional: optional.
- License concern: high. External datapack-style content.
- Future stage: cooperative BACAP integration only.

## Conclusion

- Stage 3 required only the synthetic `achievetodo:abilities/*` datapack content.
- No BACAP override / reward / cooperative pack content was copied into `port`.
- `/advancementssearch` is referenced by the original unlock-message generator but was not found in AchieveToDo source itself, so it remains an external / optional integration dependency.
