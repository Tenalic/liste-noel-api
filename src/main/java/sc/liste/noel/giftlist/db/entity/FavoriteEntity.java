package sc.liste.noel.giftlist.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "favoris")
public class FavoriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_favoris")
    private Long favoriteId;

    @Column(name = "id_liste")
    private Long giftListId;

    @Column(name = "email")
    private String email;

    public FavoriteEntity() {
    }

    public Long getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(Long favoriteId) {
        this.favoriteId = favoriteId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getGiftListId() {
        return giftListId;
    }

    public void setGiftListId(Long giftListId) {
        this.giftListId = giftListId;
    }
}
