# TradeTracker

A Minecraft mod for Fabric and NeoForge that automatically tracks and analyzes all Villager and Wandering Trader trades, calculating emerald profit/loss in real-time with a detailed trade history and HUD overlay.

## Features

- Automatic trade detection and recording for Villagers and Wandering Traders
- Real-time emerald profit/loss calculation with 100+ item value mappings
- HUD overlay with full and compact display modes
- Dedicated trade history screen with filtering and sorting
- Session management with start/pause/resume controls
- Optional session persistence across world closes
- Flash effects for profitable and losing trades
- Full in-game config screen (YACL + ModMenu)
- Client-side only - no server required

## Compatibility

- **Minecraft**: 26.2
- **Loaders**: Fabric & NeoForge
- **Fabric Loader**: 0.19.2+ (Fabric)
- **Fabric API**: Required (Fabric)
- **Java**: 25+
- **ModMenu** + **YACL**: Required for the in-game config screen on Fabric (on NeoForge the config screen is reachable from the mod list)

## Download

Download the latest release from [Modrinth](https://modrinth.com/mod/tradetracker) or [GitHub Releases](https://github.com/DennisTheGamer/TradeTracker/releases).

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download [ModMenu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl)
4. Download TradeTracker (this mod)
5. Place all JAR files in your `mods` folder
6. Launch Minecraft

## Configuration

Open the config screen via ModMenu. Available settings:

- **General** - Enable/disable mod, session persistence, Wandering Trader tracking
- **HUD** - Position (4 corners), opacity (0-100%), scale (50-150%), visibility mode
- **Session** - Session summary display

Keybinds are listed under **Controls > TradeTracker**:
- `T` - Toggle compact HUD mode
- `G` - Open trade history screen
- `J` - Start/pause/resume session

Config file is saved at `config/tradetracker.json`.

## Building from Source

```bash
git clone https://github.com/DennisTheGamer/TradeTracker.git
cd TradeTracker
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

- **Author**: Dennis_thegamer
- **Built with**: Fabric, NeoForge, Fabric API, YACL, ModMenu

## Support

- Report bugs on [GitHub Issues](https://github.com/DennisTheGamer/TradeTracker/issues)
- Visit the [Modrinth page](https://modrinth.com/mod/tradetracker) for more information
