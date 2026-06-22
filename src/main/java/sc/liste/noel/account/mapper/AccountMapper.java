package sc.liste.noel.account.mapper;

import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.dto.AccountDto;

public class AccountMapper {

    public static AccountDto entityToDto(AccountEntity accountEntity) {
        if (accountEntity == null) {
            return null;
        }
        AccountDto accountDto = new AccountDto();
        accountDto.setEmail(accountEntity.getEmail());
        accountDto.setPseudo(accountEntity.getPseudo());
        return accountDto;
    }
}
