package sc.liste.noel.liste_noel.back.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static sc.liste.noel.liste_noel.back.mapper.Constantes.API_COMPTE_EMAIL_OBLIGATOIRE_KEY;
import static sc.liste.noel.liste_noel.back.mapper.Constantes.API_COMPTE_PASSWORD_OBLIGATOIRE;

public record ConnexionRequest(
        @NotBlank(message = API_COMPTE_EMAIL_OBLIGATOIRE_KEY)
        @Email
        String email,

        @NotBlank(message = API_COMPTE_PASSWORD_OBLIGATOIRE)
        String password
) {}