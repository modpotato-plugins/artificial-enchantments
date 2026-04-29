# Artificial Enchantments

A shared plugin library for Paper 1.21+ servers that lets other plugins create **real, client-visible custom enchantments** — no lore hacks, no packet trickery.

## What It Does

Artificial Enchantments is a **server-side library** that multiple plugins can depend on simultaneously. Instead of every enchantment plugin shipping its own (conflicting) enchantment system, they all hook into this one shared registry.

**For players:** Enchantments show up natively in the client — you see them in item tooltips, anvil interfaces, and the enchantment table. They look and feel like vanilla enchantments because they *are* registered in the native Paper registry.

**For server owners:** Install this once, and any plugin that depends on it Just Works™. No conflicts, no duplicate listeners, no "why does my sword have 3 different lore-based sharpness enchantments" headaches.

## Requirements

- **Paper 1.21+** (Purpur supported; validate Folia behavior with your own plugin stack)
- **Java 21+**

## Installation

1. Download the latest release from the **Files** tab on this page.
2. Drop `artificial-enchantments-x.x.x.jar` into your server's `plugins/` folder.
3. Start (or restart) your server.
4. Install any plugin that depends on Artificial Enchantments.

That's it. There is no configuration file — this is a library, not a plugin with commands or settings.

## Why This Exists

Most custom enchantment plugins before 1.21 used one of two approaches:

- **Lore-based:** Fake enchantments stored in item lore. Looks janky, breaks constantly, incompatible with other plugins.
- **Shaded registries:** Each plugin ships its own copy of an enchantment library. They conflict with each other, duplicate event listeners, and corrupt each other's state.

Artificial Enchantments solves this by sitting **between** the server and enchantment plugins as a single, shared authority. Every plugin that uses it talks to the same registry, the same event dispatch spine, and the same native Paper enchantment system.

## Compatibility

| Platform | Status |
|---|---|
| Paper | ✅ Supported |
| Purpur | ✅ Supported |
| Folia | ⚠️ Declared support; validate on your target stack |
| Spigot | ❌ Not supported (requires Paper 1.21+ API) |

## For Plugin Developers

If you're building a plugin that uses Artificial Enchantments, check out the [GitHub repository](https://github.com/modpotato-plugins/artificial-enchantments) for:

- API documentation
- Gradle/Maven setup instructions
- Code examples
- Javadoc reference

**Do not shade this library into your plugin.** It must remain a separate plugin JAR in the `plugins/` folder. Every dependent plugin references it via `compileOnly`.

## Support

- **Bug reports & feature requests:** [GitHub Issues](https://github.com/modpotato-plugins/artificial-enchantments/issues)
- **Source code:** [GitHub](https://github.com/modpotato-plugins/artificial-enchantments)

## License

MIT License
