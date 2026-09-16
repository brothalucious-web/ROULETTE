# Roulette Wheel (Fabric mod for Minecraft 26.2)

Adds a **Roulette tab** to the top of your inventory. Put any item in the middle of the wheel,
pick Red, Black or Green, and press SPIN.

- Win on Red/Black (5 in 12 chance): keep your bet + prizes one tier above what you bet
- Win on Green (2 in 12 chance): keep your bet + prizes two tiers above
- Tiers: Uncommon > Rare > Epic > Legendary. One prize per 16 items bet (max 5)
- Lose: the bet is gone. Closing the wheel mid-spin settles the bet instantly.

## Building the jar

### Option A: no setup (GitHub builds it for you)
1. Create a new GitHub repository and upload everything in this folder.
2. Open the **Actions** tab; the "build" workflow runs automatically.
3. When it finishes, download the **Artifacts** zip. The mod is `roulette-1.0.0.jar`.

### Option B: build on your computer
1. Install **JDK 25** (for example from https://adoptium.net).
2. In this folder run `gradlew build` (Windows) or `./gradlew build` (Mac/Linux).
3. The mod is in `build/libs/roulette-1.0.0.jar` (ignore the `-sources` jar).

To test in a dev client instead: `gradlew runClient`.

## Installing
Put `roulette-1.0.0.jar` **and Fabric API** in `.minecraft/mods`, then launch Minecraft 26.2
with Fabric Loader 0.19.5 or newer. On a server, install it on both the server and every client.

## Customizing
- Prizes: `src/main/resources/data/roulette/loot_table/prize/tier1-4.json`
- Which bets count as valuable: `src/main/resources/data/roulette/tags/item/bet_tier1-4.json`
- Odds and spin speed: `src/main/java/com/roulette/RouletteRules.java` and `RouletteMenu.java`
