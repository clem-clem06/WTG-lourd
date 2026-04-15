package org.example.wtg.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// @Controller (sans "Rest") = renvoie des pages HTML et non du JSON
@Controller
public class PageController {

    // ── Page de connexion ────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(
            // Ces paramètres viennent de l'URL : /login?erreur=true  ou  /login?deconnexion=true
            @RequestParam(required = false) String erreur,
            @RequestParam(required = false) String deconnexion,
            Model model  // Model = sac dans lequel on met des données pour le HTML
    ) {
        if (erreur != null) {
            model.addAttribute("erreurMessage", "Email ou mot de passe incorrect.");
        }
        if (deconnexion != null) {
            model.addAttribute("successMessage", "Vous avez été déconnecté.");
        }
        return "login"; // → cherche src/main/resources/templates/login.html
    }

    // ── Dashboard après connexion ────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // Authentication = objet Spring Security avec les infos de l'utilisateur connecté
        model.addAttribute("email", authentication.getName());

        // Vérifie le rôle pour personnaliser l'affichage
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("isAdmin", isAdmin);

        return "dashboard"; // → cherche src/main/resources/templates/dashboard.html
    }
}
