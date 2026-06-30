package sc.liste.noel.account.mapper;

import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.dto.AccountInformationDto;

public class AccountInformationsMapper {

    private AccountInformationsMapper() {
    }

    public static final AccountInformationDto entityToDto(AccountEntity accountEntity) {
        if (accountEntity == null)
            return null;
        AccountInformationDto accountInformationDto = new AccountInformationDto();
        accountInformationDto.setEmail(accountEntity.getEmail());
        accountInformationDto.setPseudo(accountEntity.getPseudo());
        accountInformationDto.setLastLoginDate(accountEntity.getLastLoginDate().toString());
        accountInformationDto.setLastPasswordChangeDate(accountEntity.getLastPasswordChangeDate().toString());
        return accountInformationDto;
    }
}
