import json

with open("src/main/resources/ui_general.json", "r") as f:
    data = json.load(f)

data.update({
"MENU_FILE": "File",
"MENU_VIEW": "View",
"MENU_EDIT": "Edit",
"MENU_LAYER": "Layer",
"MENU_DATA": "Data",
"CLEAR_DATA_TITLE": "Clear Data & Reinitialize",
"LOADING_IN_PROGRESS_TITLE": "Loading in Progress",
"JSON_CORRECTED_TITLE": "JSON Corrected",
"ERROR_CORRECTING_JSON_TITLE": "Error Correcting JSON",
"TAB_GENERAL": "General",
"TAB_THEME": "Theme",
"TAB_ABOUT": "About",
"PREFERENCES_TITLE": "Preferences",
"EXPORT_MANAGER_TITLE": "Export Manager",
"TAB_VIEWPORT_SNAPSHOT": "Viewport Snapshot",
"TAB_SPRITE_PRINT": "Sprite Print",
"TAB_ENTITY_SHOWCASE": "Entity Showcase",
"EXPORT_VIEWPORT_TITLE": "Export Viewport",
"SAVE_SPRITE_IMAGE_TITLE": "Save Sprite Image",
"SAVE_SHOWCASE_IMAGE_TITLE": "Save Showcase Image",
"ERROR_TITLE": "Error",
"SUCCESS_TITLE": "Success",
"SELECT_BACKGROUND_COLOR_TITLE": "Select Background Color",
"TOGGLE_AUTOFIRE": "Toggle autofire",
"MODE_LINKED": "Mode: Linked",
"MODE_ALTERNATING": "Mode: Alternating",
"COLUMN_WEAPON": "Weapon"
})

with open("src/main/resources/ui_general.json", "w") as f:
    json.dump(data, f, indent=4)

with open("src/main/resources/editor_controls.json", "r") as f:
    data2 = json.load(f)

data2.update({
"ENABLE_VIEW_ROTATION": "Enable View Rotation",
"SHOW_BACKGROUND_GRID_IMAGE": "Show Background Grid / Image",
"HIDE_NON_BUILT_IN_WEAPONS": "Hide Non-Built-in Weapons",
"SHOW_CURSOR_GUIDES": "Show Cursor Guides",
"SHOW_SPRITE_BOUNDS": "Show Sprite Bounds",
"SHOW_SPRITE_CENTER_MARKER": "Show Sprite Center Marker",
"ENABLE_SELECTION_HOLDING": "Enable Selection Holding (Ctrl-Hold)",
"ENABLE_CURSOR_SNAPPING": "Enable Cursor Snapping",
"ENABLE_ROTATION_ROUNDING": "Enable Rotation Rounding (15°)",
"SELECT_CLICKED_POINT": "Select Clicked Point",
"SELECT_CLOSEST_POINT": "Select Closest Point"
})

with open("src/main/resources/editor_controls.json", "w") as f:
    json.dump(data2, f, indent=4)
