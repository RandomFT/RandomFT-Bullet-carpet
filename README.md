#### This is a migration into an official fork for my [old Bullet Carpet repo](https://github.com/Dioswilson/Bullet-carpet_old)

# Carpet Mod
Yes.

## Getting Started
### Setting up your sources
- Clone this repository.
- Run `gradlew setupCarpetmod` in the root project directory.

### Using an IDE
- To use Eclipse, run `gradlew eclipse`, then import the project in Eclipse.
- To use Intellij, run `gradlew idea`, then import the project in Intellij.

## Using the build system
Edit the files in the `src` folder, like you would for a normal project. The only special things you have to do are as follows:
### To generate patch files so they show up in version control
Use `gradlew genPatches`
### To apply patches after pulling
Use `gradlew setupCarpetmod`. It WILL overwrite your local changes to src, so be careful.
### To create a release / patch files
In case you made changes to the local copy of the code in `src`, run `genPatches` to update the project according to your src.
Use `gradlew createRelease`. The release will be a ZIP file containing all modified classes, obfuscated, in the `build/distributions` folder.
### To run the server locally (Windows)
Use `mktest.cmd` to run the modified server with generated patches as a localhost server. It requires `gradlew createRelease` to finish successfully as well as using default paths for your minecraft installation folder.

In case you use different paths, you might need to modify the build script.
This will leave a ready server jar file in your saves folder.

It requires to have 7za installed in your paths

### Vales changes:
Vales is an external user who I asked permission to use his code, here are things he did:
You need his carpet client for village marker to work
```/carpet doorSearchOptimization
/carpet doorCheckOptimization
/carpet ironFarmAABBOptimization
/carpet ironGolemsSwim 
/carpet villagerQueue
/carpet villagerQueueLength 
/carpet villagerTickingRate  
/carpet villagersRandomStart
/carpet doorDeregistrationTime 
/log villagerPos
/log villages <dynamic|overworld|nether|end>
/log golems 
/log counter all 
/log portalCaching uncaching 
/log pathfinding
fixed NullPointerException with /log
readded time and total to /counter
fixed /log items not tracking items that change dimensions
Added /ticktimes <start|stop> [filename]
```
### Bullet changes:
Here I will list what we did:

- Changed some minecraft internal behaviour on player profile, I yeeted the toLowerCase method on it and changed a map into a tree map
- Added "bullet" as a carpet command category
- Added `modern` redstoneDustBehaviour
- Fixed `/rng randomtickedChunksCount`
- I also added the following settings:
```
/carpet updateTabEveryGametick
/carpet alwaysSetPlayerIntoSurvival
/carpet commandEnderchest
/carpet commandInventory(coded by slowik)
/carpet scoreboardStats
/carpet blockStateSyncing
/carpet extremeBehaviours
/carpet prometheusExtension
```

### RandomFT changes:

Instaminable Endstone
Allows breaking Endstone instantly.

/carpet instaminableEndstone true


Anvil Turns Cobblestone to Sand
Dropping an anvil on cobblestone turns it into sand.

/carpet anvilTurnsCobbleToSand true


Renewable Mansion Mobs
Woodland mansion mobs respawn (only in newly generated mansions).

/carpet mansionMobsRespawn true


Blaze Meal
Nether wart grows faster when fed with blaze powder.

/carpet blazeMeal true


Silk Touch End Portal Frame (Experimental)
End Portal Frames can drop as items when mined with Silk Touch.

/carpet silkTouchEndPortalFrame true


Anvil Colored Names
You can rename items using &x color codes in an anvil.

/carpet anvilColoredNames true

Color Codes for Anvil Colored Names
Code	Color
&0	Black
&1	Dark Blue
&2	Dark Green
&3	Dark Aqua
&4	Dark Red
&5	Dark Purple
&6	Gold
&7	Gray
&8	Dark Gray
&9	Blue
&a	Green
&b	Aqua
&c	Red
&d	Light Purple
&e	Yellow
&f	White

### Prometheus addition:
I added prometheus integration(https://github.com/prometheus/client_java) into this for monitoring`/prometheus` to configure port.(Pasted license since i had to copy the code)

