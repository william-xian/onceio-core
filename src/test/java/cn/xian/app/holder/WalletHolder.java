package cn.xian.app.holder;

import cn.xian.app.model.entity.Wallet;
import top.onceio.core.aop.annotation.Transactional;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.exception.Failed;
import top.onceio.core.mvc.annocations.AutoApi;

@AutoApi(Wallet.class)
public class WalletHolder extends DaoHolder<Wallet, Wallet.Meta> {

    @Transactional
    public void transfer(Long from, Long to, Integer v) {
        int cnt = updateBy(Wallet.meta().balance.setExp("-" + v).id.eq(from));
        if (cnt != 1) {
            Failed.throwMsg("Wallet Id：%d is not found", from);
        }
        cnt = updateBy(Wallet.meta().balance.setExp("+" + v).id.eq(to));
        if (cnt != 1) {
            Failed.throwMsg("Wallet Id: %d is not found", from);
        }
    }
}
