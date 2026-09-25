# FlamboyantPluginTools (vendored)

Shared helpers (GUI, configurable parameters, utils) used by this plugin.

- Origin: https://github.com/RaphLeFlamboyant/FlamboyantPluginTools
- Commit: `81fafd81de9bf619a58869d6f7af838e97b4da37` (formerly the `submodules/FlamboyantPluginTools` git submodule)

The sources were copied into this repository (instead of a submodule) so they could be
patched for the Minecraft 26.2 / Spigot API `26.2-R0.1-SNAPSHOT` migration. See the git
history of `libs/FlamboyantPluginTools` for the changes made on top of the upstream commit.

They are compiled into the plugin jar via `build-helper-maven-plugin` (see `pom.xml`).
