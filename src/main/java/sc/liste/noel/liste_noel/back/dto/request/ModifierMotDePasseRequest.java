package sc.liste.noel.liste_noel.back.dto.request;

public record ModifierMotDePasseRequest(String ancienMotDePasse,
                                        String nouveauMotDePasse,
                                        String confirmationMotDePasse) {
}
