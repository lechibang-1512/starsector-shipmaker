@SuppressWarnings("module")
module shipeditor {

    // These packages are integral for the editor functionality.
    requires transitive java.desktop;
    requires transitive java.datatransfer;
    requires java.sql;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.jawt;
    requires lwjgl3.awt;
    requires org.joml;
    requires transitive com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.csv;

    // These packages are tightly intertwined with the app and/or are important, but can be removed with some work.
    requires static lombok;
    requires com.formdev.flatlaf;
    requires com.formdev.flatlaf.intellijthemes;
    requires org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.core;
    requires org.apache.commons.collections4;
    requires static com.github.spotbugs.annotations;

    // These packages are mostly cosmetic.
    requires org.kordamp.ikonli.core;
    requires transitive org.kordamp.ikonli.swing;
    requires org.kordamp.ikonli.fluentui;
    requires org.kordamp.ikonli.boxicons;
    requires filters;

    exports shipeditor;
    exports shipeditor.representation;
    exports shipeditor.components;
    exports shipeditor.components.viewer.painters;
    exports shipeditor.components.viewer.entities;
    exports shipeditor.components.viewer.entities.bays;
    exports shipeditor.components.viewer.entities.engine;
    exports shipeditor.components.viewer.entities.weapon;
    exports shipeditor.communication;
    exports shipeditor.communication.events;
    exports shipeditor.communication.events.viewer.layers;
    exports shipeditor.communication.events.viewer.points;
    exports shipeditor.components.viewer.painters.points;
    exports shipeditor.components.viewer.painters.points.weapon;
    exports shipeditor.components.viewer.painters.points.ship;
    exports shipeditor.components.viewer.painters.points.ship.features;

    opens shipeditor.components;
    exports shipeditor.components.viewer.control;
    opens shipeditor.components.viewer.control;
    exports shipeditor.components.viewer.layers;
    opens shipeditor.representation;
    exports shipeditor.components.viewer;
    opens shipeditor.components.viewer;
    exports shipeditor.components.layering;
    opens shipeditor.components.layering;
    exports shipeditor.components.instrument;
    opens shipeditor.components.instrument;
    exports shipeditor.persistence to com.fasterxml.jackson.databind;
    opens shipeditor.persistence to com.fasterxml.jackson.databind;
    exports shipeditor.parsing.deserialize;
    opens shipeditor.parsing.deserialize;
    exports shipeditor.parsing.serialize;
    opens shipeditor.parsing.serialize;
    exports shipeditor.components.datafiles.entities;
    exports shipeditor.utility;
    exports shipeditor.utility.graphics;
    exports shipeditor.representation.weapon;
    opens shipeditor.representation.weapon;
    exports shipeditor.utility.text;
    exports shipeditor.components.viewer.layers.ship;
    exports shipeditor.components.instrument.ship;
    opens shipeditor.components.instrument.ship;
    exports shipeditor.components.instrument.ship.slots;
    opens shipeditor.components.instrument.ship.slots;
    exports shipeditor.components.instrument.ship.skins;
    opens shipeditor.components.instrument.ship.skins;
    exports shipeditor.components.viewer.layers.ship.data;
    exports shipeditor.components.viewer.layers.weapon;
    exports shipeditor.representation.weapon.animation;
    opens shipeditor.representation.weapon.animation;
    exports shipeditor.components.instrument.ship.bays;
    opens shipeditor.components.instrument.ship.bays;
    exports shipeditor.components.instrument.ship.engines;
    opens shipeditor.components.instrument.ship.engines;
    exports shipeditor.components.instrument.ship.shared;
    opens shipeditor.components.instrument.ship.shared;
    exports shipeditor.utility.objects;
    exports shipeditor.utility.overseers;
    exports shipeditor.components.logging;
    opens shipeditor.components.logging;
    exports shipeditor.parsing.serialize.points;
    opens shipeditor.parsing.serialize.points;
    opens shipeditor.components.help;
    exports shipeditor.components.help;
    opens shipeditor.components.help.parts;
    exports shipeditor.components.help.parts;
    exports shipeditor.representation.ship;
    opens shipeditor.representation.ship;
    exports shipeditor.utility.themes;
    exports shipeditor.components.datafiles.entities.transferable;
    exports shipeditor.components.instrument.ship.bounds;
    opens shipeditor.components.instrument.ship.bounds;
    exports shipeditor.utility.components.containers;
    opens shipeditor.utility.components.containers;
    exports shipeditor.components.instrument.ship.hull;
    opens shipeditor.components.instrument.ship.hull;
    exports shipeditor.components.instrument.ship.builtins.hullmods;
    opens shipeditor.components.instrument.ship.builtins.hullmods;
    exports shipeditor.components.instrument.ship.builtins.wings;
    opens shipeditor.components.instrument.ship.builtins.wings;

}