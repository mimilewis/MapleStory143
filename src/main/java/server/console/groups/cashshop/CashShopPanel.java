package server.console.groups.cashshop;


import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.rootpane.WebFrame;

public class CashShopPanel extends WebDialog {

    private CashShopPanel instance;

    CashShopPanel(WebFrame owner) {
        super(owner, "游戏商城", false);
    }

    public synchronized CashShopPanel getInstance(WebFrame owner) {
        if (instance == null) {
            instance = new CashShopPanel(owner);
        }
        return instance;
    }

    public void init() {

    }
}
