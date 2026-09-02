import os

def replace_in_file(path, replacements):
    with open(path, 'r') as f:
        content = f.read()
    
    # check if we need to add import shipeditor.utility.text.StringManager;
    if 'StringManager.getString' in str(replacements) and 'import shipeditor.utility.text.StringManager;' not in content:
        # find the last import
        lines = content.split('\n')
        last_import = 0
        for i, line in enumerate(lines):
            if line.startswith('import '):
                last_import = i
        if last_import > 0:
            lines.insert(last_import + 1, 'import shipeditor.utility.text.StringManager;')
            content = '\n'.join(lines)
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(path, 'w') as f:
        f.write(content)

replace_in_file('src/main/java/shipeditor/menubar/FileMenu.java', [
    ('super("File");', 'super(StringManager.getString("MENU_FILE"));'),
    ('"Clear Data & Reinitialize"', 'StringManager.getString("CLEAR_DATA_TITLE")')
])

replace_in_file('src/main/java/shipeditor/menubar/ViewMenu.java', [
    ('super("View");', 'super(StringManager.getString("MENU_VIEW"));'),
    ('"Enable View Rotation"', 'StringManager.getString("ENABLE_VIEW_ROTATION")'),
    ('"Show Background Grid / Image"', 'StringManager.getString("SHOW_BACKGROUND_GRID_IMAGE")'),
    ('"Hide Non-Built-in Weapons"', 'StringManager.getString("HIDE_NON_BUILT_IN_WEAPONS")'),
    ('"Show Cursor Guides"', 'StringManager.getString("SHOW_CURSOR_GUIDES")'),
    ('"Show Sprite Bounds"', 'StringManager.getString("SHOW_SPRITE_BOUNDS")'),
    ('"Show Sprite Center Marker"', 'StringManager.getString("SHOW_SPRITE_CENTER_MARKER")')
])

replace_in_file('src/main/java/shipeditor/menubar/EditMenu.java', [
    ('super("Edit");', 'super(StringManager.getString("MENU_EDIT"));'),
    ('"Enable Selection Holding (Ctrl-Hold)"', 'StringManager.getString("ENABLE_SELECTION_HOLDING")'),
    ('"Enable Cursor Snapping"', 'StringManager.getString("ENABLE_CURSOR_SNAPPING")'),
    ('"Enable Rotation Rounding (15°)"', 'StringManager.getString("ENABLE_ROTATION_ROUNDING")'),
    ('"Select Clicked Point"', 'StringManager.getString("SELECT_CLICKED_POINT")'),
    ('"Select Closest Point"', 'StringManager.getString("SELECT_CLOSEST_POINT")')
])

replace_in_file('src/main/java/shipeditor/menubar/LayerMenu.java', [
    ('super("Layer");', 'super(StringManager.getString("MENU_LAYER"));')
])

replace_in_file('src/main/java/shipeditor/menubar/DataMenu.java', [
    ('super("Data");', 'super(StringManager.getString("MENU_DATA"));'),
    ('"Loading in Progress"', 'StringManager.getString("LOADING_IN_PROGRESS_TITLE")'),
    ('"JSON Corrected"', 'StringManager.getString("JSON_CORRECTED_TITLE")'),
    ('"Error Correcting JSON"', 'StringManager.getString("ERROR_CORRECTING_JSON_TITLE")')
])

replace_in_file('src/main/java/shipeditor/components/settings/PreferencesDialog.java', [
    ('super(owner, "Preferences", true);', 'super(owner, StringManager.getString("PREFERENCES_TITLE"), true);'),
    ('addTab("General"', 'addTab(StringManager.getString("TAB_GENERAL")'),
    ('addTab("Theme"', 'addTab(StringManager.getString("TAB_THEME")'),
    ('addTab("About"', 'addTab(StringManager.getString("TAB_ABOUT")')
])

replace_in_file('src/main/java/shipeditor/components/dialogs/ExportDialog.java', [
    ('super(PrimaryWindow.getInstance(), "Export Manager", true);', 'super(PrimaryWindow.getInstance(), StringManager.getString("EXPORT_MANAGER_TITLE"), true);'),
    ('addTab("Viewport Snapshot"', 'addTab(StringManager.getString("TAB_VIEWPORT_SNAPSHOT")'),
    ('addTab("Sprite Print"', 'addTab(StringManager.getString("TAB_SPRITE_PRINT")'),
    ('addTab("Entity Showcase"', 'addTab(StringManager.getString("TAB_ENTITY_SHOWCASE")'),
    ('setDialogTitle("Export Viewport");', 'setDialogTitle(StringManager.getString("EXPORT_VIEWPORT_TITLE"));'),
    ('setDialogTitle("Save Sprite Image");', 'setDialogTitle(StringManager.getString("SAVE_SPRITE_IMAGE_TITLE"));'),
    ('setDialogTitle("Save Showcase Image");', 'setDialogTitle(StringManager.getString("SAVE_SHOWCASE_IMAGE_TITLE"));'),
    ('"Select Background Color"', 'StringManager.getString("SELECT_BACKGROUND_COLOR_TITLE")'),
    ('JOptionPane.showMessageDialog(this, "Success", "Success"', 'JOptionPane.showMessageDialog(this, "Success", StringManager.getString("SUCCESS_TITLE")'),
    ('JOptionPane.showMessageDialog(this, "Export Failed: " + ex.getMessage(), "Error"', 'JOptionPane.showMessageDialog(this, "Export Failed: " + ex.getMessage(), StringManager.getString("ERROR_TITLE")')
])

replace_in_file('src/main/java/shipeditor/components/instrument/ship/variant/WeaponTreeContextMenuController.java', [
    ('"Toggle autofire"', 'StringManager.getString("TOGGLE_AUTOFIRE")'),
    ('"Mode: Linked"', 'StringManager.getString("MODE_LINKED")'),
    ('"Mode: Alternating"', 'StringManager.getString("MODE_ALTERNATING")')
])

replace_in_file('src/main/java/shipeditor/utility/components/dialog/WeaponGroupTableDialog.java', [
    ('"Weapon"', 'StringManager.getString("COLUMN_WEAPON")')
])
