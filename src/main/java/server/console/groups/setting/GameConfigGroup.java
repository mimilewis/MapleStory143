package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;

import java.awt.*;

public class GameConfigGroup extends AbstractConfigGroup {
    GameConfigGroup(ConfigPanel owner) {
        super(owner, "游戏参数设置");
    }

    @Override
    public Component getPreview() {
        TitleWebPanel titleWebPanel1 = new TitleWebPanel("倍率设置");
        titleWebPanel1.add(new GroupPanel(false,
                new ConfigComponent("经验倍率", "channel.rate.exp"),
                new ConfigComponent("金币爆率", "channel.rate.meso"),
                new ConfigComponent("怪物爆率(道具、装备)", "channel.rate.drop"),
                new ConfigComponent("公共爆率(例如活动道具)", "channel.rate.globaldrop"),
                new ConfigComponent("专业技能经验倍率", "channel.rate.trait")
        ).setMargin(5));

        TitleWebPanel titleWebPanel2 = new TitleWebPanel("其他功能");
        titleWebPanel2.add(new GroupPanel(false,
                new ConfigComponent("最大在线人数", "login.server.userlimit"),
                new ConfigComponent("默认在线人数(虚假人气)", "login.server.defaultuserlimit"),
                new ConfigComponent("排名更新时间(分钟)", "world.refreshrank"),
                new ConfigComponent("人数统计时间(分钟)", "world.refreshonline"),
                new ConfigComponent("雇佣商店持续时间(分钟)"),
                new ConfigComponent("拍卖按钮NPC代码", "channel.server.enternpc_mts"),
                new ConfigComponent("商城按钮NPC代码", "channel.server.enternpc_cashshop")
        ).setMargin(5));
        return new GroupPanel(5, false, titleWebPanel1, titleWebPanel2);
    }
}
