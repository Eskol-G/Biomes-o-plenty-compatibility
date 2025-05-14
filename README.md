# Biomes 'o plenty Compatibility mod

This mod acts as a compatibility bridge between `Biomes O' Plenty` and creature-spawning mods like `Ice and Fire`. Since many creature mods don't recognize the custom biomes added by Biomes O' Plenty, their mobs won't spawn naturally in those environments.

This mod solves that by mapping each custom biome to a corresponding vanilla biome. That way, any mod that uses vanilla biome logic for spawning will now also work seamlessly with Biomes O' Plenty biomes — no manual config required.

i will document the creation of the file

## 1. check java version (install if not installed)
to check the version
```bash
java -version
```
to install java:
```bash
sudo apt install openjdk-17-jdk
```
## 2. Install MDK
we need the package from the oficial [Forge website]("https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0-mdk.zip")
> INFO
> I use `Minecraft 1.20.1` with Forge version `47.3.0`, for any other version visit the [Forge website]("https://files.minecraftforge.net/net/minecraftforge/forge/")

unzip the content
```bash
unzip forge-1.20.1-47.3.0-mdk.zip
```

and you will get the structure of the mod, the code will be edited in the following directories:
- `/src/main/java/com/eskol/bopCompatibility/modFile.java`: in my case named `BobCompatibility.java`
- `/src/main/java/com/eskol/bopCompatibility/Config.java`: to store config params of the mod
- `/src/main/resources/META-INF/mods.toml`: metadata and dependencies of the mod

## 3. code

all the code is in the project

## 4. building
