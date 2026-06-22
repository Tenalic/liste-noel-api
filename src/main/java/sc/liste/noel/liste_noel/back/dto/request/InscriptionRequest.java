package sc.liste.noel.liste_noel.back.dto.request;

public record InscriptionRequest(String pseudo,
                                 String email,
                                 String password,
                                 String confirmPassword,
                                 Boolean acceptCGU) {
}
