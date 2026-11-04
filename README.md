# Buff's Potions

NeoForge 26.2 mod focused on making potions practical in normal play.

## Development

Minecraft 26.2 requires Java 25.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew gameTestServer
```

Curios is included in development runs by default. Use
`-PincludeCurios=false` to test the standalone fallback slot.