# Changelog

All notable changes to TradeTracker will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.1] - 2026-07-21

### Changed
- Internal: the mixin configuration now declares `JAVA_25`, matching the class
  files the build actually produces (it still claimed `JAVA_21`). No functional
  change.

## [1.4.0] - 2026-07-18

### Added
- **Freely positionable main HUD**: the config screen has a new "Edit HUD Position..." button
  that opens an editor - drag the HUD anywhere on screen and confirm
- **Position presets**: save, apply, rename and delete HUD positions
- HudLib is bundled inside the jar (jar-in-jar); there is nothing extra to install

### Changed
- The main HUD is no longer limited to the four screen corners. An existing `hudPosition`
  setting is migrated automatically to the same spot
- HUD drawing and both trade flashes (gold on profit, red on loss) now come from the shared
  HudLib; the visible behaviour is unchanged
- The tracking arrow is deliberately untouched and keeps its own separate position setting

## [1.3.0] - 2026-07-14

### Added
- **NeoForge support**: the repository now builds two jars from one codebase —
  `tradetracker-fabric-1.3.0+mc26.1-26.1.2.jar` and `tradetracker-neoforge-1.3.0+mc26.1-26.1.2.jar`
  - Shared code lives in `common/`; thin loader entrypoints in `fabric/` and `neoforge/`
  - Loader calls (config directory) go through a `Platform` interface resolved via `ServiceLoader`
  - On NeoForge the config screen is reachable from the mod list (`IConfigScreenFactory`);
    ModMenu integration remains Fabric-only

### Changed
- Unified the mod version across all loaders and Minecraft versions
- Standardized jar naming to `tradetracker-<loader>-<version>+mc<range>` (e.g.
  `tradetracker-fabric-1.3.0+mc26.1-26.1.2.jar`, `tradetracker-neoforge-1.3.0+mc26.1-26.1.2.jar`)
- Corrected author and contact metadata (Modrinth + GitHub links)
- **The NeoForge jar now requires Minecraft 26.1.2** — MC 26.1/26.1.1 are no longer accepted on NeoForge; on Fabric they remain supported
- YACL and ModMenu are now resolved from the Modrinth Maven (the YACL maven no
  longer hosts the 26.1-line builds); versions unchanged (YACL 3.9.2+26.1, ModMenu 18.0.0)

## [1.2.0] - 2026-07-02

### Added
- **TradeMemory**: persistent villager tracking and price comparison system
  - Every villager is registered automatically when its trade screen is opened
    (profession, position, dimension, first/last seen) and updated while nearby —
    stored permanently in `config/tradetracker_memory.json`
  - Offer prices are captured on GUI open (even without trading); completed trades
    are recorded per villager with item, emerald price and buy/sell direction
  - **Villager Memory screen** (keybind `V`): list of all known villagers with
    profession, custom tag, position, trade count and last-seen time; sortable by
    last seen or profession (`Tab`); typing searches offers by item name, sorted
    cheapest first
  - **Villager detail screen**: full info header, known offers with cross-villager
    price comparison ("Better price: N◆ · who · distance" badge), per-villager trade
    history, and actions: Mark for Tracking, Set Custom Tag, Remove
  - Enchanted books are compared per enchantment (e.g. "Enchanted Book (Mending)")
  - Dynamic villager prices (demand, Hero of the Village, gossip discounts/markups) are
    tracked: stored prices reflect the actual current cost, shown as "▼6◆ (7◆)" with the
    base price in parentheses — green for discounts, red for raised prices; the session
    profit calculation also uses the actually paid price now
  - **Glow effect**: villagers marked for tracking get a vanilla glow outline in a
    configurable color (client-side)
  - **Direction arrows**: HUD indicators pointing towards marked villagers with
    name/tag and distance, shown at a fixed configurable screen anchor (default
    top center, 9 positions) — only the arrow glyph follows the view direction
  - New config category **Trade Memory** (glow on/off, glow color, direction arrows
    on/off), localized in English and German

### Changed
- **Unified TradeTracker window**: Villager Memory and Trade History now share one
  window with a navigation sidebar on the left — switch between the tabs without
  separate keybinds. `V` opens the window on the last used tab.

### Fixed
- Trade counter nameplate no longer rendered twice when the villager is riding
  another entity (e.g. a boat) — the label was also submitted for the vehicle's
  renderer
- Buy offers (villager pays emeralds) now track price changes: demand and gossip
  adjustments apply to the demanded item count, not the emerald payout — the offer
  list shows "×6 → 1◆" and e.g. "▲×10 (×6) → 1◆" in red when the villager currently
  demands more items (previously buy offers always showed a static price and never
  any markup/discount)
- Known offers show how often each offer was traded with this villager
  ("· traded 26×"), making per-offer tracking visible

## [1.1.1] - 2026-06-21
- **Maven group changed** from `com.example.tradetracker` to `de.dennisthegamer.tradetracker`
  (the Minecraft 26.2 dependency updates from this release live on the `mc26.2` branch)

## [1.1.0] - 2026-05-06

### Added
- Per-villager trade counter: a nameplate-style label ("Trades: N") appears above a
  villager's head when the player's crosshair targets them, showing how many trades
  the player has completed with that specific villager
- Config option **Show Trade Count Label** (`showVillagerNameplate`, default: enabled)
  to toggle the per-villager nameplate on or off via the config screen
- `VillagerTradeStore`: persistent UUID-based trade count storage, saved permanently
  to `config/tradetracker_villagers.json` — survives world restarts
- Wandering Trader trades are counted when the "Track Wandering Trader" config option
  is enabled (consistent with existing session tracking behavior)


## [1.0.0] - 2026-04-14

### Added
- Initial release
- Automatic trade detection for Villagers and Wandering Traders
- Real-time emerald profit/loss calculation per trade
- Emerald value table with 100+ item mappings (materials, crops, food, enchanted books, armor, tools, rare items)
- Session management with start/pause/resume via keybind (`J`)
- Automatic session pause when opening pause menu or leaving world
- Optional session persistence across world closes (save/load from JSON)
- Session statistics: total trades, profit, loss, net balance
- HUD overlay with two display modes:
  1. **Full mode**: Detailed statistics (trades, balance, best/worst trades, daily count)
  2. **Compact mode**: One-line summary (balance + trade count)
- Customizable HUD position (top-left, top-right, bottom-left, bottom-right)
- Adjustable HUD opacity (0-100%) and scale (50-150%)
- HUD visibility toggle: always visible or only when merchant screen is open
- Gold flash effect for profitable trades (+10 emeralds)
- Red flash effect for losing trades (-5 emeralds)
- Trade history screen accessible via keybind (`G`)
- Two-panel history layout: profession filter (left) + trade list (right)
- Color-coded trade entries: green (profit), red (loss), gray (neutral)
- History filter modes: All, Profit only, Loss only, Last 10 trades
- Scrollable trade history interface
- Session reset function (`R` in history screen)
- Full YACL config screen via ModMenu
- Persistent config saved to `config/tradetracker.json`
- Mixin injection into `MerchantResultSlot` for trade capture
- Localization support (English & German)
- Client-side only - no server installation required
- Compatibility with Minecraft 26.1+
- Fabric Loader 0.18.4+ support
- Fabric API integration
