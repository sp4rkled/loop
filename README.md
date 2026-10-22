# Loop (Fabric)

A client-side tick recorder/replayer for Minecraft 26.2. Intended for repeating a simple, manually demonstrated movement/aim/attack pattern (for example, a circular Enderman farm). It does not teleport or use Baritone.

## Commands

1. Stand at the farm position and run `/loopstart`.
2. Perform the desired movement, rotation and attacks.
3. Run `/loopend` to finish recording.
4. Run `/looprun` to replay the captured ticks continuously.
5. Run `/loopover` to stop playback.

The recording is kept in memory only and is cleared when you start a new recording or restart the game.

## Notes / limitations

- Replay copies player movement input and view rotation tick-by-tick. It is a literal playback, not adaptive mob targeting or navigation.
- The loop can drift if collisions, knockback, lag, server tick rate, or mob positions differ from the recording.
- While replaying, Loop takes over movement and attack/use key states. Stop it with `/loopover` before manually playing.
- Use only where automation is allowed by the server/world rules.
- Build with Java 25 and Gradle: `gradle build`. The jar is in `build/libs/`.
