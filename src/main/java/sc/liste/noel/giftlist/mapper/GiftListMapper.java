package sc.liste.noel.giftlist.mapper;

import sc.liste.noel.gift.mapper.GiftMapper;
import sc.liste.noel.giftlist.db.entity.GiftListEntity;
import sc.liste.noel.giftlist.dto.GiftListDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GiftListMapper {

    public static List<GiftListDto> entitiesToDtosWithoutGifts(List<GiftListEntity> giftListEntityList) {
        if (giftListEntityList == null) {
            return List.of();
        }
        List<GiftListDto> list = new ArrayList<>();
        for (GiftListEntity giftListEntity : giftListEntityList) {
            GiftListDto giftListDto = mapping(giftListEntity);
            list.add(giftListDto);
        }
        return list;
    }


    public static GiftListDto entityToDto(GiftListEntity giftListEntity) {
        if (giftListEntity == null) {
            return null;
        }
        GiftListDto giftListDto = mapping(giftListEntity);
        giftListDto.setGifts(GiftMapper.entitiesToDtos(giftListEntity.getGifts()));
        return giftListDto;
    }

    private static GiftListDto mapping(GiftListEntity giftListEntity) {
        GiftListDto giftListDto = new GiftListDto();
        giftListDto.setName(giftListEntity.getName());
        giftListDto.setOwner(giftListEntity.getOwner());
        giftListDto.setGiftListId(giftListEntity.getGiftListId());
        giftListDto.setPublic(giftListEntity.getIsPublic());
        giftListDto.setGiftCount(Optional.of(giftListEntity).map(GiftListEntity::getGifts).map(List::size).orElse(0));
        return giftListDto;
    }

    public static String buildShareUrl(String baseUrl, Long giftListId) {
        return baseUrl + "/liste/" + giftListId;
    }

}
