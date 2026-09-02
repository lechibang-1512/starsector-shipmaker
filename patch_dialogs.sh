#!/bin/bash
# Adding methods to DialogUtilities
sed -i -e '/public static void showAdjustPointDialog/i \
    public static String resolvePackageName(java.nio.file.Path packagePath) {\
        if (packagePath == null) return "Unknown";\
        if (shipeditor.components.settings.SettingsManager.isCoreFolder(packagePath)) {\
            return "Starsector Core";\
        }\
        java.nio.file.Path fileName = packagePath.getFileName();\
        return fileName != null ? fileName.toString() : packagePath.toString();\
    }\
\
    public static String[] buildSortedPackageList(java.util.Collection<String> packages, int totalCount, String itemType, java.nio.file.Path shipPackage) {\
        java.util.Set<String> uniquePackages = new java.util.HashSet<>(packages);\
        java.util.List<String> sortedPackages = new java.util.ArrayList<>(uniquePackages);\
        sortedPackages.remove("Starsector Core");\
        java.util.Collections.sort(sortedPackages, String.CASE_INSENSITIVE_ORDER);\
\
        java.util.List<String> list = new java.util.ArrayList<>();\
        list.add("All Indexed Mods (" + totalCount + " " + itemType + ")");\
        list.add("Starsector Core");\
        if (shipPackage != null) {\
            String shipModName = resolvePackageName(shipPackage);\
            if (!"Starsector Core".equals(shipModName) && !list.contains(shipModName)) {\
                list.add("Ship'\''s Mod (" + shipModName + ")");\
            }\
        }\
        list.addAll(sortedPackages);\
\
        return list.toArray(new String[0]);\
    }\
' src/main/java/shipeditor/utility/components/dialog/DialogUtilities.java
