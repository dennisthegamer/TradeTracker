# Changelog

All notable changes to TradeTracker will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
