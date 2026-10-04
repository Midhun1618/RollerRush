Optional artwork. Everything here is OPTIONAL: the game generates placeholder art when a file is missing.

Folders: player/ obstacles/ coins/ background/ ui/

Player body parts are looked up as   player/<skinId>_<part>.png
  e.g. player/character_skin_default_head.png, player/shirt_default_torso.png ...
See AssetManager.createPlayerSprites() for the exact file names.
Obstacles:  obstacles/ground.png, obstacles/tall.png, obstacles/moving.png
Coin:       coins/coin.png
Background: background/far.png, background/mid.png, background/ground.png  (must tile horizontally)

NOTE: Android's res/drawable cannot contain sub-folders, so categorised art lives in assets/ instead.
