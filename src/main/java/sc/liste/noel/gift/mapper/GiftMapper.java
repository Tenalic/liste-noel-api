package sc.liste.noel.gift.mapper;

import sc.liste.noel.gift.db.entity.GiftEntity;
import sc.liste.noel.gift.dto.GiftDto;

import java.util.ArrayList;
import java.util.List;

public class GiftMapper {


    public static List<GiftDto> entitiesToDtos(List<GiftEntity> giftEntityList) {
        if (giftEntityList == null) {
            return List.of();
        }
        List<GiftDto> giftDtoList = new ArrayList<>();
        for (GiftEntity giftEntity : giftEntityList) {
            GiftDto giftDto = entityToDto(giftEntity);
            giftDtoList.add(giftDto);
        }
        giftDtoList.sort(((o1, o2) -> Math.toIntExact(o1.getGiftId() - o2.getGiftId()))); // sort by ascending id
        return giftDtoList;
    }

    private static GiftDto entityToDto(GiftEntity giftEntity) {
        GiftDto giftDto = new GiftDto();
        giftDto.setDescription(giftEntity.getDescription());
        giftDto.setHolder(giftEntity.getHolder());
        giftDto.setHolderPseudo(giftEntity.getHolderPseudo());
        giftDto.setUrl(giftEntity.getUrl());
        giftDto.setTaken(giftEntity.getTaken());
        giftDto.setTitle(giftEntity.getTitle());
        giftDto.setGiftId(giftEntity.getGiftId());
        giftDto.setPriorityValue(giftEntity.getPriorityValue());
        giftDto.setPriorityLabel(mapPriorityLabel(giftEntity.getPriorityValue()));
        return giftDto;
    }

    public static String mapPriorityLabel(Integer value) {
        switch (value) {
            case 1 :
                return "❤️❤️❤️❤️❤️";
            case 2 :
                return "❤️❤️❤️❤️";
            case 3 :
                return "❤️❤️❤️";
            case 4 :
                return "❤️❤️";
            case 5 :
                return "❤️";
        }
        return "NULL";
    }
}
