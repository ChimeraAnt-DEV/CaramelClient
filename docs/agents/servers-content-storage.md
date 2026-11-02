## Servers / content storage (the "add server always fails" bug)
- **`ContentManager.setStorageDirectories` is what points `ServerManager` at a game-data root.**
  `ContentListActivity.onCreate` now calls its own `updateStorageDirectories()` before `loadContent()`,
  because the screen can be the first in the process (deep link / restore / opened from another app)
  and then nobody had wired `ContentManager`, leaving `ServerManager.minecraftPeDir` null -- every
  add/delete server failed while worlds and packs happened to work. The resolution matches the worlds
  and packs path (`getGameDataDirForType`), so servers cannot drift onto a different root.
- **`QuickLaunchActivity` writes `external_servers.txt` directly** (`ServerManager.writeServerToFile`)
  rather than sending the game a `minecraft://` deep link. When a session was already running the
  URI was forwarded as an extra the game never read, so "Add server" failed with nothing to show.
  Format is the one the game reads: `id:name:ip:port:timestamp`, id one past the current maximum;
  an existing name+ip+port is refused so the row cannot duplicate.
