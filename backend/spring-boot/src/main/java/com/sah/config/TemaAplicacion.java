package com.sah.config;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Tema visual de la interfaz Vaadin.
 *
 * - Lumo: tema base de Vaadin (componentes).
 * - styles/sah.css: colores del diseño original de SAH (negro y verde),
 *   en src/main/resources/META-INF/resources/styles/sah.css.
 * - Esquema oscuro para toda la aplicación.
 *
 * Solo puede existir una clase AppShellConfigurator en el proyecto.
 */
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles/sah.css")
@ColorScheme(ColorScheme.Value.DARK)
public class TemaAplicacion implements AppShellConfigurator {
}
