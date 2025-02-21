# MCOfflineAuth

![A screenshot.](res/screenshot.png)

---
Basic authentication mod for servers.

**Only [Fabric](https://fabricmc.net/) is supported here!**

### Click here for the [forge version](https://github.com/a455jldvmsrwll1a/MCOfflineAuth4Forge).

## Dependencies

- [Minecraft 1.21.4](https://www.minecraft.net)
- [Fabric 0.16.9](https://fabricmc.net/) or later.
- [Fabric API](https://modrinth.com/mod/fabric-api)
- **[Optional]** [ModMenu](https://modrinth.com/mod/modmenu) 13.0.0 or later.

## Installation

**Just need to download the mod? You can use the precompiled binaries which can be found in the [Releases](https://github.com/a455jldvmsrwll1a/MCOfflineAuth/releases) section.**

Building from source:

1. Clone the repository and enter the project directory.
2. Run `./gradlew build` on Linux/Mac and `.\gradlew.bat build` on Windows.
3. Hopefully it should build just fine.
4. The compiled JAR can be found in `build/libs/MCOfflineAuth-*.jar`, along with the source JAR.


## Usage (Players)

**You can do `/offauth help` for available commands.**

In most cases, it is extremely simple:

1. [Install](#installation) the mod.
2. Join a server (with the  mod installed).
3. Click the prompt when you log-on or run `/offauth bind`
4. Done.

*Got kicked?* The server can be set to reject users without a key already bound. In this case, an admin needs to bind you in advance.

1. Click the **OA** button in the main menu. (Mod screen is also available if you have [ModMenu](https://modrinth.com/mod/modmenu))
2. Click the long button with the key to copy the key.
3. Share this key to an admin of the server.
4. Join the server.

#### Unbinding

To unbind, you can do `/offauth unbind`.

#### Status

To view mod status, run `/offauth info`.

It will display whether it's active or not and how many users are in the database.

To list players, run `/offauth list`. It will display the list of known players.

To check a user's key (or lack thereof), run `/offauth info <user>`. This command can only be run by someone with the `mc-offline-auth` permission or OP.

### Operators

Operators can bind and unbind other players, enable and disable the mod, and change some configuration.
The following commands require the `mc-offline-auth` permission or OP.

To bind another user, run: `/offauth bind <username> <public key>`. Substitute `<username>` with the username and `<public key>` with the key they have provided you.

To unbind another user, run: `/offauth unbind <username>`.

To unbind *all* users, run: `/offauth unbind --`.

## Usage (Server Administrators)

Setup should be as easy as dropping the JAR in the mods folder. :>

If files were modified externally during runtime, you can reload the mod's `server.conf` and `authorised-keys.json` by running `/offauth reload`.

The aformentioned command can only be run by someone with either `binding` or `config` permissions.

## Configuration

The mod creates and uses files in its own `.offline-auth` directory, located within `.minecraft`.

The client will create and read the files `secret-key` and `public-key` to store the keypair.

The server stores known users in `authorised-keys.json`, and its configuration in `server.conf`:

- **Boolean** `enforcing`

  Whether the mod is active. If this is `false`, the server will not attempt to intercept logins and will act like vanilla.

  Can be enabled or disabled  with `/offauth enable` and `/offauth disable`, respectively (requires the `mc-offline-auth` permission or OP).
  
- **Boolean** `allow_unbound_users`

  Decides whether users without a key bound prior to joining will be allowed in. If `false`, the server will kick users not in `authorised-keys.json`. A server admin will have to bind users' keys in advance.

  Users can temporarily be exempt from this when they unbind or using `/offauth grace <user>` (requires the `mc-offline-auth` permission or OP).

- **Unsigned Integer** `unbound_user_grace_period`

  If `allow_unbound_users` is false, how long to let users join after unbinding, expressed in seconds.

- **String** `msg.*`

  User-editable messages.

## Licence

[MIT](LICENSE.txt)
