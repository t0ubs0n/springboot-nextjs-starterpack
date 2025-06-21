package com.toubson.modulith.identity.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AVERTISSEMENT : CETTE CLASSE NE DOIT PAS ÊTRE DÉPLOYÉE EN PRODUCTION !
 * Classe utilitaire pour générer des mots de passe encodés.
 * À utiliser uniquement pendant le développement.
 */


public class PasswordGeneratorUtil {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Génère un mot de passe encodé à partir d'un mot de passe en clair.
     *
     * @param rawPassword le mot de passe en clair
     * @return le mot de passe encodé
     */
    public static String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Point d'entrée pour générer des mots de passe depuis la ligne de commande.
     * Usage: java com.toubson.modulith.security.util.PasswordGeneratorUtil motdepasse1 motdepasse2 ...
     */
    public static void main(String[] args) {
        System.out.println("=== GÉNÉRATEUR DE MOTS DE PASSE ENCODÉS ===");

        if (args.length == 0) {
            // Si aucun argument n'est fourni, générer quelques exemples
            String[] defaultPasswords = {"admin123", "user123", "test123"};
            for (String password : defaultPasswords) {
                System.out.println(password + " -> " + encodePassword(password));
            }
        } else {
            // Sinon, encoder tous les mots de passe fournis en arguments
            for (String password : args) {
                System.out.println(password + " -> " + encodePassword(password));
            }
        }

        System.out.println("==========================================");
    }
}