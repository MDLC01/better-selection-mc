<img src="src/main/resources/assets/better-selection/icon.png" alt="Better Selection icon" width="128" align="right">

# Better Selection

[![Downloads](https://img.shields.io/modrinth/dt/better-selection)](https://modrinth.com/mod/better-selection)
[![Game versions](https://img.shields.io/modrinth/game-versions/better-selection)](https://modrinth.com/mod/better-selection/versions)
[![GitHub release](https://img.shields.io/github/release/MDLC01/better-selection-mc)](https://github.com/MDLC01/better-selection-mc/releases/latest)
[![License](https://img.shields.io/github/license/MDLC01/better-selection-mc)](UNLICENSE)

This is a Fabric mod for Minecraft that makes text more pleasant to select. It improves the "move by word" feature (<kbd>Ctrl</kbd>+<kbd>←</kbd> and <kbd>Ctrl</kbd>+<kbd>→</kbd>), and enables text selection with the mouse.

## Q&A

### Does it support non-left-to-right scripts?

Yes, it does.

### Does it support macOS?

Yes, macOS should be supported properly:
- <kbd>⌘ Command</kbd>+<kbd>←</kbd> moves the cursor to the beginning of the line,
- <kbd>⌘ Command</kbd>+<kbd>→</kbd> moves the cursor to the end of the line,
- <kbd>⌘ Command</kbd>+<kbd>⌫ Backspace</kbd> deletes everything to the left of the cursor,
- <kbd>⌘ Command</kbd>+<kbd>⌦ Delete</kbd> deletes everything to the right of the cursor,
- Shortcuts that use <kbd>Ctrl</kbd> on Windows use <kbd>⌥ Option</kbd> on macOS.

### Does it fix [MC-121278](https://bugs.mojang.com/browse/MC-121278)?

No, it does not. But I made another mod that does: [Universal Shortcuts](https://modrinth.com/mod/universal-shortcuts).
