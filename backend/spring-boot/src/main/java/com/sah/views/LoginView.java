package com.sah.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Inicio de sesión con formulario (Spring Security formLogin).
 * Textos en español y mensaje de error cuando las credenciales fallan
 * (Spring Security redirige a /login?error).
 */
@Route("login")
@PageTitle("Iniciar sesión | Security Awareness Hub")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {

        H1 logo = new H1("SAH");
        logo.getStyle()
                .set("font-size", "44px")
                .set("font-weight", "800")
                .set("letter-spacing", "2px")
                .set("color", "var(--sah-verde)")
                .set("margin", "0");

        Paragraph lema = new Paragraph("SECURITY AWARENESS HUB · Capacita · Concientiza · Protege");
        lema.getStyle()
                .set("font-size", "13px")
                .set("letter-spacing", "1px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 0 12px 0")
                .set("text-align", "center");

        loginForm.setI18n(textosEnEspanol());
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background",
                "radial-gradient(circle at top, rgba(67, 229, 138, 0.08), transparent 55%), "
                        + "var(--sah-fondo)");

        add(logo, lema, loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent evento) {
        boolean credencialesIncorrectas = evento.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error");

        loginForm.setError(credencialesIncorrectas);
    }

    private static LoginI18n textosEnEspanol() {

        LoginI18n i18n = new LoginI18n();

        LoginI18n.Form formulario = new LoginI18n.Form();
        formulario.setTitle("Iniciar sesión");
        formulario.setUsername("Correo electrónico");
        formulario.setPassword("Contraseña");
        formulario.setSubmit("Ingresar");
        formulario.setForgotPassword("¿Olvidaste tu contraseña?");
        i18n.setForm(formulario);

        LoginI18n.ErrorMessage error = new LoginI18n.ErrorMessage();
        i18n.setErrorMessage(error);
        error.setTitle("Correo o contraseña incorrectos");
        error.setMessage("Revisa los datos e inténtalo de nuevo. "
                + "Si tu cuenta está inactiva, comunícate con el administrador.");
        error.setUsername("Escribe tu correo electrónico");
        error.setPassword("Escribe tu contraseña");

        return i18n;
    }
}
