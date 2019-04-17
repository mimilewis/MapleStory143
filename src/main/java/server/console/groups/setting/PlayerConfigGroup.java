package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;

import java.awt.*;

public class PlayerConfigGroup extends AbstractConfigGroup {
    PlayerConfigGroup(ConfigPanel owner) {
        super(owner, "角色相关");
    }

    @Override
    public Component getPreview() {
        TitleWebPanel titleWebPanel1 = new TitleWebPanel("初始属性设置");
        titleWebPanel1.add(new GroupPanel(false,
                new ConfigComponent("新手出生地图", "channel.player.beginnermap"),
                new ConfigComponent("默认可创建角色数量", "channel.player.maxcharacters"),
                new ConfigComponent("能力值上限", "channel.player.maxap"),
                new ConfigComponent("最大HP上限", "channel.player.maxhp"),
                new ConfigComponent("最大MP上限", "channel.player.maxmp"),
                new ConfigComponent("最高等级上限", "channel.player.maxlevel"),
                new ConfigComponent("持有金币上限", "channel.player.maxmeso"),
                new ConfigComponent("每日免费复活次数", "channel.player.resufreecount"),
                new ConfigComponent("付费复活所需金币", "channel.player.resuneedmeso"),
                new ConfigComponent("武器伤害上限(突破之石)", "channel.player.limitbreak")
        ).setMargin(5));
        return titleWebPanel1;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
